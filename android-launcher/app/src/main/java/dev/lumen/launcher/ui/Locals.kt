package dev.lumen.launcher.ui

import android.app.Activity
import android.graphics.Rect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import dev.lumen.launcher.data.LauncherServices
import dev.lumen.launcher.data.model.AppItem
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.model.DeepShortcut
import dev.lumen.launcher.data.model.ItemContainer
import dev.lumen.launcher.data.model.WorkspaceState
import dev.lumen.launcher.data.prefs.AppOverride
import dev.lumen.launcher.data.prefs.GestureTrigger
import dev.lumen.launcher.data.prefs.LauncherAction
import dev.lumen.launcher.data.prefs.LauncherSettings
import dev.lumen.launcher.data.workspace.WorkspaceOps
import dev.lumen.launcher.system.SystemActions
import dev.lumen.launcher.ui.glass.BackdropState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Full-screen surfaces that sit above the workspace. Only one is ever visible. */
sealed interface LauncherOverlay {
    data object None : LauncherOverlay
    data object Drawer : LauncherOverlay
    data object Settings : LauncherOverlay
    data object WidgetPicker : LauncherOverlay
    data class Folder(val folderId: String, val origin: Rect? = null) : LauncherOverlay
    data class ItemMenu(val itemId: String, val anchor: Rect) : LauncherOverlay
}

/**
 * The launcher's single action surface. Home, drawer, folders, gestures and settings all route
 * through this object, which keeps navigation state in one place and keeps the composables free of
 * business logic.
 */
@Stable
class LauncherController(
    val services: LauncherServices,
    val scope: CoroutineScope,
    private val activityProvider: () -> Activity?,
) {
    var overlay by mutableStateOf<LauncherOverlay>(LauncherOverlay.None)
        private set

    var editMode by mutableStateOf(false)
        private set

    var currentPage by mutableIntStateOf(0)

    /** True while an item is being dragged; the glass keeps refracting for the duration. */
    var dragging by mutableStateOf(false)

    val settings: LauncherSettings get() = services.settings.settings.value

    val activity: Activity? get() = activityProvider()

    // ------------------------------------------------------------------ navigation

    fun show(target: LauncherOverlay) {
        overlay = target
    }

    fun closeOverlay() {
        overlay = LauncherOverlay.None
    }

    fun openDrawer() {
        overlay = LauncherOverlay.Drawer
    }

    fun openSettings() {
        overlay = LauncherOverlay.Settings
    }

    fun openWidgetPicker() {
        overlay = LauncherOverlay.WidgetPicker
    }

    fun openFolder(folderId: String, origin: Rect? = null) {
        overlay = LauncherOverlay.Folder(folderId, origin)
    }

    fun openItemMenu(itemId: String, anchor: Rect) {
        overlay = LauncherOverlay.ItemMenu(itemId, anchor)
    }

    fun setEditMode(enabled: Boolean) {
        editMode = enabled && !settings.home.lockLayout
        if (editMode) overlay = LauncherOverlay.None
    }

    fun toggleEditMode() = setEditMode(!editMode)

    /** Back handling: returns true when the launcher consumed the gesture. */
    fun onBack(): Boolean = when {
        overlay != LauncherOverlay.None -> {
            closeOverlay()
            true
        }
        editMode -> {
            setEditMode(false)
            true
        }
        currentPage != 0 -> {
            currentPage = 0
            true
        }
        else -> false
    }

    // ------------------------------------------------------------------ launching

    fun launch(key: AppKey, bounds: Rect? = null) {
        if (services.apps.launch(key, bounds)) {
            services.usage.record(key)
            if (settings.drawer.closeOnLaunch && overlay == LauncherOverlay.Drawer) closeOverlay()
            if (settings.folders.closeOnLaunch && overlay is LauncherOverlay.Folder) closeOverlay()
        }
    }

    fun launchShortcut(shortcut: DeepShortcut, bounds: Rect? = null) {
        services.shortcuts.launch(shortcut, bounds)
        closeOverlay()
    }

    fun openAppInfo(key: AppKey, bounds: Rect? = null) {
        services.apps.openAppInfo(key, bounds)
        closeOverlay()
    }

    fun uninstall(key: AppKey) {
        services.apps.requestUninstall(key)
        closeOverlay()
    }

    // ------------------------------------------------------------------ settings & layout

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        services.settings.update(transform)
    }

    fun mutateWorkspace(transform: (WorkspaceState) -> WorkspaceState) {
        services.workspace.mutate(transform)
    }

    fun labelFor(key: AppKey): String {
        val fallback = services.apps.app(key)?.label ?: key.packageName
        return settings.apps.labelFor(key.flat, fallback)
    }

    fun rename(key: AppKey, label: String?) = updateSettings { current ->
        val existing = current.apps.overrides[key.flat] ?: AppOverride()
        val updated = existing.copy(label = label?.takeIf { it.isNotBlank() })
        current.copy(
            apps = current.apps.copy(
                overrides = current.apps.overrides + (key.flat to updated),
            ),
        )
    }

    fun setHidden(key: AppKey, hidden: Boolean) = updateSettings { current ->
        val hiddenSet = if (hidden) current.apps.hidden + key.flat else current.apps.hidden - key.flat
        current.copy(apps = current.apps.copy(hidden = hiddenSet))
    }

    fun setCustomIcon(key: AppKey, reference: String?) = updateSettings { current ->
        val existing = current.apps.overrides[key.flat] ?: AppOverride()
        current.copy(
            apps = current.apps.copy(
                overrides = current.apps.overrides + (key.flat to existing.copy(icon = reference)),
            ),
        )
    }

    /** Removes an item from the workspace (does not uninstall). */
    fun removeItem(itemId: String) {
        mutateWorkspace { state -> WorkspaceOps.dissolveThinFolders(WorkspaceOps.remove(state, itemId)) }
        closeOverlay()
    }

    fun addAppToWorkspace(key: AppKey, container: ItemContainer, cell: dev.lumen.launcher.data.model.Cell) {
        mutateWorkspace { state ->
            WorkspaceOps.add(
                state,
                container,
                AppItem(id = WorkspaceOps.newId("app"), cell = cell, key = key),
            )
        }
    }

    // ------------------------------------------------------------------ gestures

    fun perform(action: LauncherAction, trigger: GestureTrigger? = null) {
        val context = activity ?: services.appContext
        when (action) {
            LauncherAction.NONE -> Unit
            LauncherAction.OPEN_DRAWER -> openDrawer()
            LauncherAction.CLOSE_DRAWER -> closeOverlay()
            LauncherAction.OPEN_SEARCH -> {
                openDrawer()
                searchRequested = true
            }
            LauncherAction.OPEN_SETTINGS -> openSettings()
            LauncherAction.EXPAND_NOTIFICATIONS -> SystemActions.expandNotifications(context)
            LauncherAction.EXPAND_QUICK_SETTINGS -> SystemActions.expandQuickSettings(context)
            LauncherAction.LOCK_SCREEN -> SystemActions.lockScreen(context)
            LauncherAction.TOGGLE_EDIT_MODE -> toggleEditMode()
            LauncherAction.OPEN_WIDGET_PICKER -> openWidgetPicker()
            LauncherAction.OPEN_ASSISTANT -> SystemActions.openAssistant(context)
            LauncherAction.OPEN_VOICE_SEARCH -> SystemActions.openVoiceSearch(context)
            LauncherAction.SHOW_HIDDEN_APPS -> {
                openDrawer()
                showHiddenRequested = true
            }
            LauncherAction.GO_TO_FIRST_PAGE -> currentPage = 0
            LauncherAction.OPEN_APP -> trigger
                ?.let { settings.gestures.appTargets[it] }
                ?.let { AppKey.parse(it) }
                ?.let { launch(it) }
            LauncherAction.TOGGLE_GLASS -> updateSettings { current ->
                current.copy(glass = current.glass.copy(enabled = !current.glass.enabled))
            }
            LauncherAction.OPEN_WALLPAPER_PICKER -> services.wallpaper.openWallpaperPicker(context)
            LauncherAction.OPEN_RECENTS -> SystemActions.openRecents(context)
        }
    }

    /** One-shot flags the drawer consumes when it opens. */
    var searchRequested by mutableStateOf(false)
    var showHiddenRequested by mutableStateOf(false)

    fun consumeSearchRequest(): Boolean = searchRequested.also { searchRequested = false }

    fun consumeHiddenRequest(): Boolean = showHiddenRequested.also { showHiddenRequested = false }

    // ------------------------------------------------------------------ maintenance

    fun resetEverything() {
        scope.launch {
            services.settings.reset()
            services.workspace.reset()
        }
    }
}

val LocalController = staticCompositionLocalOf<LauncherController> {
    error("LauncherController not provided")
}

/**
 * Settings are read on nearly every composable in the launcher, so the root collects the flow once
 * and publishes the snapshot here instead of every leaf collecting it again.
 */
val LocalSettings = staticCompositionLocalOf { LauncherSettings() }

val LocalBackdrop = staticCompositionLocalOf<BackdropState?> { null }
