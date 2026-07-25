package dev.lumen.launcher.feature.home.layout

import dev.lumen.launcher.core.data.model.AppInfo
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.GridItem
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.data.model.WorkspaceState
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

/**
 * Pure layout algebra for the home surface. Every drag, drop, folder operation and model switch
 * funnels through here, so §5's behaviour is testable as plain data transformation — no Android
 * types, no Compose.
 *
 * Two ideas carry the whole file:
 *
 *  * **Flow order.** §1's bottom-anchored grid gives every cell a flow index counted from the
 *    bottom-left, row by row upward. `PACKED` is "the items, in flow order": positions are derived,
 *    gaps are impossible, and a removal is just a list removal followed by [compact].
 *  * **Pinned items.** Widgets (and anything spanning) do not flow. They keep their cells, and the
 *    flowing items fill the free cells around them — §5's reflow policy in one sentence: `PACKED`
 *    reflows around the pinned, `FREEFORM` never moves anything the user placed.
 */
object LayoutEngine {

    private val counter = AtomicLong(0L)

    fun newId(prefix: String): String =
        "$prefix-${System.currentTimeMillis().toString(36)}-${counter.incrementAndGet().toString(36)}"

    // ------------------------------------------------------------------ flow order

    /** Index of a cell counted from the bottom-left, upward — §1's gravity as arithmetic. */
    fun flowIndex(cell: Cell, columns: Int, rows: Int): Int =
        (rows - 1 - cell.y) * columns + cell.x

    /** The cell for a flow index on an empty page. */
    fun cellAtFlowIndex(index: Int, columns: Int, rows: Int): Cell {
        val row = rows - 1 - index / columns
        return Cell(index % columns, row.coerceAtLeast(0))
    }

    /** True for items that hold their cell instead of flowing: widgets, and anything spanning. */
    fun isPinned(item: GridItem): Boolean =
        item is WidgetItem || item.cell.spanX > 1 || item.cell.spanY > 1

    // ------------------------------------------------------------------ occupancy

    fun canPlace(
        state: WorkspaceState,
        page: Int,
        cell: Cell,
        columns: Int,
        rows: Int,
        ignoreId: String? = null,
    ): Boolean {
        if (cell.x < 0 || cell.y < 0 || cell.right > columns || cell.bottom > rows) return false
        return state.itemsOn(page).none { it.id != ignoreId && it.cell.overlaps(cell) }
    }

    fun itemAt(state: WorkspaceState, page: Int, x: Int, y: Int): GridItem? =
        state.itemsOn(page).firstOrNull { it.cell.overlaps(Cell(x, y)) }

    /** Free cells of a page in flow order (bottom row first), around everything already placed. */
    fun freeCells(
        state: WorkspaceState,
        page: Int,
        columns: Int,
        rows: Int,
        ignoreId: String? = null,
    ): List<Cell> {
        val occupied = state.itemsOn(page).filter { it.id != ignoreId }
        return (0 until columns * rows)
            .map { cellAtFlowIndex(it, columns, rows) }
            .filter { candidate -> occupied.none { it.cell.overlaps(candidate) } }
    }

    fun firstFree(
        state: WorkspaceState,
        page: Int,
        columns: Int,
        rows: Int,
        spanX: Int = 1,
        spanY: Int = 1,
    ): Cell? {
        // Spans search top-down (widgets read best up top, under the Capsule); 1×1 follows gravity.
        if (spanX == 1 && spanY == 1) return freeCells(state, page, columns, rows).firstOrNull()
        for (y in 0..(rows - spanY)) {
            for (x in 0..(columns - spanX)) {
                val cell = Cell(x, y, spanX, spanY)
                if (canPlace(state, page, cell, columns, rows)) return cell
            }
        }
        return null
    }

    /** Nearest free cell to [preferred], searched in rings, so drops land where the finger was. */
    fun nearestFree(
        state: WorkspaceState,
        page: Int,
        preferred: Cell,
        columns: Int,
        rows: Int,
        ignoreId: String? = null,
    ): Cell? {
        if (canPlace(state, page, preferred, columns, rows, ignoreId)) return preferred
        for (radius in 1..maxOf(columns, rows)) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (maxOf(abs(dx), abs(dy)) != radius) continue
                    val candidate = preferred.copy(x = preferred.x + dx, y = preferred.y + dy)
                    if (canPlace(state, page, candidate, columns, rows, ignoreId)) return candidate
                }
            }
        }
        return null
    }

    // ------------------------------------------------------------------ compaction (PACKED)

    /**
     * Re-derives every flowing item's position from its order: page by page, each flowing item takes
     * the next free cell in flow order, overflow cascading to the next page. Pinned items never
     * move. This is both §5's gap-collapse and the overflow behaviour of a drop on a full page.
     */
    fun compact(state: WorkspaceState, columns: Int, rows: Int): WorkspaceState {
        val pinned = state.items.filter { isPinned(it) }
        val flowing = state.items.filterNot { isPinned(it) }
            .sortedWith(compareBy({ it.page }, { flowIndex(it.cell, columns, rows) }))
        return state.copy(items = emptyList()).withFlow(pinned, flowing, columns, rows)
    }

    /**
     * Places [orderedFlowing] into the free cells around [pinned], in exactly the given order —
     * the one primitive both [compact] (which sorts by current position first) and
     * [reorderPacked] (which supplies an explicit new order) share.
     */
    private fun WorkspaceState.withFlow(
        pinned: List<GridItem>,
        orderedFlowing: List<GridItem>,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        val placed = mutableListOf<GridItem>()
        placed += pinned
        var page = 0
        var queue = orderedFlowing
        while (queue.isNotEmpty() && page <= MAX_PAGES) {
            val occupancy = WorkspaceState(items = placed, pageCount = page + 1)
            val free = freeCells(occupancy, page, columns, rows)
            val take = queue.take(free.size)
            take.forEachIndexed { index, item -> placed += item.at(page, free[index]) }
            queue = queue.drop(take.size)
            page += 1
        }
        val usedPages = (placed.maxOfOrNull { it.page } ?: 0) + 1
        return copy(items = placed, pageCount = usedPages.coerceAtLeast(1))
    }

    // ------------------------------------------------------------------ mutations

    fun remove(state: WorkspaceState, id: String, model: HomeModel, columns: Int, rows: Int): WorkspaceState {
        val next = state.copy(
            items = state.items.mapNotNull { item ->
                when {
                    item.id == id -> null
                    item is FolderItem -> item.copy(items = item.items.filterNot { it.id == id })
                    else -> item
                }
            },
        )
        val dissolved = dissolveThin(next, model, columns, rows)
        return if (model == HomeModel.PACKED) compact(dissolved, columns, rows) else dissolved
    }

    /** Uninstall handling: every placement of the app goes, including inside folders. */
    fun removeApp(state: WorkspaceState, key: AppKey, model: HomeModel, columns: Int, rows: Int): WorkspaceState {
        val next = state.copy(
            items = state.items.mapNotNull { item ->
                when {
                    item is AppItem && item.key == key -> null
                    item is FolderItem -> item.copy(items = item.items.filterNot { it.key == key })
                    else -> item
                }
            },
        )
        val dissolved = dissolveThin(next, model, columns, rows)
        return if (model == HomeModel.PACKED) compact(dissolved, columns, rows) else dissolved
    }

    /**
     * The drop. `FREEFORM` places at the nearest free cell to the finger. `PACKED` treats the drop
     * cell as an *order* — the item is removed from the flow, re-inserted at the target's flow
     * position, and everything re-derives, which is what makes icons visibly make room.
     */
    fun move(
        state: WorkspaceState,
        id: String,
        targetPage: Int,
        targetCell: Cell,
        model: HomeModel,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        val fromFolder = state.folderOf(id)
        val item = state.find(id) ?: return state

        var working = if (fromFolder != null) {
            state.copy(
                items = state.items.map { candidate ->
                    if (candidate.id == fromFolder.id && candidate is FolderItem) {
                        candidate.copy(items = candidate.items.filterNot { it.id == id })
                    } else {
                        candidate
                    }
                },
            )
        } else {
            state
        }

        working = when (model) {
            HomeModel.FREEFORM -> {
                val cell = nearestFree(working, targetPage, targetCell, columns, rows, ignoreId = id)
                    ?: return state
                working.copy(
                    items = working.items.filterNot { it.id == id } + item.at(targetPage, cell),
                    pageCount = maxOf(working.pageCount, targetPage + 1),
                )
            }

            HomeModel.PACKED -> {
                if (isPinned(item)) {
                    val cell = nearestFree(working, targetPage, targetCell, columns, rows, ignoreId = id)
                        ?: return state
                    compact(
                        working.copy(
                            items = working.items.filterNot { it.id == id } + item.at(targetPage, cell),
                            pageCount = maxOf(working.pageCount, targetPage + 1),
                        ),
                        columns,
                        rows,
                    )
                } else {
                    reorderPacked(working, item, targetPage, targetCell, columns, rows)
                }
            }
        }

        return dissolveThin(working, model, columns, rows)
    }

    private fun reorderPacked(
        state: WorkspaceState,
        item: GridItem,
        targetPage: Int,
        targetCell: Cell,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        val others = state.items.filterNot { it.id == item.id }
        val flowing = others.filterNot { isPinned(it) }
            .sortedWith(compareBy({ it.page }, { flowIndex(it.cell, columns, rows) }))
        val targetFlow = flowIndex(targetCell.coerceInto(columns, rows), columns, rows)
        val insertAt = flowing.count { other ->
            other.page < targetPage ||
                (other.page == targetPage && flowIndex(other.cell, columns, rows) < targetFlow)
        }
        val reordered = flowing.toMutableList().apply { add(insertAt.coerceIn(0, size), item) }
        val pinned = others.filter { isPinned(it) }
        // Not compact(): the order here is the *new* order, and must not be re-derived from cells.
        return state.copy(
            items = emptyList(),
            pageCount = maxOf(state.pageCount, targetPage + 1),
        ).withFlow(pinned, reordered, columns, rows)
    }

    fun place(
        state: WorkspaceState,
        item: GridItem,
        model: HomeModel,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        val next = state.copy(
            items = state.items + item,
            pageCount = maxOf(state.pageCount, item.page + 1),
        )
        return if (model == HomeModel.PACKED && !isPinned(item)) compact(next, columns, rows) else next
    }

    /** Swap two 1×1 items — §5's 200ms hover-to-swap in `FREEFORM`. */
    fun swap(state: WorkspaceState, idA: String, idB: String): WorkspaceState {
        val a = state.find(idA) ?: return state
        val b = state.find(idB) ?: return state
        if (isPinned(a) || isPinned(b)) return state
        return state.copy(
            items = state.items.map {
                when (it.id) {
                    idA -> it.at(b.page, b.cell)
                    idB -> it.at(a.page, a.cell)
                    else -> it
                }
            },
        )
    }

    // ------------------------------------------------------------------ folders

    /** Dropping [sourceId] on [targetId]: makes a folder, or grows one. §5's 520ms dwell commits here. */
    fun combine(
        state: WorkspaceState,
        targetId: String,
        sourceId: String,
        model: HomeModel,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        if (targetId == sourceId) return state
        val target = state.find(targetId) ?: return state
        val source = state.find(sourceId) as? AppItem ?: return state

        val withoutSource = state.copy(
            items = state.items.mapNotNull { item ->
                when {
                    item.id == sourceId -> null
                    item is FolderItem -> item.copy(items = item.items.filterNot { it.id == sourceId })
                    else -> item
                }
            },
        )

        val next = when (target) {
            is FolderItem -> withoutSource.copy(
                items = withoutSource.items.map { item ->
                    if (item.id == targetId && item is FolderItem) {
                        item.copy(items = (item.items + source).distinctBy { it.key.flat })
                    } else {
                        item
                    }
                },
            )

            is AppItem -> {
                val folder = FolderItem(
                    id = newId("folder"),
                    page = target.page,
                    cell = target.cell.copy(spanX = 1, spanY = 1),
                    label = "",
                    items = listOf(target, source).distinctBy { it.key.flat },
                )
                withoutSource.copy(
                    items = withoutSource.items.map { if (it.id == targetId) folder else it },
                )
            }

            else -> return state
        }

        return if (model == HomeModel.PACKED) compact(next, columns, rows) else next
    }

    fun renameFolder(state: WorkspaceState, folderId: String, label: String): WorkspaceState =
        state.copy(
            items = state.items.map { item ->
                if (item.id == folderId && item is FolderItem) item.copy(label = label) else item
            },
        )

    /** Folders with one child unwrap; empty folders vanish. Nesting depth is 1 by construction (§6). */
    fun dissolveThin(state: WorkspaceState, model: HomeModel, columns: Int, rows: Int): WorkspaceState {
        var changed = false
        val next = state.copy(
            items = state.items.mapNotNull { item ->
                if (item is FolderItem) {
                    when (item.items.size) {
                        0 -> {
                            changed = true
                            null
                        }
                        1 -> {
                            changed = true
                            item.items.first().at(item.page, item.cell.copy(spanX = 1, spanY = 1))
                        }
                        else -> item
                    }
                } else {
                    item
                }
            },
        )
        if (!changed) return state
        return if (model == HomeModel.PACKED) compact(next, columns, rows) else next
    }

    // ------------------------------------------------------------------ pages

    fun trimTrailingEmptyPages(state: WorkspaceState): WorkspaceState {
        var pages = state.pageCount
        while (pages > 1 && state.items.none { it.page == pages - 1 }) pages -= 1
        return state.copy(pageCount = pages)
    }

    // ------------------------------------------------------------------ seeding & model switches

    /** First run in `PACKED`: every app, alphabetically, filling pages from the bottom up. */
    fun seedPacked(apps: List<AppInfo>, columns: Int, rows: Int): WorkspaceState {
        val perPage = (columns * rows).coerceAtLeast(1)
        val items = apps.sortedBy { it.label.lowercase() }.mapIndexed { index, app ->
            AppItem(
                id = newId("app"),
                page = index / perPage,
                cell = cellAtFlowIndex(index % perPage, columns, rows),
                key = app.key,
            )
        }
        return WorkspaceState(
            items = items,
            pageCount = if (items.isEmpty()) 1 else (items.size - 1) / perPage + 1,
            seeded = true,
        )
    }

    /** First run in `FREEFORM`: an empty page; the drawer holds the apps. */
    fun seedFreeform(): WorkspaceState = WorkspaceState(items = emptyList(), pageCount = 1, seeded = true)

    /**
     * §5: models are switchable without data loss. To `PACKED`, every installed app must be on a
     * page, so missing ones append in flow order; to `FREEFORM` nothing changes — positions survive
     * verbatim and the drawer simply exists again.
     */
    fun ensureAllApps(
        state: WorkspaceState,
        apps: List<AppInfo>,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        val missing = apps
            .filterNot { state.containsApp(it.key) }
            .sortedBy { it.label.lowercase() }
        var working = state
        for (app in missing) {
            working = appendApp(working, app.key, columns, rows)
        }
        return compact(working, columns, rows)
    }

    /** Newly installed app in `PACKED` lands at the end of the flow. */
    fun appendApp(state: WorkspaceState, key: AppKey, columns: Int, rows: Int): WorkspaceState {
        if (state.containsApp(key)) return state
        var page = state.pageCount - 1
        var cell = firstFree(state, page, columns, rows)
        if (cell == null) {
            page += 1
            cell = cellAtFlowIndex(0, columns, rows)
        }
        return state.copy(
            items = state.items + AppItem(newId("app"), page, cell, key),
            pageCount = maxOf(state.pageCount, page + 1),
        )
    }

    private const val MAX_PAGES = 64
}
