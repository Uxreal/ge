package dev.lumen.launcher.data.workspace

import dev.lumen.launcher.data.model.AppCategory
import dev.lumen.launcher.data.model.AppInfo
import dev.lumen.launcher.data.model.AppItem
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.model.Cell
import dev.lumen.launcher.data.model.FolderItem
import dev.lumen.launcher.data.model.ItemContainer
import dev.lumen.launcher.data.model.PageState
import dev.lumen.launcher.data.model.WidgetItem
import dev.lumen.launcher.data.model.WorkspaceItem
import dev.lumen.launcher.data.model.WorkspaceState

/**
 * Pure, side-effect free layout algebra for the workspace. Every mutation the UI performs
 * (drag, drop, folder creation, grid resize, page add/remove) funnels through here so the drag
 * layer stays dumb and the behaviour stays testable.
 */
object WorkspaceOps {

    data class Located(
        val container: ItemContainer,
        val item: WorkspaceItem,
        val indexInContainer: Int,
    )

    private var idCounter = 0L

    fun newId(prefix: String): String {
        idCounter += 1
        return "$prefix-${System.currentTimeMillis().toString(36)}-${idCounter.toString(36)}"
    }

    // ---------------------------------------------------------------- lookup

    fun locate(state: WorkspaceState, id: String): Located? {
        state.dock.forEachIndexed { i, item ->
            if (item.id == id) return Located(ItemContainer.Dock, item, i)
        }
        state.pages.forEach { page ->
            page.items.forEachIndexed { i, item ->
                if (item.id == id) return Located(ItemContainer.Page(page.id), item, i)
            }
        }
        state.pages.forEach { page ->
            page.items.filterIsInstance<FolderItem>().forEach { folder ->
                folder.items.forEachIndexed { i, child ->
                    if (child.id == id) return Located(ItemContainer.Folder(folder.id), child, i)
                }
            }
        }
        state.dock.filterIsInstance<FolderItem>().forEach { folder ->
            folder.items.forEachIndexed { i, child ->
                if (child.id == id) return Located(ItemContainer.Folder(folder.id), child, i)
            }
        }
        return null
    }

    fun folder(state: WorkspaceState, folderId: String): FolderItem? =
        (state.pages.asSequence().flatMap { it.items.asSequence() } + state.dock.asSequence())
            .filterIsInstance<FolderItem>()
            .firstOrNull { it.id == folderId }

    fun itemsIn(state: WorkspaceState, container: ItemContainer): List<WorkspaceItem> = when (container) {
        is ItemContainer.Dock -> state.dock
        is ItemContainer.Page -> state.pages.firstOrNull { it.id == container.pageId }?.items.orEmpty()
        is ItemContainer.Folder -> folder(state, container.folderId)?.items.orEmpty()
    }

    fun containsApp(state: WorkspaceState, key: AppKey): Boolean =
        allAppItems(state).any { it.key == key }

    fun allAppItems(state: WorkspaceState): List<AppItem> {
        val out = mutableListOf<AppItem>()
        fun collect(items: List<WorkspaceItem>) {
            items.forEach { item ->
                when (item) {
                    is AppItem -> out += item
                    is FolderItem -> out += item.items
                    else -> Unit
                }
            }
        }
        state.pages.forEach { collect(it.items) }
        collect(state.dock)
        return out
    }

    fun widgetIds(state: WorkspaceState): List<Int> =
        (state.pages.flatMap { it.items } + state.dock)
            .filterIsInstance<WidgetItem>()
            .map { it.appWidgetId }

    // ---------------------------------------------------------------- occupancy

    fun canPlace(
        items: List<WorkspaceItem>,
        cell: Cell,
        columns: Int,
        rows: Int,
        ignoreId: String? = null,
    ): Boolean {
        if (cell.x < 0 || cell.y < 0 || cell.right > columns || cell.bottom > rows) return false
        return items.none { it.id != ignoreId && it.cell.overlaps(cell) }
    }

    fun itemAt(items: List<WorkspaceItem>, x: Int, y: Int): WorkspaceItem? =
        items.firstOrNull { it.cell.overlaps(Cell(x, y, 1, 1)) }

    fun firstFreeCell(
        items: List<WorkspaceItem>,
        columns: Int,
        rows: Int,
        spanX: Int = 1,
        spanY: Int = 1,
    ): Cell? {
        for (y in 0..(rows - spanY)) {
            for (x in 0..(columns - spanX)) {
                val candidate = Cell(x, y, spanX, spanY)
                if (canPlace(items, candidate, columns, rows)) return candidate
            }
        }
        return null
    }

    /** Nearest free cell to [preferred], searched in rings so drops land where the finger was. */
    fun nearestFreeCell(
        items: List<WorkspaceItem>,
        preferred: Cell,
        columns: Int,
        rows: Int,
        ignoreId: String? = null,
    ): Cell? {
        if (canPlace(items, preferred, columns, rows, ignoreId)) return preferred
        val maxRadius = maxOf(columns, rows)
        for (radius in 1..maxRadius) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != radius) continue
                    val candidate = preferred.copy(x = preferred.x + dx, y = preferred.y + dy)
                    if (canPlace(items, candidate, columns, rows, ignoreId)) return candidate
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------------- mutation

    private fun mapContainer(
        state: WorkspaceState,
        container: ItemContainer,
        transform: (List<WorkspaceItem>) -> List<WorkspaceItem>,
    ): WorkspaceState = when (container) {
        is ItemContainer.Dock -> state.copy(dock = transform(state.dock))
        is ItemContainer.Page -> state.copy(
            pages = state.pages.map { page ->
                if (page.id == container.pageId) page.copy(items = transform(page.items)) else page
            },
        )
        is ItemContainer.Folder -> mapFolder(state, container.folderId) { folder ->
            folder.copy(items = transform(folder.items).filterIsInstance<AppItem>())
        }
    }

    private fun mapFolder(
        state: WorkspaceState,
        folderId: String,
        transform: (FolderItem) -> WorkspaceItem,
    ): WorkspaceState {
        fun mapList(items: List<WorkspaceItem>) = items.map { item ->
            if (item is FolderItem && item.id == folderId) transform(item) else item
        }
        return state.copy(
            pages = state.pages.map { it.copy(items = mapList(it.items)) },
            dock = mapList(state.dock),
        )
    }

    fun remove(state: WorkspaceState, id: String): WorkspaceState {
        fun strip(items: List<WorkspaceItem>) = items.mapNotNull { item ->
            when {
                item.id == id -> null
                item is FolderItem -> item.copy(items = item.items.filterNot { it.id == id })
                else -> item
            }
        }
        return state.copy(
            pages = state.pages.map { it.copy(items = strip(it.items)) },
            dock = strip(state.dock),
        )
    }

    fun removeAppEverywhere(state: WorkspaceState, key: AppKey): WorkspaceState {
        fun strip(items: List<WorkspaceItem>) = items.mapNotNull { item ->
            when {
                item is AppItem && item.key == key -> null
                item is FolderItem -> item.copy(items = item.items.filterNot { it.key == key })
                else -> item
            }
        }
        return state.copy(
            pages = state.pages.map { it.copy(items = strip(it.items)) },
            dock = strip(state.dock),
        )
    }

    fun update(
        state: WorkspaceState,
        id: String,
        transform: (WorkspaceItem) -> WorkspaceItem,
    ): WorkspaceState {
        fun mapList(items: List<WorkspaceItem>) = items.map { item ->
            when {
                item.id == id -> transform(item)
                item is FolderItem && item.items.any { it.id == id } ->
                    item.copy(
                        items = item.items.map { child ->
                            if (child.id == id) transform(child) as? AppItem ?: child else child
                        },
                    )
                else -> item
            }
        }
        return state.copy(
            pages = state.pages.map { it.copy(items = mapList(it.items)) },
            dock = mapList(state.dock),
        )
    }

    /** Moves [id] into [target] at [cell], removing it from wherever it currently lives. */
    fun move(
        state: WorkspaceState,
        id: String,
        target: ItemContainer,
        cell: Cell,
    ): WorkspaceState {
        val located = locate(state, id) ?: return state
        val item = located.item.withCell(cell)
        if (target is ItemContainer.Folder && item !is AppItem) return state
        val without = remove(state, id)
        return mapContainer(without, target) { items -> items + item }
    }

    /** Reorders within a linear container (dock, folder) by index. */
    fun reorder(
        state: WorkspaceState,
        container: ItemContainer,
        fromIndex: Int,
        toIndex: Int,
    ): WorkspaceState = mapContainer(state, container) { items ->
        if (fromIndex !in items.indices) {
            items
        } else {
            val mutable = items.toMutableList()
            val moved = mutable.removeAt(fromIndex)
            mutable.add(toIndex.coerceIn(0, mutable.size), moved)
            mutable.mapIndexed { index, item -> item.withCell(item.cell.copy(x = index, y = 0)) }
        }
    }

    fun add(state: WorkspaceState, container: ItemContainer, item: WorkspaceItem): WorkspaceState =
        mapContainer(state, container) { items -> items + item }

    // ---------------------------------------------------------------- folders

    /** Drops [sourceId] onto [targetId]: makes a folder, or grows an existing one. */
    fun combine(
        state: WorkspaceState,
        targetId: String,
        sourceId: String,
        labelFor: (List<AppItem>) -> String = { "Folder" },
    ): WorkspaceState {
        if (targetId == sourceId) return state
        val target = locate(state, targetId) ?: return state
        val source = locate(state, sourceId) ?: return state
        val sourceApps: List<AppItem> = when (val item = source.item) {
            is AppItem -> listOf(item)
            is FolderItem -> item.items
            else -> return state
        }

        return when (val targetItem = target.item) {
            is FolderItem -> {
                val merged = (targetItem.items + sourceApps).distinctBy { it.key.flat }
                remove(state, sourceId).let { cleaned ->
                    mapFolder(cleaned, targetId) { folder ->
                        folder.copy(
                            items = merged,
                            label = if (folder.autoLabelled) labelFor(merged) else folder.label,
                        )
                    }
                }
            }
            is AppItem -> {
                val contents = (listOf(targetItem) + sourceApps).distinctBy { it.key.flat }
                val folder = FolderItem(
                    id = newId("folder"),
                    cell = targetItem.cell.copy(spanX = 1, spanY = 1),
                    label = labelFor(contents),
                    items = contents,
                )
                var next = remove(state, sourceId)
                next = remove(next, targetId)
                mapContainer(next, target.container) { items -> items + folder }
            }
            else -> state
        }
    }

    /** Pulls an app back out of a folder onto [target]; dissolves folders left with one child. */
    fun extractFromFolder(
        state: WorkspaceState,
        folderId: String,
        itemId: String,
        target: ItemContainer,
        cell: Cell,
    ): WorkspaceState {
        val folder = folder(state, folderId) ?: return state
        val child = folder.items.firstOrNull { it.id == itemId } ?: return state
        var next = mapFolder(state, folderId) { f -> f.copy(items = f.items.filterNot { it.id == itemId }) }
        next = mapContainer(next, target) { items -> items + child.withCell(cell) }
        return dissolveThinFolders(next)
    }

    fun dissolveThinFolders(state: WorkspaceState): WorkspaceState {
        fun mapList(items: List<WorkspaceItem>) = items.mapNotNull { item ->
            if (item is FolderItem) {
                when (item.items.size) {
                    0 -> null
                    1 -> item.items.first().withCell(item.cell.copy(spanX = 1, spanY = 1))
                    else -> item
                }
            } else {
                item
            }
        }
        return state.copy(
            pages = state.pages.map { it.copy(items = mapList(it.items)) },
            dock = mapList(state.dock),
        )
    }

    /** Names a folder after the dominant category of its contents, Apple-style. */
    fun autoLabel(items: List<AppItem>, categoryOf: (AppKey) -> AppCategory): String {
        if (items.isEmpty()) return "Folder"
        val dominant = items
            .groupingBy { categoryOf(it.key) }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: AppCategory.OTHER
        return when (dominant) {
            AppCategory.SOCIAL -> "Social"
            AppCategory.COMMUNICATION -> "Messaging"
            AppCategory.MEDIA -> "Media"
            AppCategory.PHOTOGRAPHY -> "Photos"
            AppCategory.PRODUCTIVITY -> "Work"
            AppCategory.GAMES -> "Games"
            AppCategory.NEWS -> "Reading"
            AppCategory.SHOPPING -> "Shopping"
            AppCategory.TRAVEL -> "Travel"
            AppCategory.FINANCE -> "Finance"
            AppCategory.HEALTH -> "Health"
            AppCategory.EDUCATION -> "Learning"
            AppCategory.UTILITIES -> "Tools"
            AppCategory.SYSTEM -> "System"
            AppCategory.OTHER -> "Folder"
        }
    }

    // ---------------------------------------------------------------- pages

    fun addPage(state: WorkspaceState, atIndex: Int = state.pages.size): WorkspaceState {
        val pages = state.pages.toMutableList()
        pages.add(atIndex.coerceIn(0, pages.size), PageState(id = newId("page")))
        return state.copy(pages = pages)
    }

    fun removePage(state: WorkspaceState, pageId: String): WorkspaceState {
        if (state.pages.size <= 1) return state
        return state.copy(pages = state.pages.filterNot { it.id == pageId })
    }

    fun movePage(state: WorkspaceState, from: Int, to: Int): WorkspaceState {
        if (from !in state.pages.indices) return state
        val pages = state.pages.toMutableList()
        val moved = pages.removeAt(from)
        pages.add(to.coerceIn(0, pages.size), moved)
        return state.copy(pages = pages)
    }

    fun trimTrailingEmptyPages(state: WorkspaceState): WorkspaceState {
        val pages = state.pages.toMutableList()
        while (pages.size > 1 && pages.last().items.isEmpty()) pages.removeAt(pages.lastIndex)
        return state.copy(pages = pages)
    }

    /** Re-flows everything into a (possibly smaller) grid, spilling overflow onto new pages. */
    fun reflow(state: WorkspaceState, columns: Int, rows: Int, dockColumns: Int): WorkspaceState {
        val pages = mutableListOf<MutableList<WorkspaceItem>>()
        fun sink(item: WorkspaceItem) {
            val clamped = item.cell.coerceInto(columns, rows)
            for (page in pages) {
                val cell = nearestFreeCell(page, clamped, columns, rows, item.id)
                if (cell != null) {
                    page += item.withCell(cell)
                    return
                }
            }
            val fresh = mutableListOf<WorkspaceItem>()
            val cell = firstFreeCell(fresh, columns, rows, clamped.spanX, clamped.spanY)
                ?: Cell(0, 0, 1, 1)
            fresh += item.withCell(cell)
            pages += fresh
        }

        state.pages.forEach { page ->
            pages += mutableListOf<WorkspaceItem>()
            page.items
                .sortedWith(compareBy({ it.cell.y }, { it.cell.x }))
                .forEach { sink(it) }
        }
        // Widgets and icons that no longer fit spill into fresh pages via sink() above.
        val newPages = pages.mapIndexed { index, items ->
            PageState(
                id = state.pages.getOrNull(index)?.id ?: newId("page"),
                items = items,
            )
        }.ifEmpty { listOf(PageState(newId("page"))) }

        val dock = state.dock
            .sortedBy { it.cell.x }
            .take(dockColumns)
            .mapIndexed { index, item -> item.withCell(Cell(index, 0, 1, 1)) }

        return trimTrailingEmptyPages(state.copy(pages = newPages, dock = dock))
    }

    // ---------------------------------------------------------------- bootstrap

    private val DOCK_PRIORITY = listOf(
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
        "com.google.android.apps.messaging", "com.android.messaging", "com.whatsapp",
        "com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx",
        "com.google.android.GoogleCamera", "com.android.camera2", "com.sec.android.app.camera",
    )

    /**
     * First-run layout: a curated dock (phone / messages / browser / camera when present) and the
     * remaining apps laid out alphabetically across as many pages as they need.
     */
    fun defaultLayout(
        apps: List<AppInfo>,
        columns: Int,
        rows: Int,
        dockColumns: Int,
    ): WorkspaceState {
        val sorted = apps.sortedBy { it.label.lowercase() }
        val dockPicks = mutableListOf<AppInfo>()
        DOCK_PRIORITY.forEach { pkg ->
            if (dockPicks.size >= dockColumns) return@forEach
            sorted.firstOrNull { it.packageName == pkg && dockPicks.none { p -> p.key == it.key } }
                ?.let { dockPicks += it }
        }
        if (dockPicks.size < dockColumns) {
            sorted.asSequence()
                .filter { !it.isSystemApp && dockPicks.none { p -> p.key == it.key } }
                .take(dockColumns - dockPicks.size)
                .forEach { dockPicks += it }
        }

        val dock = dockPicks.mapIndexed { index, app ->
            AppItem(id = newId("app"), cell = Cell(index, 0), key = app.key)
        }

        val remaining = sorted.filter { app -> dockPicks.none { it.key == app.key } }
        val perPage = (columns * rows).coerceAtLeast(1)
        val pages = remaining.chunked(perPage).mapIndexed { pageIndex, chunk ->
            PageState(
                id = "page-$pageIndex",
                items = chunk.mapIndexed { i, app ->
                    AppItem(
                        id = newId("app"),
                        cell = Cell(i % columns, i / columns),
                        key = app.key,
                    )
                },
            )
        }.ifEmpty { listOf(PageState("page-0")) }

        return WorkspaceState(pages = pages, dock = dock, initialized = true)
    }

    /** Places freshly installed apps on the first page with room (or a new trailing page). */
    fun autoPlace(
        state: WorkspaceState,
        keys: List<AppKey>,
        columns: Int,
        rows: Int,
    ): WorkspaceState {
        var next = state
        keys.filterNot { containsApp(next, it) }.forEach { key ->
            var placed = false
            for (page in next.pages) {
                val cell = firstFreeCell(page.items, columns, rows)
                if (cell != null) {
                    next = add(
                        next,
                        ItemContainer.Page(page.id),
                        AppItem(id = newId("app"), cell = cell, key = key),
                    )
                    placed = true
                    break
                }
            }
            if (!placed) {
                next = addPage(next)
                val page = next.pages.last()
                next = add(
                    next,
                    ItemContainer.Page(page.id),
                    AppItem(id = newId("app"), cell = Cell(0, 0), key = key),
                )
            }
        }
        return next
    }
}
