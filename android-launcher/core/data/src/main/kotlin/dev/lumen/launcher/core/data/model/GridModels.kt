package dev.lumen.launcher.core.data.model

/** §5: the two first-class home models, chosen at setup and switchable without data loss. */
enum class HomeModel { PACKED, FREEFORM }

/** Grid position and span, in cell units. Origin is the top-left of the page's grid. */
data class Cell(
    val x: Int,
    val y: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
) {
    val right: Int get() = x + spanX
    val bottom: Int get() = y + spanY

    fun overlaps(other: Cell): Boolean =
        x < other.right && other.x < right && y < other.bottom && other.y < bottom

    fun coerceInto(columns: Int, rows: Int): Cell {
        val sx = spanX.coerceIn(1, columns)
        val sy = spanY.coerceIn(1, rows)
        return copy(
            x = x.coerceIn(0, (columns - sx).coerceAtLeast(0)),
            y = y.coerceIn(0, (rows - sy).coerceAtLeast(0)),
            spanX = sx,
            spanY = sy,
        )
    }
}

/**
 * One item in the sparse (page, x, y) map from §5. Folder children live inside [FolderItem] in
 * order, not on the grid.
 */
sealed interface GridItem {
    val id: String
    val page: Int
    val cell: Cell

    fun at(page: Int, cell: Cell): GridItem
}

data class AppItem(
    override val id: String,
    override val page: Int,
    override val cell: Cell,
    val key: AppKey,
) : GridItem {
    override fun at(page: Int, cell: Cell): AppItem = copy(page = page, cell = cell)
}

data class FolderItem(
    override val id: String,
    override val page: Int,
    override val cell: Cell,
    val label: String = "",
    val items: List<AppItem> = emptyList(),
) : GridItem {
    override fun at(page: Int, cell: Cell): FolderItem = copy(page = page, cell = cell)
}

data class WidgetItem(
    override val id: String,
    override val page: Int,
    override val cell: Cell,
    val appWidgetId: Int,
    val providerFlat: String,
    val label: String = "",
) : GridItem {
    override fun at(page: Int, cell: Cell): WidgetItem = copy(page = page, cell = cell)
}

data class ShortcutItem(
    override val id: String,
    override val page: Int,
    override val cell: Cell,
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val profile: Long = 0L,
) : GridItem {
    override fun at(page: Int, cell: Cell): ShortcutItem = copy(page = page, cell = cell)
}

/** The whole home surface. [pageCount] is explicit so trailing empty pages survive process death. */
data class WorkspaceState(
    val items: List<GridItem> = emptyList(),
    val pageCount: Int = 1,
    val seeded: Boolean = false,
) {
    fun itemsOn(page: Int): List<GridItem> = items.filter { it.page == page }

    fun find(id: String): GridItem? {
        items.forEach { item ->
            if (item.id == id) return item
            if (item is FolderItem) item.items.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    fun folderOf(childId: String): FolderItem? =
        items.filterIsInstance<FolderItem>().firstOrNull { folder ->
            folder.items.any { it.id == childId }
        }

    fun allApps(): List<AppItem> = buildList {
        items.forEach { item ->
            when (item) {
                is AppItem -> add(item)
                is FolderItem -> addAll(item.items)
                else -> Unit
            }
        }
    }

    fun containsApp(key: AppKey): Boolean = allApps().any { it.key == key }
}
