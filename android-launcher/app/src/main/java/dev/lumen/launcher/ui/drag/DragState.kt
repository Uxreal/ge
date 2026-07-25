package dev.lumen.launcher.ui.drag

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import dev.lumen.launcher.data.model.AppItem
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.model.Cell
import dev.lumen.launcher.data.model.DeepShortcut
import dev.lumen.launcher.data.model.ItemContainer
import dev.lumen.launcher.data.model.ShortcutItem
import dev.lumen.launcher.data.model.WidgetItem
import dev.lumen.launcher.data.model.WorkspaceState
import dev.lumen.launcher.data.workspace.WorkspaceOps
import dev.lumen.launcher.ui.LauncherController

/** What is being dragged. */
sealed interface DragPayload {
    data class App(val key: AppKey) : DragPayload
    data class Folder(val folderId: String) : DragPayload
    data class Widget(val providerFlat: String, val appWidgetId: Int) : DragPayload
    data class Shortcut(val shortcut: DeepShortcut) : DragPayload
}

/**
 * One drag, from pick-up to drop.
 *
 * @param itemId set when an existing workspace item is being moved; null when the item is being
 *   dragged in fresh (from the app drawer, search results or the widget picker).
 */
data class DragSession(
    val payload: DragPayload,
    val itemId: String? = null,
    val source: ItemContainer? = null,
    val previewSizePx: Float = 0f,
    val grabOffset: Offset = Offset.Zero,
    val spanX: Int = 1,
    val spanY: Int = 1,
)

/** Where the finger currently hovers. Drop targets are published by whichever surface owns them. */
sealed interface DropTarget {
    data object None : DropTarget
    data class GridCell(val container: ItemContainer, val cell: Cell) : DropTarget

    /** Hovering another item — drop combines them into (or grows) a folder. */
    data class CombineWith(val itemId: String) : DropTarget
    data object Remove : DropTarget
    data object Uninstall : DropTarget
    data object NewPage : DropTarget
}

/**
 * Drag is cross-surface — an icon can leave the drawer, cross the workspace, hover a folder and
 * land in the dock — so the session lives above all of them, and each surface only has to say
 * "the finger is over me, here" and let [applyDrop] do the rest.
 */
@Stable
class DragState {
    var session: DragSession? by mutableStateOf(null)
        private set

    var pointer: Offset by mutableStateOf(Offset.Zero)
        private set

    var target: DropTarget by mutableStateOf(DropTarget.None)
        private set

    val isDragging: Boolean get() = session != null

    fun begin(session: DragSession, pointer: Offset) {
        this.session = session
        this.pointer = pointer
        this.target = DropTarget.None
    }

    fun move(pointer: Offset) {
        this.pointer = pointer
    }

    fun hover(target: DropTarget) {
        if (this.target != target) this.target = target
    }

    /** Ends the drag and hands back what happened, or null if nothing was being dragged. */
    fun finish(): Pair<DragSession, DropTarget>? {
        val current = session ?: return null
        val landed = target
        session = null
        target = DropTarget.None
        return current to landed
    }

    fun cancel() {
        session = null
        target = DropTarget.None
    }
}

val LocalDragState = staticCompositionLocalOf { DragState() }

/**
 * Applies a completed drag to the workspace. Centralised so the dock, the grid, folders and the
 * drawer all behave identically — including the awkward cases: dragging the last item out of a
 * folder, dropping onto an occupied cell, or dropping a brand-new app that has no item yet.
 */
fun applyDrop(controller: LauncherController, session: DragSession, target: DropTarget) {
    val settings = controller.settings
    val columns = settings.grid.columns
    val rows = settings.grid.rows

    when (target) {
        DropTarget.None -> Unit

        DropTarget.Remove -> session.itemId?.let { controller.removeItem(it) }

        DropTarget.Uninstall -> when (val payload = session.payload) {
            is DragPayload.App -> controller.uninstall(payload.key)
            else -> session.itemId?.let { controller.removeItem(it) }
        }

        DropTarget.NewPage -> controller.mutateWorkspace { state ->
            val grown = WorkspaceOps.addPage(state)
            val page = grown.pages.last()
            placeInto(grown, session, ItemContainer.Page(page.id), Cell(0, 0, session.spanX, session.spanY))
        }

        is DropTarget.CombineWith -> controller.mutateWorkspace { state ->
            val sourceId = session.itemId ?: run {
                // Fresh app dropped onto an existing icon: materialise it next to the target first.
                val located = WorkspaceOps.locate(state, target.itemId) ?: return@mutateWorkspace state
                val container = located.container
                val temp = newItem(session, located.item.cell)
                return@mutateWorkspace WorkspaceOps.combine(
                    state = WorkspaceOps.add(state, container, temp),
                    targetId = target.itemId,
                    sourceId = temp.id,
                    labelFor = { items -> folderLabel(controller, items) },
                )
            }
            WorkspaceOps.combine(
                state = state,
                targetId = target.itemId,
                sourceId = sourceId,
                labelFor = { items -> folderLabel(controller, items) },
            )
        }

        is DropTarget.GridCell -> controller.mutateWorkspace { state ->
            val container = target.container
            val existing = WorkspaceOps.itemsIn(state, container)
            val desired = target.cell.copy(spanX = session.spanX, spanY = session.spanY)
            val limitColumns = if (container is ItemContainer.Dock) settings.dock.columns else columns
            val limitRows = if (container is ItemContainer.Dock) settings.dock.rows else rows
            val cell = WorkspaceOps.nearestFreeCell(
                items = existing,
                preferred = desired.coerceInto(limitColumns, limitRows),
                columns = limitColumns,
                rows = limitRows,
                ignoreId = session.itemId,
            ) ?: return@mutateWorkspace state

            val moved = when {
                session.itemId != null && session.source is ItemContainer.Folder ->
                    WorkspaceOps.extractFromFolder(
                        state = state,
                        folderId = (session.source as ItemContainer.Folder).folderId,
                        itemId = session.itemId,
                        target = container,
                        cell = cell,
                    )

                session.itemId != null -> WorkspaceOps.move(state, session.itemId, container, cell)

                else -> placeInto(state, session, container, cell)
            }
            WorkspaceOps.dissolveThinFolders(moved)
        }
    }
}

private fun placeInto(
    state: WorkspaceState,
    session: DragSession,
    container: ItemContainer,
    cell: Cell,
): WorkspaceState = WorkspaceOps.add(state, container, newItem(session, cell))

private fun newItem(session: DragSession, cell: Cell) = when (val payload = session.payload) {
    is DragPayload.App -> AppItem(id = WorkspaceOps.newId("app"), cell = cell, key = payload.key)

    is DragPayload.Widget -> WidgetItem(
        id = WorkspaceOps.newId("widget"),
        cell = cell.copy(spanX = session.spanX, spanY = session.spanY),
        appWidgetId = payload.appWidgetId,
        providerFlat = payload.providerFlat,
    )

    is DragPayload.Shortcut -> ShortcutItem(
        id = WorkspaceOps.newId("shortcut"),
        cell = cell,
        packageName = payload.shortcut.packageName,
        shortcutId = payload.shortcut.id,
        label = payload.shortcut.shortLabel,
        profile = payload.shortcut.profile,
    )

    is DragPayload.Folder -> AppItem(
        id = WorkspaceOps.newId("app"),
        cell = cell,
        key = AppKey("", "", 0L),
    )
}

private fun folderLabel(controller: LauncherController, items: List<AppItem>): String =
    if (controller.settings.folders.autoLabel) {
        WorkspaceOps.autoLabel(items) { key ->
            controller.services.apps.app(key)?.category
                ?: dev.lumen.launcher.data.model.AppCategory.OTHER
        }
    } else {
        "Folder"
    }
