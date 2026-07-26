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
    val iconCache: IconCache,
    val wallpaper: WallpaperCoordinator,
) : ViewModel() {

    val state: StateFlow<WorkspaceState> = workspace.state
    val prefs: StateFlow<PrefsSnapshot> = prefsRepo.prefs
    val apps: StateFlow<List<AppInfo>> = appRepo.apps

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
            combine(prefsRepo.prefs, appRepo.apps, workspace.ready) { p, apps, ready ->
                Triple(p, apps, ready)
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
        if (changed && model == HomeModel.PACKED && workspace.state.value.seeded) {
            // A column-count change re-derives every flowing position; FREEFORM items stay put.
            workspace.mutate { LayoutEngine.compact(it, columns, rows) }
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
