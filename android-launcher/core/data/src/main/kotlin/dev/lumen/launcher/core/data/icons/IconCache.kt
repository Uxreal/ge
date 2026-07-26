package dev.lumen.launcher.core.data.icons

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Looper
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lumen.launcher.core.data.apps.LauncherProfiles
import dev.lumen.launcher.core.data.model.AppKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The §5 icon pipeline: two-tier cache, all decoding off the main thread, **unmasked** bitmaps.
 *
 * The shape mask is applied at draw time in `:core:design` (§3), so a theme or shape change costs
 * nothing here — the cache never needs invalidating for it. Memory tier is an [LruCache] sized from
 * [ActivityManager.getMemoryClass]; disk tier is PNGs keyed by package + last-update time + size, so
 * an app update naturally misses and re-renders.
 *
 * §5's hard requirement — zero icon decodes on the main thread — is asserted in debuggable builds:
 * any decode reaching the main looper throws immediately instead of shipping a jank.
 */
@Singleton
class IconCache @Inject constructor(
    @ApplicationContext private val context: Context,
    scope: CoroutineScope,
) {
    private val launcherApps: LauncherApps? =
        runCatching { context.getSystemService(LauncherApps::class.java) }.getOrNull()

    private val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val io: CoroutineDispatcher = Dispatchers.IO

    private val memory: LruCache<String, Bitmap> = run {
        val memoryClassMb = runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
        }.getOrDefault(64)
        object : LruCache<String, Bitmap>(memoryClassMb * 1024 * 1024 / 8) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }
    }

    private val diskDir: File = File(context.filesDir, "icon-cache")
    private val diskLock = Mutex()

    /** Bumps when the whole cache is dropped (locale change, manual clear); UI re-requests. */
    private val _generation = MutableStateFlow(0)
    val generation: StateFlow<Int> = _generation.asStateFlow()

    /** Per-package update times so the disk key changes when an app updates. */
    @Volatile
    private var updateTimes: Map<String, Long> = emptyMap()

    init {
        scope.launch(io) { runCatching { refreshUpdateTimes() } }
    }

    /** Non-blocking probe, safe during composition. Memory tier only. */
    fun cached(key: AppKey, sizePx: Int): Bitmap? = memory.get(cacheKey(key, sizePx))

    /** Loads (memory → disk → render), always off the main thread. */
    suspend fun icon(key: AppKey, sizePx: Int): Bitmap? {
        memory.get(cacheKey(key, sizePx))?.let { return it }
        return withContext(io) {
            assertNotMainThread()
            val cacheKey = cacheKey(key, sizePx)
            val fromDisk = diskLock.withLock { readDisk(cacheKey) }
            val bitmap = fromDisk ?: renderIcon(key, sizePx)?.also { rendered ->
                diskLock.withLock { writeDisk(cacheKey, rendered) }
            }
            bitmap?.also { memory.put(cacheKey, it) }
        }
    }

    fun invalidate(key: AppKey? = null) {
        if (key == null) {
            memory.evictAll()
            _generation.value += 1
        } else {
            // Sizes vary; evict every entry for the app.
            memory.snapshot().keys.filter { it.startsWith("${key.flat}|") }.forEach { memory.remove(it) }
        }
    }

    fun onPackageChanged(packageName: String) {
        refreshUpdateTimes()
        memory.snapshot().keys.filter { it.startsWith(packageName) }.forEach { memory.remove(it) }
        _generation.value += 1
    }

    // ------------------------------------------------------------------ internals

    private fun cacheKey(key: AppKey, sizePx: Int): String =
        "${key.flat}|${updateTimes[key.packageName] ?: 0L}|$sizePx"

    private fun assertNotMainThread() {
        if (debuggable) {
            check(Looper.myLooper() != Looper.getMainLooper()) {
                "Icon decode on the main thread — §5 forbids this"
            }
        }
    }

    private fun refreshUpdateTimes() {
        updateTimes = runCatching {
            context.packageManager.getInstalledPackages(0).associate { it.packageName to it.lastUpdateTime }
        }.getOrDefault(updateTimes)
    }

    private fun renderIcon(key: AppKey, sizePx: Int): Bitmap? = runCatching {
        val apps = launcherApps ?: return null
        val user = LauncherProfiles.handleFor(context, key.profile)
            ?: android.os.Process.myUserHandle()
        val activity = apps.getActivityList(key.packageName, user)
            .firstOrNull { it.componentName.className == key.activityName }
            ?: apps.getActivityList(key.packageName, user).firstOrNull()
            ?: return null
        val drawable = activity.getIcon(context.resources.displayMetrics.densityDpi) ?: return null
        drawable.toBitmap(sizePx)
    }.getOrNull()

    private fun Drawable.toBitmap(sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bitmap
    }

    private fun readDisk(cacheKey: String): Bitmap? = runCatching {
        val file = File(diskDir, cacheKey.toFileName())
        if (!file.exists()) return null
        BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    private fun writeDisk(cacheKey: String, bitmap: Bitmap) {
        runCatching {
            diskDir.mkdirs()
            File(diskDir, cacheKey.toFileName()).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    private fun String.toFileName(): String = "${hashCode().toUInt()}.png"
}
