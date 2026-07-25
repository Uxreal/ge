package dev.lumen.launcher.feature.home.layout

import dev.lumen.launcher.core.data.model.AppCategory
import dev.lumen.launcher.core.data.model.AppInfo
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.data.model.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §5's behaviour as data transformation: bottom gravity, PACKED reflow around pinned widgets,
 * FREEFORM never moving what the user placed, folders, and lossless model switches.
 */
class LayoutEngineTest {

    private val cols = 4
    private val rows = 5

    private fun key(name: String) = AppKey("com.example.$name", ".Main")

    private fun app(id: String, page: Int, cell: Cell) = AppItem(id, page, cell, key(id))

    private fun info(name: String) = AppInfo(
        key = key(name), label = name, searchTokens = name.lowercase(),
        isSystemApp = false, isWorkProfile = false,
        firstInstallTime = 0L, lastUpdateTime = 0L, category = AppCategory.OTHER,
    )

    // ------------------------------------------------------------------ flow order (§1 gravity)

    @Test
    fun `flow index counts from the bottom-left upward`() {
        // Bottom row is indices 0..3, the row above 4..7, and so on.
        assertEquals(0, LayoutEngine.flowIndex(Cell(0, rows - 1), cols, rows))
        assertEquals(3, LayoutEngine.flowIndex(Cell(3, rows - 1), cols, rows))
        assertEquals(4, LayoutEngine.flowIndex(Cell(0, rows - 2), cols, rows))
        assertEquals(cols * rows - 1, LayoutEngine.flowIndex(Cell(cols - 1, 0), cols, rows))
    }

    @Test
    fun `cellAtFlowIndex inverts flowIndex`() {
        for (index in 0 until cols * rows) {
            val cell = LayoutEngine.cellAtFlowIndex(index, cols, rows)
            assertEquals(index, LayoutEngine.flowIndex(cell, cols, rows))
        }
    }

    @Test
    fun `seeding PACKED fills the bottom row first`() {
        val seeded = LayoutEngine.seedPacked(listOf(info("a"), info("b"), info("c")), cols, rows)
        val cells = seeded.items.map { it.cell }
        // Three apps: bottom-left, then rightward along the bottom row. Thumb territory.
        assertTrue(cells.all { it.y == rows - 1 })
        assertEquals(listOf(0, 1, 2), cells.map { it.x }.sorted())
        assertTrue(seeded.seeded)
        assertEquals(1, seeded.pageCount)
    }

    @Test
    fun `seeding overflows onto more pages`() {
        val many = (0 until cols * rows + 3).map { info("app%02d".format(it)) }
        val seeded = LayoutEngine.seedPacked(many, cols, rows)
        assertEquals(2, seeded.pageCount)
        assertEquals(3, seeded.itemsOn(1).size)
    }

    // ------------------------------------------------------------------ PACKED reflow (§5)

    @Test
    fun `removal in PACKED collapses the gap toward gravity`() {
        val state = LayoutEngine.seedPacked(listOf(info("a"), info("b"), info("c")), cols, rows)
        val ids = state.items.sortedBy { LayoutEngine.flowIndex(it.cell, cols, rows) }.map { it.id }

        val next = LayoutEngine.remove(state, ids[0], HomeModel.PACKED, cols, rows)

        // The two survivors slid down into flow indices 0 and 1 — no hole.
        val flows = next.items.map { LayoutEngine.flowIndex(it.cell, cols, rows) }.sorted()
        assertEquals(listOf(0, 1), flows)
    }

    @Test
    fun `removal in PACKED pulls items back across a page boundary`() {
        val many = (0 until cols * rows + 2).map { info("app%02d".format(it)) }
        val state = LayoutEngine.seedPacked(many, cols, rows)
        assertEquals(2, state.itemsOn(1).size)

        val victim = state.itemsOn(0).minByOrNull { LayoutEngine.flowIndex(it.cell, cols, rows) }!!
        val next = LayoutEngine.remove(state, victim.id, HomeModel.PACKED, cols, rows)

        // One item cascades back from page 2; page 1 is full again.
        assertEquals(cols * rows, next.itemsOn(0).size)
        assertEquals(1, next.itemsOn(1).size)
    }

    @Test
    fun `compact flows around a pinned widget without moving it`() {
        val widget = WidgetItem("w", 0, Cell(0, 0, 4, 2), appWidgetId = 7, providerFlat = "p/w")
        val apps = (0 until 5).map { app("a$it", 0, LayoutEngine.cellAtFlowIndex(it, cols, rows)) }
        val state = WorkspaceState(items = apps + widget, pageCount = 1)

        val next = LayoutEngine.compact(state, cols, rows)

        val movedWidget = next.items.filterIsInstance<WidgetItem>().single()
        assertEquals(Cell(0, 0, 4, 2), movedWidget.cell)
        // No flowing item may overlap the widget's two rows.
        next.items.filterIsInstance<AppItem>().forEach { assertTrue(it.cell.y >= 2) }
    }

    @Test
    fun `FREEFORM removal leaves every other item exactly where it was`() {
        val a = app("a", 0, Cell(1, 1))
        val b = app("b", 0, Cell(3, 4))
        val state = WorkspaceState(items = listOf(a, b), pageCount = 1)

        val next = LayoutEngine.remove(state, "a", HomeModel.FREEFORM, cols, rows)

        assertNull(next.find("a"))
        assertEquals(Cell(3, 4), next.find("b")!!.cell)
    }

    // ------------------------------------------------------------------ moving

    @Test
    fun `PACKED move is a reorder - dropping at flow position zero shifts the rest up`() {
        val state = LayoutEngine.seedPacked(listOf(info("a"), info("b"), info("c")), cols, rows)
        val byFlow = state.items.sortedBy { LayoutEngine.flowIndex(it.cell, cols, rows) }
        val last = byFlow.last()

        val next = LayoutEngine.move(
            state, last.id, targetPage = 0,
            targetCell = LayoutEngine.cellAtFlowIndex(0, cols, rows),
            model = HomeModel.PACKED, columns = cols, rows = rows,
        )

        val order = next.items.sortedBy { LayoutEngine.flowIndex(it.cell, cols, rows) }.map { it.id }
        assertEquals(last.id, order.first())
        assertEquals(3, next.items.size)
    }

    @Test
    fun `FREEFORM move lands on the requested cell when free and the nearest when not`() {
        val a = app("a", 0, Cell(0, 4))
        val b = app("b", 0, Cell(2, 2))
        val state = WorkspaceState(items = listOf(a, b), pageCount = 1)

        val free = LayoutEngine.move(state, "a", 0, Cell(3, 0), HomeModel.FREEFORM, cols, rows)
        assertEquals(Cell(3, 0), free.find("a")!!.cell)

        val onto = LayoutEngine.move(free, "a", 0, Cell(2, 2), HomeModel.FREEFORM, cols, rows)
        val landed = onto.find("a")!!.cell
        assertTrue(landed != Cell(2, 2))
        assertTrue(abs(landed.x - 2) <= 1 && abs(landed.y - 2) <= 1)
    }

    private fun abs(v: Int) = if (v < 0) -v else v

    @Test
    fun `moving to a later page grows the page count`() {
        val state = WorkspaceState(items = listOf(app("a", 0, Cell(0, 4))), pageCount = 1)
        val next = LayoutEngine.move(state, "a", 1, Cell(0, 4), HomeModel.FREEFORM, cols, rows)
        assertEquals(2, next.pageCount)
        assertEquals(1, next.find("a")!!.page)
    }

    @Test
    fun `swap exchanges two one-by-one items and refuses widgets`() {
        val a = app("a", 0, Cell(0, 4))
        val b = app("b", 0, Cell(3, 3))
        val w = WidgetItem("w", 0, Cell(0, 0, 2, 2), 1, "p/w")
        val state = WorkspaceState(items = listOf(a, b, w), pageCount = 1)

        val swapped = LayoutEngine.swap(state, "a", "b")
        assertEquals(Cell(3, 3), swapped.find("a")!!.cell)
        assertEquals(Cell(0, 4), swapped.find("b")!!.cell)

        assertEquals(swapped, LayoutEngine.swap(swapped, "a", "w"))
    }

    // ------------------------------------------------------------------ folders (§6)

    @Test
    fun `dropping one app on another makes a folder in the target's cell`() {
        val a = app("a", 0, Cell(0, 4))
        val b = app("b", 0, Cell(1, 4))
        val state = WorkspaceState(items = listOf(a, b), pageCount = 1)

        val next = LayoutEngine.combine(state, "a", "b", HomeModel.FREEFORM, cols, rows)

        val folder = next.items.filterIsInstance<FolderItem>().single()
        assertEquals(Cell(0, 4), folder.cell)
        assertEquals(2, folder.items.size)
        assertNull(next.items.firstOrNull { it.id == "b" })
    }

    @Test
    fun `dropping on a folder grows it without duplicates`() {
        val a = app("a", 0, Cell(0, 4))
        val b = app("b", 0, Cell(1, 4))
        val c = app("c", 0, Cell(2, 4))
        var state = WorkspaceState(items = listOf(a, b, c), pageCount = 1)
        state = LayoutEngine.combine(state, "a", "b", HomeModel.FREEFORM, cols, rows)
        val folderId = state.items.filterIsInstance<FolderItem>().single().id

        state = LayoutEngine.combine(state, folderId, "c", HomeModel.FREEFORM, cols, rows)
        assertEquals(3, state.items.filterIsInstance<FolderItem>().single().items.size)

        // Re-adding an app with the same key must not duplicate it.
        val clone = app("c2", 0, Cell(3, 4)).copy(key = key("c"))
        state = state.copy(items = state.items + clone)
        state = LayoutEngine.combine(state, folderId, "c2", HomeModel.FREEFORM, cols, rows)
        assertEquals(3, state.items.filterIsInstance<FolderItem>().single().items.size)
    }

    @Test
    fun `a folder left with one child dissolves back to an icon`() {
        val folder = FolderItem(
            "f", 0, Cell(1, 4), "Pair",
            items = listOf(app("x", 0, Cell(0, 0)), app("y", 0, Cell(0, 0))),
        )
        val state = WorkspaceState(items = listOf(folder), pageCount = 1)

        val next = LayoutEngine.move(state, "x", 0, Cell(3, 4), HomeModel.FREEFORM, cols, rows)

        assertTrue(next.items.none { it is FolderItem })
        assertNotNull(next.find("x"))
        assertNotNull(next.find("y"))
        assertEquals(Cell(1, 4), next.find("y")!!.cell)
    }

    @Test
    fun `rename sticks`() {
        val folder = FolderItem(
            "f", 0, Cell(0, 4), "",
            items = listOf(app("x", 0, Cell(0, 0)), app("y", 0, Cell(0, 0))),
        )
        val state = WorkspaceState(items = listOf(folder), pageCount = 1)
        val next = LayoutEngine.renameFolder(state, "f", "Work")
        assertEquals("Work", (next.find("f") as FolderItem).label)
    }

    // ------------------------------------------------------------------ model switches (§5)

    @Test
    fun `switching to PACKED appends missing apps without moving placed ones`() {
        val placed = app("placed", 0, Cell(2, 2))
        val state = WorkspaceState(items = listOf(placed), pageCount = 1)
        val all = listOf(info("placed"), info("alpha"), info("zeta"))

        val next = LayoutEngine.ensureAllApps(state, all, cols, rows)

        assertEquals(3, next.allApps().size)
        assertTrue(next.containsApp(key("alpha")))
        assertTrue(next.containsApp(key("zeta")))
    }

    @Test
    fun `appendApp lands at the end of the flow and overflows to a new page`() {
        var state = LayoutEngine.seedPacked((0 until cols * rows).map { info("app%02d".format(it)) }, cols, rows)
        assertEquals(1, state.pageCount)

        state = LayoutEngine.appendApp(state, key("fresh"), cols, rows)
        assertEquals(2, state.pageCount)
        assertEquals(1, state.itemsOn(1).size)

        // Appending an app already on the grid is a no-op.
        assertEquals(state, LayoutEngine.appendApp(state, key("fresh"), cols, rows))
    }

    // ------------------------------------------------------------------ misc

    @Test
    fun `trimTrailingEmptyPages keeps at least one page`() {
        val state = WorkspaceState(items = emptyList(), pageCount = 3)
        assertEquals(1, LayoutEngine.trimTrailingEmptyPages(state).pageCount)

        val withItem = WorkspaceState(items = listOf(app("a", 1, Cell(0, 4))), pageCount = 4)
        assertEquals(2, LayoutEngine.trimTrailingEmptyPages(withItem).pageCount)
    }

    @Test
    fun `canPlace rejects out-of-grid and overlapping cells`() {
        val state = WorkspaceState(items = listOf(app("a", 0, Cell(1, 1))), pageCount = 1)
        assertFalse(LayoutEngine.canPlace(state, 0, Cell(-1, 0), cols, rows))
        assertFalse(LayoutEngine.canPlace(state, 0, Cell(3, 0, 2, 1), cols, rows))
        assertFalse(LayoutEngine.canPlace(state, 0, Cell(1, 1), cols, rows))
        assertTrue(LayoutEngine.canPlace(state, 0, Cell(1, 1), cols, rows, ignoreId = "a"))
    }

    @Test
    fun `ids never collide`() {
        val ids = (0 until 500).map { LayoutEngine.newId("app") }
        assertEquals(ids.size, ids.distinct().size)
    }
}
