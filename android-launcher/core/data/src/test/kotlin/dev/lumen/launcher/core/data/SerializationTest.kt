package dev.lumen.launcher.data

import dev.lumen.launcher.data.model.AppItem
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.model.Cell
import dev.lumen.launcher.data.model.FolderItem
import dev.lumen.launcher.data.model.PageState
import dev.lumen.launcher.data.model.ShortcutItem
import dev.lumen.launcher.data.model.WidgetItem
import dev.lumen.launcher.data.model.WorkspaceItem
import dev.lumen.launcher.data.model.WorkspaceState
import dev.lumen.launcher.data.prefs.AppOverride
import dev.lumen.launcher.data.prefs.GestureTrigger
import dev.lumen.launcher.data.prefs.GlassOverride
import dev.lumen.launcher.data.prefs.GlassSurfaceId
import dev.lumen.launcher.data.prefs.IconShape
import dev.lumen.launcher.data.prefs.LauncherAction
import dev.lumen.launcher.data.prefs.LauncherSettings
import dev.lumen.launcher.data.prefs.ThemeMode
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The settings tree and the workspace layout are the two things the launcher persists, and both use
 * shapes that quietly break JSON round-trips if they are wrong: a sealed hierarchy of workspace
 * items, and maps keyed by enums. These tests pin both down, and prove that a saved profile from an
 * older build still loads once new fields are added.
 */
class SerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ------------------------------------------------------------------ settings

    @Test
    fun `default settings round-trip unchanged`() {
        val defaults = LauncherSettings()
        val decoded = json.decodeFromString<LauncherSettings>(json.encodeToString(defaults))
        assertEquals(defaults, decoded)
    }

    @Test
    fun `a heavily customised profile round-trips including enum-keyed maps`() {
        val customised = LauncherSettings(
            activePreset = "cupertino",
            theme = LauncherSettings().theme.copy(
                mode = ThemeMode.DARK,
                dynamicColor = false,
                accentColor = 0xFFFF6699.toInt(),
                fontScale = 1.25f,
            ),
            glass = LauncherSettings().glass.copy(
                blurRadius = 42f,
                refraction = 21f,
                dispersion = 0.8f,
                surfaces = mapOf(
                    GlassSurfaceId.DOCK to GlassOverride(blurScale = 1.4f, tintAlphaScale = 0.5f),
                    GlassSurfaceId.DRAWER to GlassOverride(enabled = false),
                    GlassSurfaceId.FOLDER to GlassOverride(cornerRadiusOverride = 36f),
                ),
            ),
            grid = LauncherSettings().grid.copy(columns = 7, rows = 6, iconSize = 48f),
            icons = LauncherSettings().icons.copy(
                shape = IconShape.CLOVER,
                iconPack = "com.example.pack",
                themedIcons = true,
            ),
            gestures = LauncherSettings().gestures.copy(
                bindings = mapOf(
                    GestureTrigger.SWIPE_UP to LauncherAction.OPEN_DRAWER,
                    GestureTrigger.DOUBLE_TAP to LauncherAction.OPEN_APP,
                ),
                appTargets = mapOf(GestureTrigger.DOUBLE_TAP to "com.example/.Main@0"),
            ),
            apps = LauncherSettings().apps.copy(
                overrides = mapOf(
                    "com.example/.Main@0" to AppOverride(label = "Renamed", tint = 0x33FF0000, hidden = true),
                ),
                hidden = setOf("com.other/.Main@0"),
            ),
        )

        val decoded = json.decodeFromString<LauncherSettings>(json.encodeToString(customised))
        assertEquals(customised, decoded)
        assertEquals(3, decoded.glass.surfaces.size)
        assertEquals(1.4f, decoded.glass.surfaces.getValue(GlassSurfaceId.DOCK).blurScale)
        assertEquals(false, decoded.glass.surfaces.getValue(GlassSurfaceId.DRAWER).enabled)
        assertEquals(
            LauncherAction.OPEN_APP,
            decoded.gestures.bindings.getValue(GestureTrigger.DOUBLE_TAP),
        )
    }

    @Test
    fun `enum-keyed maps serialise as plain json objects`() {
        val encoded = json.encodeToString(
            LauncherSettings().copy(
                glass = LauncherSettings().glass.copy(
                    surfaces = mapOf(GlassSurfaceId.DOCK to GlassOverride(blurScale = 2f)),
                ),
            ),
        )
        assertTrue("expected the enum name as the JSON key", encoded.contains("\"DOCK\""))
    }

    @Test
    fun `a settings file written by an older build still loads`() {
        // Only two fields present: everything else must fall back to its default.
        val sparse = """{"schemaVersion":1,"grid":{"columns":6}}"""
        val decoded = json.decodeFromString<LauncherSettings>(sparse)

        assertEquals(6, decoded.grid.columns)
        assertEquals(LauncherSettings().grid.rows, decoded.grid.rows)
        assertEquals(LauncherSettings().glass, decoded.glass)
        assertEquals(LauncherSettings().dock, decoded.dock)
    }

    @Test
    fun `unknown fields from a newer build are ignored rather than fatal`() {
        val futuristic = """{"schemaVersion":99,"grid":{"columns":4,"someFutureKnob":true},"newGroup":{"x":1}}"""
        val decoded = json.decodeFromString<LauncherSettings>(futuristic)
        assertEquals(4, decoded.grid.columns)
        assertEquals(99, decoded.schemaVersion)
    }

    @Test
    fun `AppCustomizations resolves labels and hidden state from either source`() {
        val apps = LauncherSettings().apps.copy(
            overrides = mapOf("a/.M@0" to AppOverride(label = "Custom", hidden = true)),
            hidden = setOf("b/.M@0"),
        )
        assertEquals("Custom", apps.labelFor("a/.M@0", "Original"))
        assertEquals("Original", apps.labelFor("z/.M@0", "Original"))
        assertTrue(apps.isHidden("a/.M@0"))
        assertTrue(apps.isHidden("b/.M@0"))
        assertTrue(!apps.isHidden("c/.M@0"))
    }

    // ------------------------------------------------------------------ workspace

    @Test
    fun `every workspace item type round-trips through the sealed hierarchy`() {
        val state = WorkspaceState(
            pages = listOf(
                PageState(
                    id = "p0",
                    items = listOf(
                        AppItem(id = "a", cell = Cell(1, 2), key = AppKey("com.a", ".Main", 0L)),
                        FolderItem(
                            id = "f",
                            cell = Cell(3, 0),
                            label = "Work",
                            items = listOf(AppItem("c1", Cell(0, 0), AppKey("com.c", ".M", 10L))),
                            tint = 0xFF00FF00.toInt(),
                            autoLabelled = false,
                        ),
                        WidgetItem(
                            id = "w",
                            cell = Cell(0, 3, 4, 2),
                            appWidgetId = 42,
                            providerFlat = "com.w/.Provider",
                            label = "Clock",
                            profile = 0L,
                        ),
                        ShortcutItem(
                            id = "s",
                            cell = Cell(2, 4),
                            packageName = "com.s",
                            shortcutId = "compose",
                            label = "New message",
                            profile = 0L,
                        ),
                    ),
                ),
                PageState("p1"),
            ),
            dock = listOf(AppItem("d", Cell(0, 0), AppKey("com.d", ".Main", 0L))),
            initialized = true,
        )

        val decoded = json.decodeFromString<WorkspaceState>(json.encodeToString(state))
        assertEquals(state, decoded)

        val items: List<WorkspaceItem> = decoded.pages.first().items
        assertTrue(items[0] is AppItem)
        assertTrue(items[1] is FolderItem)
        assertTrue(items[2] is WidgetItem)
        assertTrue(items[3] is ShortcutItem)
        assertEquals(4, (items[2] as WidgetItem).cell.spanX)
        assertEquals(10L, (items[1] as FolderItem).items.first().key.profile)
    }

    @Test
    fun `an empty workspace round-trips`() {
        val empty = WorkspaceState()
        assertEquals(empty, json.decodeFromString<WorkspaceState>(json.encodeToString(empty)))
    }

    @Test
    fun `withCell preserves the concrete item type and its payload`() {
        val widget = WidgetItem(id = "w", appWidgetId = 3, providerFlat = "p/.W")
        val moved = widget.withCell(Cell(2, 2, 3, 1))
        assertTrue(moved is WidgetItem)
        assertEquals(3, (moved as WidgetItem).appWidgetId)
        assertEquals(Cell(2, 2, 3, 1), moved.cell)
    }

    // ------------------------------------------------------------------ keys

    @Test
    fun `AppKey survives a flat round-trip including work profiles`() {
        listOf(
            AppKey("com.example.app", "com.example.app.MainActivity", 0L),
            AppKey("com.example.app", "com.example.app.MainActivity", 11L),
            AppKey("a.b.c", ".Alias", 4294967296L),
        ).forEach { original ->
            val parsed = AppKey.parse(original.flat)
            assertNotNull("failed to parse ${original.flat}", parsed)
            assertEquals(original, parsed)
        }
    }

    @Test
    fun `AppKey rejects malformed flat keys instead of throwing`() {
        listOf("", "nonsense", "com.example.app", "com.example/.Main", "com.example/.Main@x", "@0")
            .forEach { assertEquals("should reject '$it'", null, AppKey.parse(it)) }
    }
}
