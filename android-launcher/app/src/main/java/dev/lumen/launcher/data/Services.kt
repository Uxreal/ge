package dev.lumen.launcher.data

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import dev.lumen.launcher.data.apps.DefaultAppRepository
import dev.lumen.launcher.data.apps.DefaultUsageTracker
import dev.lumen.launcher.data.icons.DefaultIconLoader
import dev.lumen.launcher.data.notifications.DefaultBadgeRepository
import dev.lumen.launcher.data.model.AppInfo
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.model.DeepShortcut
import dev.lumen.launcher.data.model.WidgetProviderInfo
import dev.lumen.launcher.data.model.WorkspaceState
import dev.lumen.launcher.data.prefs.DefaultSettingsStore
import dev.lumen.launcher.data.prefs.LauncherSettings
import dev.lumen.launcher.data.shortcuts.DefaultShortcutRepository
import dev.lumen.launcher.data.wallpaper.DefaultWallpaperController
import dev.lumen.launcher.data.widgets.DefaultWidgetController
import dev.lumen.launcher.data.workspace.DefaultWorkspaceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Persisted, observable user settings. Writes are debounced and coalesced by the implementation. */
interface SettingsStore {
    val settings: StateFlow<LauncherSettings>
    fun update(transform: (LauncherSettings) -> LauncherSettings)
    suspend fun replace(value: LauncherSettings)
    suspend fun reset()
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Result<Unit>
}

/** The installed-app universe, kept live via `LauncherApps` callbacks. */
interface AppRepository {
    val apps: StateFlow<List<AppInfo>>
    val loading: StateFlow<Boolean>

    /** Apps that appeared after the first load — used for auto-placement on the workspace. */
    val installed: SharedFlow<AppKey>

    /** Apps that disappeared — the workspace prunes their icons. */
    val uninstalled: SharedFlow<AppKey>

    fun app(key: AppKey): AppInfo?
    fun refresh()
    fun launch(key: AppKey, sourceBounds: Rect? = null): Boolean
    fun openAppInfo(key: AppKey, sourceBounds: Rect? = null)
    fun requestUninstall(key: AppKey)
    fun canUninstall(key: AppKey): Boolean
    fun hasWorkProfile(): Boolean
}

data class IconPackInfo(val packageName: String, val label: String)

/**
 * Shapes, masks, tints and caches every icon the UI draws. [generation] increments whenever the
 * rendered result changes (shape, icon pack, themed-icon toggle …) so composables can re-request.
 */
interface IconLoader {
    val generation: StateFlow<Int>

    /** Non-blocking cache probe — safe to call during composition. */
    fun cached(key: AppKey, sizePx: Int): ImageBitmap?
    suspend fun load(key: AppKey, sizePx: Int): ImageBitmap?
    suspend fun loadShortcut(shortcut: DeepShortcut, sizePx: Int): ImageBitmap?

    /** ARGB colour sampled from the icon, for folder tints and colour sorting. */
    fun dominantColor(key: AppKey): Int
    fun invalidate(key: AppKey? = null)
    suspend fun availableIconPacks(): List<IconPackInfo>
}

/** Persisted home-screen layout. All mutations go through [WorkspaceOpsHost.mutate]. */
interface WorkspaceStore {
    val state: StateFlow<WorkspaceState>
    val ready: StateFlow<Boolean>
    fun mutate(transform: (WorkspaceState) -> WorkspaceState)
    suspend fun ensureInitialized(apps: List<AppInfo>, columns: Int, rows: Int, dockColumns: Int)
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Result<Unit>
    suspend fun reset()
}

/** Marker for documentation purposes — see [WorkspaceStore.mutate]. */
private interface WorkspaceOpsHost

/** Notification counts per package, fed by the optional notification listener service. */
interface BadgeRepository {
    /** package name -> notification count. */
    val badges: StateFlow<Map<String, Int>>
    val listenerConnected: StateFlow<Boolean>
    fun isPermissionGranted(): Boolean
    fun requestPermission(context: Context)
}

interface ShortcutRepository {
    fun shortcuts(key: AppKey): List<DeepShortcut>
    fun launch(shortcut: DeepShortcut, sourceBounds: Rect? = null): Boolean
    fun supported(): Boolean
}

interface WidgetController {
    val providers: StateFlow<List<WidgetProviderInfo>>
    fun startListening()
    fun stopListening()
    fun refreshProviders()
    fun allocateId(): Int
    fun deleteWidget(appWidgetId: Int)

    /** Binds without user interaction when possible; returns false if [requestBind] is needed. */
    fun bind(appWidgetId: Int, providerFlat: String, profile: Long): Boolean
    fun requestBind(activity: Activity, appWidgetId: Int, providerFlat: String, profile: Long)
    fun needsConfigure(providerFlat: String): Boolean
    fun startConfigure(activity: Activity, appWidgetId: Int)
    fun createHostView(context: Context, appWidgetId: Int, providerFlat: String): View?
    fun updateHostViewSize(view: View, widthDp: Int, heightDp: Int)

    /** Preferred span in cells for a provider, given the current cell size. */
    fun defaultSpan(providerFlat: String, cellWidthPx: Int, cellHeightPx: Int): Pair<Int, Int>
    fun previewFor(providerFlat: String, sizePx: Int): ImageBitmap?

    /** Forwarded by the host Activity for bind/configure flows; true when consumed. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?): Boolean
}

/** Launch counts and recency, powering usage-sorted drawers and predictions. */
interface UsageTracker {
    val launchCounts: StateFlow<Map<String, Int>>
    val lastLaunched: StateFlow<Map<String, Long>>
    fun record(key: AppKey)
    fun predictions(limit: Int): List<AppKey>
    fun clear()
}

interface WallpaperController {
    /** Null when the platform denies wallpaper reads; the UI then falls back to a bundled source. */
    val snapshot: StateFlow<ImageBitmap?>
    val canReadWallpaper: StateFlow<Boolean>
    fun refresh()
    fun openWallpaperPicker(context: Context)
}

/**
 * Manual dependency container. A launcher is process-critical and restarts constantly (rotation,
 * theme change, memory pressure) so everything hangs off one long-lived object owned by the
 * Application, with no annotation processors in the build.
 */
class LauncherServices(context: Context) {
    val appContext: Context = context.applicationContext
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    val settings: SettingsStore = DefaultSettingsStore(appContext, scope)
    val apps: AppRepository = DefaultAppRepository(appContext, scope)
    val icons: IconLoader = DefaultIconLoader(appContext, scope, settings, apps)
    val workspace: WorkspaceStore = DefaultWorkspaceStore(appContext, scope)
    val badges: BadgeRepository = DefaultBadgeRepository(appContext, scope)
    val shortcuts: ShortcutRepository = DefaultShortcutRepository(appContext)
    val widgets: WidgetController = DefaultWidgetController(appContext, scope)
    val usage: UsageTracker = DefaultUsageTracker(appContext, scope)
    val wallpaper: WallpaperController = DefaultWallpaperController(appContext, scope)
}

val LocalServices = staticCompositionLocalOf<LauncherServices> {
    error("LauncherServices not provided")
}

val LocalLauncherActivity = staticCompositionLocalOf<Activity?> { null }
