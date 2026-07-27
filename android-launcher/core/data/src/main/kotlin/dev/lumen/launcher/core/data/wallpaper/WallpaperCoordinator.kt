package dev.lumen.launcher.core.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The launcher's two wallpaper duties: §5's parallax via [setOffsets], and §3's contrast floor via
 * [luminanceAt] — a coarse luminance map sampled from the wallpaper so each label region can compute
 * exactly the scrim it needs.
 *
 * Reading the wallpaper is best-effort: recent Android throws `SecurityException` for apps that are
 * not the default home. When the read fails, [luminanceAt] returns null and labels fall back to a
 * conservative fixed scrim rather than assuming a dark wallpaper.
 */
@Singleton
class WallpaperCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
) {
    private val manager: WallpaperManager? =
        runCatching { WallpaperManager.getInstance(context) }.getOrNull()

    /** GRID×GRID mean-luminance map of the wallpaper, or null when it cannot be read. */
    private val _luminance = MutableStateFlow<FloatArray?>(null)
    val luminance: StateFlow<FloatArray?> = _luminance.asStateFlow()

    private val _colors = MutableStateFlow<WallpaperColorsSnapshot?>(null)

    /** The wallpaper's own colours, kept live across wallpaper changes. Raw ints: `:core:data`
     *  does not depend on `:core:design`, so the design layer maps these into its palette. */
    val colors: StateFlow<WallpaperColorsSnapshot?> = _colors.asStateFlow()

    init {
        readColors()
        runCatching {
            manager?.addOnColorsChangedListener(
                { wallpaperColors, which ->
                    if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                        _colors.value = wallpaperColors?.toSnapshot()
                        refresh()
                    }
                },
                android.os.Handler(android.os.Looper.getMainLooper()),
            )
        }
    }

    private fun readColors() {
        _colors.value = runCatching {
            manager?.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.toSnapshot()
        }.getOrNull()
    }

    private fun android.app.WallpaperColors.toSnapshot() = WallpaperColorsSnapshot(
        primary = primaryColor.toArgb(),
        secondary = secondaryColor?.toArgb() ?: primaryColor.toArgb(),
        tertiary = tertiaryColor?.toArgb() ?: primaryColor.toArgb(),
        // HINT_SUPPORTS_DARK_TEXT set means the wallpaper is light enough for dark-on-light.
        supportsDarkText = if (android.os.Build.VERSION.SDK_INT >= 31) {
            colorHints and android.app.WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        } else {
            false
        },
    )

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            _luminance.value = runCatching { sample() }.getOrNull()
        }
    }

    /**
     * Mean relative luminance of the wallpaper around fractional position ([fx], [fy]) of the
     * screen, or null when the wallpaper is unreadable.
     */
    fun luminanceAt(fx: Float, fy: Float): Float? {
        val map = _luminance.value ?: return null
        val x = (fx.coerceIn(0f, 0.999f) * GRID).toInt()
        val y = (fy.coerceIn(0f, 0.999f) * GRID).toInt()
        return map[y * GRID + x]
    }

    /** §5: `setWallpaperOffsets` for page parallax. [xFraction] is 0..1 across the page range. */
    fun setOffsets(windowToken: IBinder?, xFraction: Float) {
        val token = windowToken ?: return
        runCatching { manager?.setWallpaperOffsets(token, xFraction.coerceIn(0f, 1f), 0.5f) }
    }

    fun setOffsetSteps(windowToken: IBinder?, pages: Int) {
        val token = windowToken ?: return
        val step = if (pages > 1) 1f / (pages - 1) else 0f
        runCatching { manager?.setWallpaperOffsetSteps(step, 0f) }
        // Some OEMs only apply steps after an offsets call on the same token.
        runCatching { manager?.setWallpaperOffsets(token, 0f, 0.5f) }
    }

    private fun sample(): FloatArray? {
        val drawable = manager?.let {
            runCatching { it.fastDrawable ?: it.drawable }.getOrNull()
        } ?: return null
        val bitmap = Bitmap.createBitmap(GRID * 4, GRID * 4, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(canvas)

        val map = FloatArray(GRID * GRID)
        val cellPx = 4
        for (gy in 0 until GRID) {
            for (gx in 0 until GRID) {
                var sum = 0f
                for (py in 0 until cellPx) {
                    for (px in 0 until cellPx) {
                        val color = bitmap.getPixel(gx * cellPx + px, gy * cellPx + py)
                        sum += luminanceOf(color)
                    }
                }
                map[gy * GRID + gx] = sum / (cellPx * cellPx)
            }
        }
        bitmap.recycle()
        return map
    }

    private fun luminanceOf(color: Int): Float {
        fun channel(c: Int): Float {
            val v = c / 255f
            return if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055) / 1.055), 2.4).toFloat()
        }
        return 0.2126f * channel(Color.red(color)) +
            0.7152f * channel(Color.green(color)) +
            0.0722f * channel(Color.blue(color))
    }

    private companion object {
        const val GRID = 8
    }
}

/** `WallpaperManager.getWallpaperColors` distilled to plain ints for the module boundary. */
data class WallpaperColorsSnapshot(
    val primary: Int,
    val secondary: Int,
    val tertiary: Int,
    val supportsDarkText: Boolean,
)
