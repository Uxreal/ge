package dev.lumen.launcher.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.GridItem
import dev.lumen.launcher.core.data.model.ShortcutItem
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.data.model.WorkspaceState
import kotlinx.coroutines.flow.Flow

/**
 * One row per grid item; folder children are rows whose [folderId] points at the parent and whose
 * [position] orders them. §5: every mutation is a transaction, and the layout must be readable and
 * drawable on the first frame after process death — hence the single flat table with no joins.
 */
@Entity(tableName = "grid_items")
data class GridItemEntity(
    @PrimaryKey val id: String,
    val kind: String,             // app | folder | widget | shortcut
    val page: Int,
    val x: Int,
    val y: Int,
    val spanX: Int,
    val spanY: Int,
    val folderId: String?,        // parent folder, null for items directly on the grid
    val position: Int,            // order inside the folder; 0 for grid items
    val packageName: String?,
    val activityName: String?,
    val profile: Long,
    val label: String?,
    val appWidgetId: Int?,
    val providerFlat: String?,
    val shortcutId: String?,
)

/** Single-row bookkeeping for the workspace itself. */
@Entity(tableName = "workspace_meta")
data class WorkspaceMetaEntity(
    @PrimaryKey val id: Int = 0,
    val pageCount: Int,
    val seeded: Boolean,
)

@Dao
interface GridDao {
    @Query("SELECT * FROM grid_items ORDER BY page, y, x, position")
    fun observeItems(): Flow<List<GridItemEntity>>

    @Query("SELECT * FROM grid_items ORDER BY page, y, x, position")
    suspend fun items(): List<GridItemEntity>

    @Query("SELECT * FROM workspace_meta WHERE id = 0")
    suspend fun meta(): WorkspaceMetaEntity?

    @Query("SELECT * FROM workspace_meta WHERE id = 0")
    fun observeMeta(): Flow<WorkspaceMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GridItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: WorkspaceMetaEntity)

    @Query("DELETE FROM grid_items")
    suspend fun clear()

    /** The whole layout swaps atomically; a crash mid-write can never leave half a home screen. */
    @Transaction
    suspend fun replaceAll(items: List<GridItemEntity>, meta: WorkspaceMetaEntity) {
        clear()
        insertAll(items)
        putMeta(meta)
    }
}

@Database(
    entities = [GridItemEntity::class, WorkspaceMetaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun gridDao(): GridDao
}

// ---------------------------------------------------------------------- mapping

fun WorkspaceState.toEntities(): List<GridItemEntity> = buildList {
    for (item in items) {
        when (item) {
            is AppItem -> add(item.toEntity(folderId = null, position = 0))
            is FolderItem -> {
                add(
                    GridItemEntity(
                        id = item.id, kind = "folder", page = item.page,
                        x = item.cell.x, y = item.cell.y, spanX = item.cell.spanX, spanY = item.cell.spanY,
                        folderId = null, position = 0, packageName = null, activityName = null,
                        profile = 0L, label = item.label, appWidgetId = null, providerFlat = null,
                        shortcutId = null,
                    ),
                )
                item.items.forEachIndexed { index, child ->
                    add(child.toEntity(folderId = item.id, position = index))
                }
            }
            is WidgetItem -> add(
                GridItemEntity(
                    id = item.id, kind = "widget", page = item.page,
                    x = item.cell.x, y = item.cell.y, spanX = item.cell.spanX, spanY = item.cell.spanY,
                    folderId = null, position = 0, packageName = null, activityName = null,
                    profile = 0L, label = item.label, appWidgetId = item.appWidgetId,
                    providerFlat = item.providerFlat, shortcutId = null,
                ),
            )
            is ShortcutItem -> add(
                GridItemEntity(
                    id = item.id, kind = "shortcut", page = item.page,
                    x = item.cell.x, y = item.cell.y, spanX = item.cell.spanX, spanY = item.cell.spanY,
                    folderId = null, position = 0, packageName = item.packageName, activityName = null,
                    profile = item.profile, label = item.label, appWidgetId = null,
                    providerFlat = null, shortcutId = item.shortcutId,
                ),
            )
        }
    }
}

private fun AppItem.toEntity(folderId: String?, position: Int) = GridItemEntity(
    id = id, kind = "app", page = page,
    x = cell.x, y = cell.y, spanX = cell.spanX, spanY = cell.spanY,
    folderId = folderId, position = position,
    packageName = key.packageName, activityName = key.activityName, profile = key.profile,
    label = null, appWidgetId = null, providerFlat = null, shortcutId = null,
)

/** Rows back to the model. Malformed rows are dropped, never fatal — this draws the home screen. */
fun List<GridItemEntity>.toWorkspace(meta: WorkspaceMetaEntity?): WorkspaceState {
    val children = filter { it.folderId != null }.groupBy { it.folderId }
    val top = filter { it.folderId == null }
    val items = top.mapNotNull { row ->
        val cell = Cell(row.x, row.y, row.spanX, row.spanY)
        when (row.kind) {
            "app" -> row.appKey()?.let { AppItem(row.id, row.page, cell, it) }
            "folder" -> FolderItem(
                id = row.id, page = row.page, cell = cell, label = row.label.orEmpty(),
                items = children[row.id].orEmpty().sortedBy { it.position }.mapNotNull { child ->
                    child.appKey()?.let { AppItem(child.id, row.page, Cell(0, 0), it) }
                },
            )
            "widget" -> row.appWidgetId?.let { widgetId ->
                WidgetItem(row.id, row.page, cell, widgetId, row.providerFlat.orEmpty(), row.label.orEmpty())
            }
            "shortcut" -> row.shortcutId?.let { shortcutId ->
                ShortcutItem(
                    row.id, row.page, cell,
                    row.packageName.orEmpty(), shortcutId, row.label.orEmpty(), row.profile,
                )
            }
            else -> null
        }
    }
    val pages = meta?.pageCount ?: ((items.maxOfOrNull { it.page } ?: 0) + 1)
    return WorkspaceState(
        items = items,
        pageCount = pages.coerceAtLeast(1),
        seeded = meta?.seeded ?: items.isNotEmpty(),
    )
}

private fun GridItemEntity.appKey(): AppKey? {
    val pkg = packageName ?: return null
    val activity = activityName ?: return null
    return AppKey(pkg, activity, profile)
}
