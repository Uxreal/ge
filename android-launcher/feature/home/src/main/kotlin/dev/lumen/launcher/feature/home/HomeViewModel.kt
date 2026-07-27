package dev.lumen.launcher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lumen.launcher.core.data.apps.AppRepository
import dev.lumen.launcher.core.data.apps.UsageRepository
import dev.lumen.launcher.core.data.icons.IconCache
import dev.lumen.launcher.core.data.model.AppInfo
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.GridItem
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.data.model.WorkspaceState
import dev.lumen.launcher.core.data.prefs.PrefsRepository
import dev.lumen.launcher.core.data.prefs.PrefsSnapshot
import dev.lumen.launcher.core.data.wallpaper.WallpaperCoordinator
import dev.lumen.launcher.core.data.workspace.WorkspaceRepository
import dev.lumen.launcher.feature.home.layout.LayoutEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State holder for the home surface. Mutations delegate to [LayoutEngine] against the current grid
 * dimensions, which the UI reports through [onGridDimensions] — the engine needs rows, and rows are
 * derived from the window (§3), so the UI is the only party that knows them.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workspace: WorkspaceRepository,
    private val prefsRepo: PrefsRepository,
    private val appRepo: AppRepository,
    private val usage: UsageRepository,
    notificationDots: dev.lumen.launcher.core.data.notifications.NotificationDotsRepository,
    val iconCache: IconCache,
    val wallpaper: WallpaperCoordinator,
) : ViewModel() {

    val state: StateFlow<WorkspaceState> = workspace.state
    val prefs: StateFlow<PrefsSnapshot> = prefsRepo.prefs
    val apps: StateFlow<List<AppInfo>> = appRepo.apps

    /** D43: packages whose icons carry a dot right now. Empty when access or the toggle is off. */
    val dots: StateFlow<Set<String>> = notificationDots.packages

    /** The dock (D38): stored keys resolved against installed apps; uninstalled entries drop out. */
    fun dockApps(): List<AppInfo> {
        val index = apps.value.associateBy { it.key.flat }
        return prefs.value.dockKeys.mapNotNull { index[it] }
    }

    fun removeFromDock(key: dev.lumen.launcher.core.data.model.AppKey) =
        prefsRepo.removeDockKey(key.flat)

    @Volatile
    private var columns = 4

    @Volatile
    private var rows = 5

    private val model: HomeModel
        get() = prefs.value.homeModel ?: HomeModel.PACKED

    init {
        // First run: once the model is chosen and the app list is real, seed the workspace. Keyed
        // to the apps list itself, not a loading flag — a flag can wedge, a non-empty list cannot.
        // Also self-heals: a PACKED workspace that somehow persisted empty re-fills from installed
        // apps instead of greeting the user with bare wallpaper.
        viewModelScope.launch {
            combine(
                prefsRepo.prefs,
                appRepo.apps,
                workspace.ready,
                dimensionsReady,
            ) { p, apps, ready, dims ->
                Triple(p, apps, ready && dims)
            }.collect { (p, installedApps, ready) ->
                val model = p.homeModel ?: return@collect
                if (installedApps.isEmpty() || !ready) return@collect
                val current = workspace.state.value
                when {
                    !current.seeded -> seed(model)
                    model == HomeModel.PACKED && current.allApps().isEmpty() ->
                        workspace.mutate {
                            LayoutEngine.ensureAllApps(it, installedApps, columns, rows)
                        }
                    else -> Unit
                }
                // D41: once ever, pin the phone's factory basics — its actual default dialer,
                // messenger, browser and camera to the dock, clock and settings to the grid —
                // so the home screen starts the way the phone left the factory.
                if (!p.dockSeeded && workspace.state.value.seeded && !factorySeeded) {
                    factorySeeded = true
                    prefsRepo.setDockSeeded()
                    seedFactoryDefaults(model, installedApps)
                }
            }
        }
        viewModelScope.launch {
            appRepo.uninstalled.collect { key ->
                workspace.mutate { LayoutEngine.removeApp(it, key, model, columns, rows) }
            }
        }
        viewModelScope.launch {
            appRepo.installed.collect { key ->
                if (model == HomeModel.PACKED) {
                    workspace.mutate { LayoutEngine.appendApp(it, key, columns, rows) }
                }
                iconCache.onPackageChanged(key.packageName)
            }
        }
    }

    @Volatile
    private var factorySeeded = false

    /**
     * True once the composed grid has reported its real column/row counts. Anything that PLACES
     * items must wait for it: the 1.1.0 factory seeding ran against the 4×5 defaults, and on a
     * screen whose grid resolves shorter, the seeded items landed on rows that do not exist —
     * "the home screen apps are not on the screen", exactly.
     */
    private val dimensionsReady = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Settings → "Apply factory layout": the on-demand version of the one-time seeding. */
    fun applyFactoryLayout() {
        factorySeeded = true
        prefsRepo.setDockSeeded()
        seedFactoryDefaults(model, appRepo.apps.value)
    }

    private fun seedFactoryDefaults(
        model: HomeModel,
        installed: List<AppInfo>,
    ) {
        fun byPackage(pkg: String) =
            installed.firstOrNull { it.key.packageName == pkg && !it.isWorkProfile }

        val (dockPackages, gridPackages) = appRepo.factoryBasics()
        val pinned = prefs.value.dockKeys.toSet()
        dockPackages.mapNotNull(::byPackage)
            .filterNot { it.key.flat in pinned }
            .take((4 - pinned.size).coerceAtLeast(0))
            .forEach { app -> prefsRepo.addDockKey(app.key.flat) }
        // PACKED already carries every app; only FREEFORM needs the grid seeds. addToHome
        // dedupes, so re-applying never duplicates.
        if (model == HomeModel.FREEFORM) {
            gridPackages.mapNotNull(::byPackage).forEach { app -> addToHome(app.key) }
        }
    }

    private fun seed(model: HomeModel) {
        val installed = appRepo.apps.value
        workspace.replace(
            when (model) {
                HomeModel.PACKED -> LayoutEngine.seedPacked(installed, columns, rows)
                HomeModel.FREEFORM -> LayoutEngine.seedFreeform()
            },
        )
    }

    fun onGridDimensions(columns: Int, rows: Int) {
        val changed = this.columns != columns || this.rows != rows
        this.columns = columns
        this.rows = rows
        dimensionsReady.value = true
        if (changed && model == HomeModel.PACKED && workspace.state.value.seeded) {
            // A column-count change re-derives every flowing position; FREEFORM items stay put.
            workspace.mutate { LayoutEngine.compact(it, columns, rows) }
        }
        if (model == HomeModel.FREEFORM && workspace.state.value.seeded) {
            // Self-heal: rescue anything a past bug or a shrunken grid left outside the screen.
            workspace.mutate { LayoutEngine.reclaimOffGrid(it, columns, rows) }
        }
    }

    // ------------------------------------------------------------------ actions

    fun launch(key: AppKey, bounds: android.graphics.Rect?) {
        if (appRepo.launch(key, bounds)) usage.recordLaunch(key)
    }

    fun appInfo(key: AppKey, bounds: android.graphics.Rect?) = appRepo.openAppInfo(key, bounds)

    fun requestUninstall(key: AppKey) = appRepo.requestUninstall(key)

    fun canUninstall(key: AppKey) = appRepo.canUninstall(key)

    fun appLabel(key: AppKey): String = appRepo.app(key)?.label ?: key.packageName

    /**
     * FREEFORM "Add to Home" from the drawer: the app lands on the first page with room, at the
     * first free cell in flow order (bottom-up, §1). Already-placed apps are left alone.
     */
    fun addToHome(key: AppKey): Boolean {
        var added = false
        workspace.mutate { current ->
            if (current.containsApp(key)) return@mutate current
            var page = 0
            var cell: Cell? = null
            while (page < current.pageCount && cell == null) {
                cell = LayoutEngine.firstFree(current, page, columns, rows)
                if (cell == null) page += 1
            }
            val target = cell ?: LayoutEngine.cellAtFlowIndex(0, columns, rows)
            added = true
            current.copy(
                items = current.items + AppItem(LayoutEngine.newId("app"), page, target, key),
                pageCount = maxOf(current.pageCount, page + 1),
            )
        }
        return added
    }

    fun completeOnboarding() = prefsRepo.setOnboardingDone()

    fun move(id: String, page: Int, cell: Cell) =
        workspace.mutate { LayoutEngine.move(it, id, page, cell, model, columns, rows) }

    fun combine(targetId: String, sourceId: String) =
        workspace.mutate { LayoutEngine.combine(it, targetId, sourceId, model, columns, rows) }

    fun swap(idA: String, idB: String) = workspace.mutate { LayoutEngine.swap(it, idA, idB) }

    fun removeItem(id: String) =
        workspace.mutate { LayoutEngine.remove(it, id, model, columns, rows) }

    fun renameFolder(folderId: String, label: String) =
        workspace.mutate { LayoutEngine.renameFolder(it, folderId, label) }

    fun placeWidget(appWidgetId: Int, providerFlat: String, label: String, spanX: Int, spanY: Int): Boolean {
        var placed = false
        workspace.mutate { current ->
            val span = Cell(0, 0, spanX.coerceIn(1, columns), spanY.coerceIn(1, rows))
            var page = 0
            var cell: Cell? = null
            while (page < current.pageCount && cell == null) {
                cell = LayoutEngine.firstFree(current, page, columns, rows, span.spanX, span.spanY)
                if (cell == null) page += 1
            }
            val target = cell ?: Cell(0, 0, span.spanX, span.spanY).also { page = current.pageCount }
            placed = true
            LayoutEngine.place(
                current,
                WidgetItem(LayoutEngine.newId("widget"), page, target, appWidgetId, providerFlat, label),
                model,
                columns,
                rows,
            )
        }
        return placed
    }

    fun resizeWidget(id: String, spanX: Int, spanY: Int) {
        workspace.mutate { current ->
            val item = current.find(id) as? WidgetItem ?: return@mutate current
            val resized = item.cell.copy(
                spanX = spanX.coerceIn(1, columns - item.cell.x),
                spanY = spanY.coerceIn(1, rows - item.cell.y),
            )
            val without = current.copy(items = current.items.filterNot { it.id == id })
            if (!LayoutEngine.canPlace(without, item.page, resized, columns, rows)) {
                current // Overlap: refuse; the UI springs the handle back (§5 invalid drop).
            } else {
                val next = current.copy(
                    items = current.items.map { if (it.id == id) item.copy(cell = resized) else it },
                )
                if (model == HomeModel.PACKED) LayoutEngine.compact(next, columns, rows) else next
            }
        }
    }

    fun removeWidget(id: String, releaseId: (Int) -> Unit) {
        (workspace.state.value.find(id) as? WidgetItem)?.let { releaseId(it.appWidgetId) }
        removeItem(id)
    }

    fun setModel(newModel: HomeModel) {
        prefsRepo.setHomeModel(newModel)
        if (newModel == HomeModel.PACKED) {
            workspace.mutate { LayoutEngine.ensureAllApps(it, appRepo.apps.value, columns, rows) }
        }
    }

    /** A drop preview: where everything would land, without committing. Powers live reflow. */
    fun previewMove(id: String, page: Int, cell: Cell): WorkspaceState =
        LayoutEngine.move(state.value, id, page, cell, model, columns, rows)

    fun currentModel(): HomeModel = model
}
