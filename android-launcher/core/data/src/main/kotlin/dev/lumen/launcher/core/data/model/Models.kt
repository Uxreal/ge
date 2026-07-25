package dev.lumen.launcher.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Stable identity for a launchable activity. [profile] is the `UserHandle` serial number so that a
 * work-profile clone of an app never collides with its personal counterpart.
 */
@Serializable
data class AppKey(
    val packageName: String,
    val activityName: String,
    val profile: Long = 0L,
) {
    /** Round-trippable string form, used as a map/persistence key. */
    val flat: String get() = "$packageName/$activityName@$profile"

    companion object {
        fun parse(flat: String): AppKey? {
            val at = flat.lastIndexOf('@')
            if (at <= 0) return null
            val profile = flat.substring(at + 1).toLongOrNull() ?: return null
            val slash = flat.indexOf('/')
            if (slash <= 0 || slash > at) return null
            return AppKey(
                packageName = flat.substring(0, slash),
                activityName = flat.substring(slash + 1, at),
                profile = profile,
            )
        }
    }
}

enum class AppCategory {
    SOCIAL, COMMUNICATION, MEDIA, PHOTOGRAPHY, PRODUCTIVITY, GAMES, NEWS,
    SHOPPING, TRAVEL, FINANCE, HEALTH, EDUCATION, UTILITIES, SYSTEM, OTHER;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** A launchable app as surfaced by `LauncherApps`. Icons are fetched separately via `IconLoader`. */
data class AppInfo(
    val key: AppKey,
    val label: String,
    /** Lowercase, diacritic-stripped label plus initials — precomputed for search. */
    val searchTokens: String,
    val isSystemApp: Boolean,
    val isWorkProfile: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val category: AppCategory,
) {
    val packageName: String get() = key.packageName
}

/** Grid position and span, in cell units. */
@Serializable
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

@Serializable
sealed interface WorkspaceItem {
    val id: String
    val cell: Cell

    fun withCell(cell: Cell): WorkspaceItem
}

@Serializable
@SerialName("app")
data class AppItem(
    override val id: String,
    override val cell: Cell = Cell(0, 0),
    val key: AppKey,
) : WorkspaceItem {
    override fun withCell(cell: Cell): AppItem = copy(cell = cell)
}

@Serializable
@SerialName("folder")
data class FolderItem(
    override val id: String,
    override val cell: Cell = Cell(0, 0),
    val label: String = "",
    val items: List<AppItem> = emptyList(),
    /** ARGB tint; 0 means "derive from contents". */
    val tint: Int = 0,
    val autoLabelled: Boolean = true,
) : WorkspaceItem {
    override fun withCell(cell: Cell): FolderItem = copy(cell = cell)
}

@Serializable
@SerialName("widget")
data class WidgetItem(
    override val id: String,
    override val cell: Cell = Cell(0, 0, 2, 2),
    val appWidgetId: Int,
    val providerFlat: String,
    val label: String = "",
    val profile: Long = 0L,
) : WorkspaceItem {
    override fun withCell(cell: Cell): WidgetItem = copy(cell = cell)
}

@Serializable
@SerialName("shortcut")
data class ShortcutItem(
    override val id: String,
    override val cell: Cell = Cell(0, 0),
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val profile: Long = 0L,
) : WorkspaceItem {
    override fun withCell(cell: Cell): ShortcutItem = copy(cell = cell)
}

@Serializable
data class PageState(
    val id: String,
    val items: List<WorkspaceItem> = emptyList(),
)

@Serializable
data class WorkspaceState(
    val version: Int = 1,
    val pages: List<PageState> = emptyList(),
    val dock: List<WorkspaceItem> = emptyList(),
    val initialized: Boolean = false,
) {
    val pageCount: Int get() = pages.size
}

/** Where an item lives. Used by drag-and-drop and by the pure ops in [dev.lumen.launcher.data.workspace]. */
sealed interface ItemContainer {
    data class Page(val pageId: String) : ItemContainer
    data object Dock : ItemContainer
    data class Folder(val folderId: String) : ItemContainer
}

/** A deep shortcut published by an app (long-press menu entries). */
data class DeepShortcut(
    val id: String,
    val packageName: String,
    val shortLabel: String,
    val longLabel: String?,
    val rank: Int,
    val enabled: Boolean,
    val profile: Long,
)

/** A bindable app-widget provider, flattened for the picker UI. */
data class WidgetProviderInfo(
    val providerFlat: String,
    val label: String,
    val packageName: String,
    val appLabel: String,
    val minWidthPx: Int,
    val minHeightPx: Int,
    val maxResizeWidthPx: Int,
    val maxResizeHeightPx: Int,
    val resizeHorizontal: Boolean,
    val resizeVertical: Boolean,
    val configurable: Boolean,
    val profile: Long,
    val description: String?,
)
