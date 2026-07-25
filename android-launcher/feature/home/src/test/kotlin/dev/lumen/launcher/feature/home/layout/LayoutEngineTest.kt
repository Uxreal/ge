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
import dev.lumen.launcher.data.model.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [WorkspaceOps] is deliberately free of Android and Compose types, so the behaviour behind every
 * drag, drop and grid change can be verified as plain data transformation.
 */
class WorkspaceOpsTest {

    private fun key(name: String, profile: Long = 0L) = AppKey("com.example.$name", ".Main", profile)

    private fun app(id: String, x: Int, y: Int, name: String = id) =
        AppItem(id = id, cell = Cell(x, y), key = key(name))

    private fun state(vararg items: AppItem) = WorkspaceState(
        pages = listOf(PageState("p0", items.toList())),
        dock = emptyList(),
        initialized = true,
    )

    // ------------------------------------------------------------------ occupancy

    @Test
    fun `cells overlap only when they intersect`() {
        assertTrue(Cell(0, 0, 2, 2).overlaps(Cell(1, 1)))
        assertFalse(Cell(0, 0, 2, 2).overlaps(Cell(2, 0)))
        assertFalse(Cell(0, 0).overlaps(Cell(0, 1)))
    }

    @Test
    fun `coerceInto clamps position and span to the grid`() {
        val clamped = Cell(9, 9, 4, 4).coerceInto(columns = 5, rows = 5)
        assertEquals(Cell(1, 1, 4, 4), clamped)

        val oversized = Cell(0, 0, 9, 9).coerceInto(columns = 4, rows = 3)
        assertEquals(Cell(0, 0, 4, 3), oversized)
    }

    @Test
    fun `firstFreeCell skips occupied cells and respects spans`() {
        val items = listOf(app("a", 0, 0), app("b", 1, 0))
        assertEquals(Cell(2, 0), WorkspaceOps.firstFreeCell(items, columns = 4, rows = 2))

        // A 2x2 widget cannot fit on row 0 next to the two icons on a 4x2 grid.
        assertEquals(
            Cell(2, 0, 2, 2),
            WorkspaceOps.firstFreeCell(items, columns = 4, rows = 2, spanX = 2, spanY = 2),
        )
    }

    @Test
    fun `firstFreeCell returns null when the page is full`() {
        val full = (0 until 4).map { app("a$it", it, 0) }
        assertNull(WorkspaceOps.firstFreeCell(full, columns = 4, rows = 1))
    }

    @Test
    fun `nearestFreeCell prefers the requested cell then searches outward`() {
        val items = listOf(app("a", 2, 2))
        assertEquals(Cell(1, 1), WorkspaceOps.nearestFreeCell(items, Cell(1, 1), 5, 5))

        // Dropping onto an occupied cell lands adjacent, not across the page.
        val landed = WorkspaceOps.nearestFreeCell(items, Cell(2, 2), 5, 5)
        assertNotNull(landed)
        assertTrue((landed!!.x - 2) * (landed.x - 2) + (landed.y - 2) * (landed.y - 2) <= 2)
    }

    @Test
    fun `nearestFreeCell ignores the item being moved`() {
        val items = listOf(app("a", 2, 2))
        assertEquals(
            Cell(2, 2),
            WorkspaceOps.nearestFreeCell(items, Cell(2, 2), 5, 5, ignoreId = "a"),
        )
    }

    @Test
    fun `canPlace rejects cells that leave the grid`() {
        assertFalse(WorkspaceOps.canPlace(emptyList(), Cell(3, 0, 2, 1), columns = 4, rows = 4))
        assertTrue(WorkspaceOps.canPlace(emptyList(), Cell(2, 0, 2, 1), columns = 4, rows = 4))
        assertFalse(WorkspaceOps.canPlace(emptyList(), Cell(-1, 0), columns = 4, rows = 4))
    }

    // ------------------------------------------------------------------ moving

    @Test
    fun `move relocates an item into another container`() {
        val start = state(app("a", 0, 0))
        val moved = WorkspaceOps.move(start, "a", ItemContainer.Dock, Cell(1, 0))

        assertTrue(moved.pages.first().items.isEmpty())
        assertEquals(1, moved.dock.size)
        assertEquals(Cell(1, 0), moved.dock.first().cell)
    }

    @Test
    fun `locate finds items on pages, in the dock and inside folders`() {
        val folder = FolderItem(
            id = "f",
            cell = Cell(1, 0),
            items = listOf(app("child", 0, 0)),
        )
        val start = WorkspaceState(
            pages = listOf(PageState("p0", listOf(app("a", 0, 0), folder))),
            dock = listOf(app("d", 0, 0)),
        )

        assertEquals(ItemContainer.Page("p0"), WorkspaceOps.locate(start, "a")?.container)
        assertEquals(ItemContainer.Dock, WorkspaceOps.locate(start, "d")?.container)
        assertEquals(ItemContainer.Folder("f"), WorkspaceOps.locate(start, "child")?.container)
        assertNull(WorkspaceOps.locate(start, "missing"))
    }

    @Test
    fun `reorder renumbers a linear container`() {
        val start = WorkspaceState(
            pages = listOf(PageState("p0")),
            dock = listOf(app("a", 0, 0), app("b", 1, 0), app("c", 2, 0)),
        )
        val reordered = WorkspaceOps.reorder(start, ItemContainer.Dock, fromIndex = 2, toIndex = 0)

        assertEquals(listOf("c", "a", "b"), reordered.dock.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.dock.map { it.cell.x })
    }

    // ------------------------------------------------------------------ folders

    @Test
    fun `combining two apps creates a folder in place`() {
        val start = state(app("a", 0, 0), app("b", 1, 0))
        val combined = WorkspaceOps.combine(start, targetId = "a", sourceId = "b") { "Folder" }

        val items = combined.pages.first().items
        assertEquals(1, items.size)
        val folder = items.first() as FolderItem
        assertEquals(2, folder.items.size)
        assertEquals(Cell(0, 0), folder.cell)
    }

    @Test
    fun `combining into an existing folder grows it without duplicating`() {
        val folder = FolderItem(id = "f", cell = Cell(0, 0), items = listOf(app("a", 0, 0, "a")))
        val start = WorkspaceState(pages = listOf(PageState("p0", listOf(folder, app("b", 1, 0, "b")))))

        val grown = WorkspaceOps.combine(start, targetId = "f", sourceId = "b") { "Folder" }
        val result = grown.pages.first().items.filterIsInstance<FolderItem>().single()
        assertEquals(2, result.items.size)

        // Dropping the same app again must not duplicate it.
        val again = WorkspaceOps.combine(
            WorkspaceOps.add(grown, ItemContainer.Page("p0"), app("b2", 2, 0, "b")),
            targetId = "f",
            sourceId = "b2",
        ) { "Folder" }
        assertEquals(2, again.pages.first().items.filterIsInstance<FolderItem>().single().items.size)
    }

    @Test
    fun `combining an item with itself is a no-op`() {
        val start = state(app("a", 0, 0))
        assertEquals(start, WorkspaceOps.combine(start, targetId = "a", sourceId = "a"))
    }

    @Test
    fun `extracting the second-to-last child dissolves the folder`() {
        val folder = FolderItem(
            id = "f",
            cell = Cell(0, 0),
            items = listOf(app("x", 0, 0, "x"), app("y", 1, 0, "y")),
        )
        val start = WorkspaceState(pages = listOf(PageState("p0", listOf(folder))))

        val extracted = WorkspaceOps.extractFromFolder(
            state = start,
            folderId = "f",
            itemId = "x",
            target = ItemContainer.Page("p0"),
            cell = Cell(2, 0),
        )

        val items = extracted.pages.first().items
        assertEquals(2, items.size)
        assertTrue("the one-item folder should have dissolved", items.none { it is FolderItem })
    }

    @Test
    fun `dissolveThinFolders drops empty folders and unwraps single-item ones`() {
        val start = WorkspaceState(
            pages = listOf(
                PageState(
                    "p0",
                    listOf(
                        FolderItem(id = "empty", cell = Cell(0, 0)),
                        FolderItem(id = "one", cell = Cell(1, 0), items = listOf(app("solo", 0, 0))),
                        FolderItem(
                            id = "two",
                            cell = Cell(2, 0),
                            items = listOf(app("p", 0, 0, "p"), app("q", 1, 0, "q")),
                        ),
                    ),
                ),
            ),
        )

        val items = WorkspaceOps.dissolveThinFolders(start).pages.first().items
        assertEquals(2, items.size)
        assertTrue(items.any { it is AppItem && it.id == "solo" && it.cell == Cell(1, 0) })
        assertTrue(items.any { it is FolderItem && it.id == "two" })
    }

    @Test
    fun `autoLabel names a folder after the dominant category`() {
        val items = listOf(app("a", 0, 0, "a"), app("b", 1, 0, "b"), app("c", 2, 0, "c"))
        val categories = mapOf(
            key("a") to AppCategory.GAMES,
            key("b") to AppCategory.GAMES,
            key("c") to AppCategory.FINANCE,
        )
        assertEquals("Games", WorkspaceOps.autoLabel(items) { categories.getValue(it) })
        assertEquals("Folder", WorkspaceOps.autoLabel(emptyList()) { AppCategory.OTHER })
    }

    // ------------------------------------------------------------------ removal

    @Test
    fun `remove strips an item from pages, dock and folder contents`() {
        val folder = FolderItem(
            id = "f",
            cell = Cell(1, 0),
            items = listOf(app("child", 0, 0), app("child2", 1, 0, "other")),
        )
        val start = WorkspaceState(
            pages = listOf(PageState("p0", listOf(app("a", 0, 0), folder))),
            dock = listOf(app("d", 0, 0)),
        )

        assertNull(WorkspaceOps.locate(WorkspaceOps.remove(start, "child"), "child"))
        assertNull(WorkspaceOps.locate(WorkspaceOps.remove(start, "d"), "d"))
        assertNull(WorkspaceOps.locate(WorkspaceOps.remove(start, "f"), "f"))
    }

    @Test
    fun `removeAppEverywhere clears every instance of an uninstalled app`() {
        val target = key("gone")
        val start = WorkspaceState(
            pages = listOf(
                PageState(
                    "p0",
                    listOf(
                        AppItem("a", Cell(0, 0), target),
                        FolderItem(
                            "f",
                            Cell(1, 0),
                            items = listOf(
                                AppItem("b", Cell(0, 0), target),
                                AppItem("c", Cell(1, 0), key("stays")),
                            ),
                        ),
                    ),
                ),
            ),
            dock = listOf(AppItem("d", Cell(0, 0), target)),
        )

        val cleaned = WorkspaceOps.removeAppEverywhere(start, target)
        assertTrue(WorkspaceOps.allAppItems(cleaned).none { it.key == target })
        assertEquals(1, WorkspaceOps.allAppItems(cleaned).size)
    }

    // ------------------------------------------------------------------ pages

    @Test
    fun `pages can be added, moved and removed but never all removed`() {
        var s = WorkspaceState(pages = listOf(PageState("p0")))
        s = WorkspaceOps.addPage(s)
        assertEquals(2, s.pageCount)

        s = WorkspaceOps.add(s, ItemContainer.Page(s.pages[1].id), app("a", 0, 0))
        val secondId = s.pages[1].id
        s = WorkspaceOps.movePage(s, from = 1, to = 0)
        assertEquals(secondId, s.pages.first().id)

        s = WorkspaceOps.removePage(s, secondId)
        assertEquals(1, s.pageCount)

        // The last page is never removable - the user must always have a home screen.
        val onlyPage = s.pages.first().id
        assertEquals(1, WorkspaceOps.removePage(s, onlyPage).pageCount)
    }

    @Test
    fun `trimTrailingEmptyPages keeps the first page and drops trailing blanks`() {
        val s = WorkspaceState(
            pages = listOf(
                PageState("p0", listOf(app("a", 0, 0))),
                PageState("p1"),
                PageState("p2"),
            ),
        )
        assertEquals(1, WorkspaceOps.trimTrailingEmptyPages(s).pageCount)
        assertEquals(1, WorkspaceOps.trimTrailingEmptyPages(WorkspaceState(pages = listOf(PageState("p0")))).pageCount)
    }

    // ------------------------------------------------------------------ reflow

    @Test
    fun `reflow spills overflow onto new pages when the grid shrinks`() {
        val items = (0 until 9).map { app("a$it", it % 3, it / 3) }
        val start = WorkspaceState(pages = listOf(PageState("p0", items)), initialized = true)

        val reflowed = WorkspaceOps.reflow(start, columns = 2, rows = 2, dockColumns = 4)

        // Nine icons cannot fit in a single 2x2 page, so pages are added rather than icons lost.
        assertTrue(reflowed.pageCount >= 3)
        assertEquals(9, reflowed.pages.sumOf { it.items.size })
        reflowed.pages.forEach { page ->
            page.items.forEach { item ->
                assertTrue(item.cell.right <= 2 && item.cell.bottom <= 2)
            }
        }
    }

    @Test
    fun `reflow clamps the dock to its column count`() {
        val start = WorkspaceState(
            pages = listOf(PageState("p0")),
            dock = (0 until 6).map { app("d$it", it, 0) },
        )
        val reflowed = WorkspaceOps.reflow(start, columns = 5, rows = 5, dockColumns = 4)
        assertEquals(4, reflowed.dock.size)
        assertEquals(listOf(0, 1, 2, 3), reflowed.dock.map { it.cell.x })
    }

    @Test
    fun `reflow keeps widget spans intact`() {
        val widget = WidgetItem(id = "w", cell = Cell(0, 0, 2, 2), appWidgetId = 7, providerFlat = "p/w")
        val start = WorkspaceState(pages = listOf(PageState("p0", listOf(widget))))

        val reflowed = WorkspaceOps.reflow(start, columns = 5, rows = 5, dockColumns = 4)
        val result = reflowed.pages.flatMap { it.items }.filterIsInstance<WidgetItem>().single()
        assertEquals(2, result.cell.spanX)
        assertEquals(2, result.cell.spanY)
        assertEquals(7, result.appWidgetId)
    }

    // ------------------------------------------------------------------ bootstrap

    private fun info(pkg: String, label: String, system: Boolean = false) = AppInfo(
        key = AppKey(pkg, ".Main"),
        label = label,
        searchTokens = label.lowercase(),
        isSystemApp = system,
        isWorkProfile = false,
        firstInstallTime = 0L,
        lastUpdateTime = 0L,
        category = AppCategory.OTHER,
    )

    @Test
    fun `defaultLayout fills the dock with known essentials and pages the rest`() {
        val apps = listOf(
            info("com.google.android.dialer", "Phone"),
            info("com.google.android.apps.messaging", "Messages"),
            info("com.android.chrome", "Chrome"),
            info("com.google.android.GoogleCamera", "Camera"),
        ) + (0 until 12).map { info("com.example.app$it", "App $it") }

        val layout = WorkspaceOps.defaultLayout(apps, columns = 4, rows = 2, dockColumns = 4)

        assertTrue(layout.initialized)
        assertEquals(4, layout.dock.size)
        assertEquals(
            listOf(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.android.chrome",
                "com.google.android.GoogleCamera",
            ),
            layout.dock.filterIsInstance<AppItem>().map { it.key.packageName },
        )
        // Every remaining app is placed exactly once, across as many 4x2 pages as needed.
        assertEquals(12, layout.pages.sumOf { it.items.size })
        assertEquals(12, WorkspaceOps.allAppItems(layout).size - layout.dock.size)
        layout.pages.forEach { page ->
            assertTrue(page.items.size <= 8)
            assertEquals(page.items.size, page.items.map { it.cell }.distinct().size)
        }
    }

    @Test
    fun `defaultLayout on an empty device still yields one page`() {
        val layout = WorkspaceOps.defaultLayout(emptyList(), columns = 5, rows = 5, dockColumns = 4)
        assertEquals(1, layout.pageCount)
        assertTrue(layout.dock.isEmpty())
    }

    @Test
    fun `autoPlace ignores apps already on the workspace and adds pages when full`() {
        val existing = key("known")
        val start = WorkspaceState(
            pages = listOf(PageState("p0", listOf(AppItem("a", Cell(0, 0), existing)))),
            initialized = true,
        )

        val unchanged = WorkspaceOps.autoPlace(start, listOf(existing), columns = 1, rows = 1)
        assertEquals(1, WorkspaceOps.allAppItems(unchanged).size)

        val grown = WorkspaceOps.autoPlace(start, listOf(key("fresh")), columns = 1, rows = 1)
        assertEquals(2, grown.pageCount)
        assertEquals(2, WorkspaceOps.allAppItems(grown).size)
    }

    @Test
    fun `newId never collides`() {
        val ids = (0 until 500).map { WorkspaceOps.newId("app") }
        assertEquals(ids.size, ids.distinct().size)
    }
}
