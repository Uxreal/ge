package dev.lumen.launcher.data.prefs

import kotlinx.serialization.Serializable

/**
 * The complete, serializable state of every user-facing knob in Lumen.
 *
 * This type is the single source of truth: persistence (JSON in DataStore), the generated settings
 * UI, presets, and backup/restore are all derived from it. Every field has a default, so adding a
 * knob is backward compatible with existing saved profiles.
 */
@Serializable
data class LauncherSettings(
    val schemaVersion: Int = SCHEMA_VERSION,
    val activePreset: String? = null,
    val theme: ThemeSettings = ThemeSettings(),
    val glass: GlassSettings = GlassSettings(),
    val wallpaper: WallpaperSettings = WallpaperSettings(),
    val home: HomeSettings = HomeSettings(),
    val grid: GridSettings = GridSettings(),
    val icons: IconSettings = IconSettings(),
    val labels: LabelSettings = LabelSettings(),
    val dock: DockSettings = DockSettings(),
    val drawer: DrawerSettings = DrawerSettings(),
    val folders: FolderSettings = FolderSettings(),
    val search: SearchSettings = SearchSettings(),
    val motion: MotionSettings = MotionSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val widgets: WidgetSettings = WidgetSettings(),
    val badges: BadgeSettings = BadgeSettings(),
    val apps: AppCustomizations = AppCustomizations(),
    val advanced: AdvancedSettings = AdvancedSettings(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

// --------------------------------------------------------------------------- theme

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentSource { WALLPAPER, MANUAL, MATERIAL_YOU }

enum class TypographyStyle { SYSTEM, ROUNDED, GEOMETRIC, SERIF, MONO }

enum class CornerStyle {
    /** Apple's continuous curvature — corners blend into the edge. */
    SQUIRCLE,

    /** Material's circular corner arc. */
    ROUNDED,
    SHARP,
}

@Serializable
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You: derive the palette from the system wallpaper (Android 12+). */
    val dynamicColor: Boolean = true,
    val accentSource: AccentSource = AccentSource.MATERIAL_YOU,
    val accentColor: Int = 0xFF7AA2FF.toInt(),
    val secondaryColor: Int = 0xFFB79CFF.toInt(),
    val contrast: Float = 0f,
    val amoledBlack: Boolean = false,
    val typography: TypographyStyle = TypographyStyle.ROUNDED,
    val fontScale: Float = 1f,
    val fontWeightBias: Int = 0,
    val cornerStyle: CornerStyle = CornerStyle.SQUIRCLE,
    val cornerScale: Float = 1f,
    val surfaceOpacity: Float = 0.62f,
    val elevationTint: Float = 0.5f,
    val lightSystemBarIcons: Boolean = false,
    val monochromeSurfaces: Boolean = false,
)

// --------------------------------------------------------------------------- liquid glass

enum class GlassQuality {
    /** Picks a tier from device capability at runtime. */
    AUTO,
    HIGH,
    BALANCED,
    POWER_SAVER,
    OFF,
}

enum class GlassSurfaceId {
    DOCK, DRAWER, FOLDER, SEARCH, CONTEXT_MENU, PAGE_INDICATOR, SETTINGS,
    WIDGET_TILE, ICON_TILE, GLANCE_PILL, DIALOG, TOOLBAR,
}

/** Per-surface multipliers layered on top of the global glass style. */
@Serializable
data class GlassOverride(
    val enabled: Boolean? = null,
    val blurScale: Float = 1f,
    val refractionScale: Float = 1f,
    val tintAlphaScale: Float = 1f,
    val specularScale: Float = 1f,
    val cornerRadiusOverride: Float = -1f,
)

@Serializable
data class GlassSettings(
    val enabled: Boolean = true,
    val quality: GlassQuality = GlassQuality.AUTO,
    /** Gaussian blur applied to the captured backdrop, in dp. */
    val blurRadius: Float = 28f,
    /** Peak edge displacement of the refracted backdrop, in dp. */
    val refraction: Float = 14f,
    /** Width of the refracting rim band, in dp — the apparent thickness of the glass. */
    val thickness: Float = 22f,
    /** Chromatic aberration at the rim, 0..1. */
    val dispersion: Float = 0.35f,
    val tintColor: Int = 0xFFFFFFFF.toInt(),
    val tintAlpha: Float = 0.12f,
    val brightness: Float = 0.04f,
    val saturation: Float = 1.18f,
    val specular: Float = 0.55f,
    val rimWidth: Float = 1.2f,
    val rimAlpha: Float = 0.5f,
    val innerShadow: Float = 0.22f,
    val grain: Float = 0.02f,
    /** Direction of the virtual light source, in degrees clockwise from "up". */
    val lightAngle: Float = 315f,
    /** Let the accelerometer steer the highlight, like Apple's glass catching real light. */
    val tiltReactive: Boolean = true,
    val tiltStrength: Float = 0.6f,
    val shimmer: Boolean = true,
    val shimmerStrength: Float = 0.10f,
    val shimmerSpeed: Float = 0.22f,
    val surfaces: Map<GlassSurfaceId, GlassOverride> = emptyMap(),
)

// --------------------------------------------------------------------------- wallpaper

enum class WallpaperSource {
    /** Transparent window — the system wallpaper shows through untouched. */
    SYSTEM,

    /** Read the system wallpaper into the app so glass can actually refract it. */
    SNAPSHOT,

    /** Bundled animated mesh gradient, rendered on the GPU. */
    LIQUID_MESH,

    /** Bundled animated aurora field. */
    AURORA,
    SOLID,
}

@Serializable
data class WallpaperSettings(
    val source: WallpaperSource = WallpaperSource.SNAPSHOT,
    val dim: Float = 0.18f,
    val blur: Float = 0f,
    val saturation: Float = 1f,
    val parallax: Boolean = true,
    val parallaxDepth: Float = 0.55f,
    val meshSpeed: Float = 0.25f,
    val meshComplexity: Float = 0.5f,
    val meshFromAccent: Boolean = true,
    val solidColor: Int = 0xFF0B1020.toInt(),
    val darkenOnDrawerOpen: Float = 0.35f,
)

// --------------------------------------------------------------------------- home

enum class IndicatorStyle { DOTS, PILL, BARS, NUMBERS, NONE }

enum class IndicatorPosition { ABOVE_DOCK, BOTTOM_EDGE, TOP }

enum class GlancePillStyle { NONE, DATE, DATE_AND_BATTERY, DYNAMIC_ISLAND }

@Serializable
data class HomeSettings(
    val infiniteScroll: Boolean = false,
    val indicatorStyle: IndicatorStyle = IndicatorStyle.DOTS,
    val indicatorPosition: IndicatorPosition = IndicatorPosition.ABOVE_DOCK,
    val showStatusBar: Boolean = true,
    val lockLayout: Boolean = false,
    val allowRotation: Boolean = false,
    val doubleTapToLock: Boolean = true,
    val glancePill: GlancePillStyle = GlancePillStyle.DATE,
    val glancePillGlass: Boolean = true,
    val topPadding: Float = 18f,
    val bottomPadding: Float = 8f,
    val sidePadding: Float = 12f,
    val keepAppsSortedOnPage: Boolean = false,
    val autoPlaceNewApps: Boolean = true,
    val hideEmptyTrailingPages: Boolean = true,
    val pageSnapStrength: Float = 1f,
)

// --------------------------------------------------------------------------- grid

@Serializable
data class GridSettings(
    val columns: Int = 5,
    val rows: Int = 5,
    val iconSize: Float = 56f,
    val horizontalSpacing: Float = 1f,
    val verticalSpacing: Float = 1f,
    val snapToGrid: Boolean = true,
    val allowWidgetOverlap: Boolean = false,
    val fillGapsOnRemove: Boolean = false,
    val edgeMarginX: Float = 8f,
    val edgeMarginY: Float = 4f,
)

// --------------------------------------------------------------------------- icons

enum class IconShape {
    SQUIRCLE, CIRCLE, ROUNDED_SQUARE, SQUARE, TEARDROP, HEXAGON, COOKIE, CLOVER, PEBBLE, SYSTEM
}

@Serializable
data class IconSettings(
    val shape: IconShape = IconShape.SQUIRCLE,
    val cornerRadiusPercent: Float = 0.44f,
    val squircleSmoothing: Float = 0.72f,
    /** Scale of the adaptive-icon foreground inside the mask. */
    val foregroundScale: Float = 1f,
    /** Force legacy (non-adaptive) icons into the chosen shape. */
    val normalizeLegacyIcons: Boolean = true,
    val legacyIconPadding: Float = 0.14f,
    /** Material You themed icons: monochrome, tinted from the palette. */
    val themedIcons: Boolean = false,
    val themedTintStrength: Float = 1f,
    val iconPack: String? = null,
    val shadow: Float = 0.35f,
    /** Apple-style gloss sheen across the top of every icon. */
    val gloss: Float = 0.0f,
    /** Seat each icon on its own little liquid-glass tile. */
    val glassTile: Boolean = false,
    val glassTileAlpha: Float = 0.5f,
    val saturation: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val greyscale: Boolean = false,
)

// --------------------------------------------------------------------------- labels

@Serializable
data class LabelSettings(
    val showOnHome: Boolean = true,
    val showInDock: Boolean = false,
    val showInDrawer: Boolean = true,
    val showInFolder: Boolean = true,
    val fontSize: Float = 11.5f,
    val maxLines: Int = 1,
    val fontWeight: Int = 500,
    val letterSpacing: Float = 0f,
    val shadow: Float = 0.6f,
    val allCaps: Boolean = false,
    val color: Int = 0,
    val alpha: Float = 0.95f,
    val offsetY: Float = 0f,
)

// --------------------------------------------------------------------------- dock

enum class DockStyle { FLOATING_GLASS, EDGE_GLASS, TRANSPARENT, SOLID }

enum class SearchPillStyle { NONE, GOOGLE_PILL, GLASS_PILL, COMPACT_ORB }

enum class SearchPillPosition { BELOW_DOCK, ABOVE_DOCK, IN_DOCK, TOP }

@Serializable
data class DockSettings(
    val enabled: Boolean = true,
    val style: DockStyle = DockStyle.FLOATING_GLASS,
    val columns: Int = 4,
    val rows: Int = 1,
    val iconSize: Float = 58f,
    val cornerRadius: Float = 34f,
    val horizontalMargin: Float = 14f,
    val bottomMargin: Float = 14f,
    val verticalPadding: Float = 10f,
    val backgroundAlpha: Float = 1f,
    val searchPill: SearchPillStyle = SearchPillStyle.GOOGLE_PILL,
    val searchPillPosition: SearchPillPosition = SearchPillPosition.BELOW_DOCK,
    val searchPillHeight: Float = 48f,
    val showDrawerButton: Boolean = false,
    val separateFromWorkspace: Boolean = true,
)

// --------------------------------------------------------------------------- drawer

enum class DrawerStyle { SHEET, FULLSCREEN, PAGED_GRID, VERTICAL_LIST, CATEGORY_TABS }

enum class DrawerBackground { GLASS, DIM, SOLID, WALLPAPER_ONLY }

enum class DrawerSort { ALPHABETICAL, MOST_USED, RECENTLY_INSTALLED, RECENTLY_USED, COLOR }

enum class DrawerAnimation { SLIDE_UP, FADE_SCALE, LIQUID_RISE, ZOOM_ICONS }

enum class SearchFieldPosition { TOP, BOTTOM }

@Serializable
data class DrawerSettings(
    val style: DrawerStyle = DrawerStyle.SHEET,
    val columns: Int = 4,
    val iconSize: Float = 56f,
    val background: DrawerBackground = DrawerBackground.GLASS,
    val backgroundAlpha: Float = 0.72f,
    val blurAmount: Float = 34f,
    val cornerRadius: Float = 40f,
    val sortOrder: DrawerSort = DrawerSort.ALPHABETICAL,
    val showSearch: Boolean = true,
    val searchPosition: SearchFieldPosition = SearchFieldPosition.TOP,
    val autoFocusKeyboard: Boolean = false,
    val alphabetIndex: Boolean = true,
    val sectionHeaders: Boolean = true,
    val showPredictions: Boolean = true,
    val predictionCount: Int = 4,
    val categoryTabs: Boolean = false,
    val showSystemApps: Boolean = true,
    val showWorkTab: Boolean = true,
    val openAnimation: DrawerAnimation = DrawerAnimation.LIQUID_RISE,
    val closeOnLaunch: Boolean = true,
    val resetScrollOnClose: Boolean = true,
    val rubberBand: Float = 0.55f,
    val verticalPadding: Float = 16f,
    val peekHeightFraction: Float = 0.92f,
)

// --------------------------------------------------------------------------- folders

enum class FolderPreviewStyle { STACK_2X2, GRID_3X3, FAN, RING, CLUSTER }

enum class FolderBackground { GLASS, TINTED, SOLID, NONE }

enum class FolderOpenAnimation { APPLE_ZOOM, MATERIAL_CONTAINER, LIQUID_MORPH, FADE }

enum class FolderLabelPosition { INSIDE_TOP, OUTSIDE_BOTTOM, NONE }

@Serializable
data class FolderSettings(
    val previewStyle: FolderPreviewStyle = FolderPreviewStyle.STACK_2X2,
    val background: FolderBackground = FolderBackground.GLASS,
    val openAnimation: FolderOpenAnimation = FolderOpenAnimation.APPLE_ZOOM,
    val columns: Int = 3,
    val rows: Int = 3,
    val labelPosition: FolderLabelPosition = FolderLabelPosition.INSIDE_TOP,
    val autoLabel: Boolean = true,
    val backdropBlur: Float = 40f,
    val backdropDim: Float = 0.35f,
    val cornerRadiusPercent: Float = 0.4f,
    val previewPadding: Float = 0.16f,
    val closeOnLaunch: Boolean = true,
    val dissolveSingleItem: Boolean = true,
    val openOnHover: Boolean = true,
)

// --------------------------------------------------------------------------- search

enum class SearchProvider { LUMEN, GOOGLE, DUCKDUCKGO, BRAVE, BING, SYSTEM_DEFAULT }

@Serializable
data class SearchSettings(
    val webProvider: SearchProvider = SearchProvider.GOOGLE,
    val searchApps: Boolean = true,
    val searchShortcuts: Boolean = true,
    val searchContacts: Boolean = false,
    val searchSettings: Boolean = true,
    val searchWeb: Boolean = true,
    val calculator: Boolean = true,
    val unitConverter: Boolean = true,
    val fuzzyMatching: Boolean = true,
    val fuzzyThreshold: Float = 0.55f,
    val launchOnSingleResult: Boolean = false,
    val showRecentSearches: Boolean = true,
    val voiceButton: Boolean = true,
    val maxResults: Int = 24,
)

// --------------------------------------------------------------------------- motion

enum class PageTransition { SLIDE, DEPTH, ZOOM, CUBE, CAROUSEL, FLIP, LIQUID_SLIDE, PARALLAX }

enum class AppOpenAnimation { SYSTEM, ZOOM_FROM_ICON, LIQUID_EXPAND, FADE, SLIDE_UP }

enum class OverscrollStyle { RUBBER_BAND, STRETCH, NONE }

@Serializable
data class MotionSettings(
    val speed: Float = 1f,
    val springStiffness: Float = 380f,
    val springDamping: Float = 0.82f,
    val pageTransition: PageTransition = PageTransition.LIQUID_SLIDE,
    val appOpenAnimation: AppOpenAnimation = AppOpenAnimation.ZOOM_FROM_ICON,
    val overscroll: OverscrollStyle = OverscrollStyle.RUBBER_BAND,
    val iconPressScale: Float = 0.9f,
    val wiggleInEditMode: Boolean = true,
    val parallaxDepth: Float = 0.4f,
    val haptics: Boolean = true,
    val hapticStrength: Float = 0.7f,
    val reduceMotion: Boolean = false,
    val blurDuringTransitions: Boolean = true,
)

// --------------------------------------------------------------------------- gestures

enum class GestureTrigger {
    SWIPE_UP, SWIPE_DOWN, DOUBLE_TAP, LONG_PRESS_EMPTY, PINCH_IN, PINCH_OUT,
    TWO_FINGER_SWIPE_UP, TWO_FINGER_SWIPE_DOWN, SWIPE_LEFT_EDGE, SWIPE_RIGHT_EDGE, HOME_PRESS,
}

enum class LauncherAction {
    NONE, OPEN_DRAWER, CLOSE_DRAWER, OPEN_SEARCH, OPEN_SETTINGS, EXPAND_NOTIFICATIONS,
    EXPAND_QUICK_SETTINGS, LOCK_SCREEN, TOGGLE_EDIT_MODE, OPEN_WIDGET_PICKER,
    OPEN_ASSISTANT, OPEN_VOICE_SEARCH, SHOW_HIDDEN_APPS, GO_TO_FIRST_PAGE,
    OPEN_APP, TOGGLE_GLASS, OPEN_WALLPAPER_PICKER, OPEN_RECENTS,
}

@Serializable
data class GestureSettings(
    val bindings: Map<GestureTrigger, LauncherAction> = mapOf(
        GestureTrigger.SWIPE_UP to LauncherAction.OPEN_DRAWER,
        GestureTrigger.SWIPE_DOWN to LauncherAction.EXPAND_NOTIFICATIONS,
        GestureTrigger.DOUBLE_TAP to LauncherAction.LOCK_SCREEN,
        GestureTrigger.LONG_PRESS_EMPTY to LauncherAction.TOGGLE_EDIT_MODE,
        GestureTrigger.PINCH_IN to LauncherAction.TOGGLE_EDIT_MODE,
        GestureTrigger.PINCH_OUT to LauncherAction.OPEN_WIDGET_PICKER,
        GestureTrigger.TWO_FINGER_SWIPE_DOWN to LauncherAction.EXPAND_QUICK_SETTINGS,
        GestureTrigger.TWO_FINGER_SWIPE_UP to LauncherAction.OPEN_SETTINGS,
        GestureTrigger.SWIPE_LEFT_EDGE to LauncherAction.NONE,
        GestureTrigger.SWIPE_RIGHT_EDGE to LauncherAction.NONE,
        GestureTrigger.HOME_PRESS to LauncherAction.GO_TO_FIRST_PAGE,
    ),
    /** Flat [dev.lumen.launcher.data.model.AppKey] per trigger, for [LauncherAction.OPEN_APP]. */
    val appTargets: Map<GestureTrigger, String> = emptyMap(),
    val sensitivity: Float = 1f,
    val requireLongSwipe: Boolean = false,
    val dockSwipeOpensDrawer: Boolean = true,
)

// --------------------------------------------------------------------------- widgets

@Serializable
data class WidgetSettings(
    val enabled: Boolean = true,
    val glassBackdrop: Boolean = true,
    val glassAlpha: Float = 0.45f,
    val cornerRadius: Float = 24f,
    val clipToCorners: Boolean = true,
    val padding: Float = 4f,
    val allowResize: Boolean = true,
    val showResizeHandles: Boolean = true,
    val snapResizeToGrid: Boolean = true,
    val reconfigureOnLongPress: Boolean = true,
)

// --------------------------------------------------------------------------- badges

enum class BadgeStyle { NONE, DOT, DOT_LARGE, COUNT }

enum class BadgePosition { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

@Serializable
data class BadgeSettings(
    val style: BadgeStyle = BadgeStyle.DOT,
    val position: BadgePosition = BadgePosition.TOP_RIGHT,
    /** 0 means "use the accent colour". */
    val color: Int = 0,
    val size: Float = 1f,
    val maxCount: Int = 99,
    val showOnFolders: Boolean = true,
    val showInDock: Boolean = true,
    val showInDrawer: Boolean = true,
)

// --------------------------------------------------------------------------- per-app

@Serializable
data class AppOverride(
    val label: String? = null,
    /** File name inside `filesDir/lumen/custom-icons`, or `pack:<package>:<drawable>`. */
    val icon: String? = null,
    val tint: Int = 0,
    val hidden: Boolean = false,
    val badgesMuted: Boolean = false,
)

@Serializable
data class AppCustomizations(
    /** Keyed by [dev.lumen.launcher.data.model.AppKey.flat]. */
    val overrides: Map<String, AppOverride> = emptyMap(),
    val hidden: Set<String> = emptySet(),
    val drawerPinned: Set<String> = emptySet(),
) {
    fun labelFor(flatKey: String, fallback: String): String =
        overrides[flatKey]?.label?.takeIf { it.isNotBlank() } ?: fallback

    fun isHidden(flatKey: String): Boolean =
        flatKey in hidden || overrides[flatKey]?.hidden == true
}

// --------------------------------------------------------------------------- advanced

@Serializable
data class AdvancedSettings(
    val showFps: Boolean = false,
    val debugOverlay: Boolean = false,
    val iconCacheMb: Int = 48,
    val gpuFriendlyMode: Boolean = false,
    val restoreDrawerScroll: Boolean = true,
    val experimentalLensing: Boolean = true,
    val verboseLogging: Boolean = false,
    val doubleBackToExitEdit: Boolean = true,
    val settingsSearchHistory: Boolean = true,
)
