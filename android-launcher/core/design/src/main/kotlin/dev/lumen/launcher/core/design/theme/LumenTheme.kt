package dev.lumen.launcher.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.interaction.HapticIntensity
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.interaction.rememberHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.motion.MotionTokens
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.shape.SuperellipseShape
import dev.lumen.launcher.core.design.surface.FrostedTokens
import dev.lumen.launcher.core.design.surface.LocalFrostedTokens
import dev.lumen.launcher.core.design.surface.SurfaceRole
import androidx.compose.foundation.shape.CornerSize

/** §3: light / dark / auto plus a true-black variant. */
enum class ThemeMode { LIGHT, DARK, AUTO, TRUE_BLACK }

/**
 * Wallpaper-extracted colours, produced in the data layer (Palette) and passed in. Keeping extraction
 * out of `:core:design` is what lets this module render in isolation for previews and screenshot
 * tests.
 */
@Immutable
data class WallpaperPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
    val isDark: Boolean,
) {
    companion object {
        /** Neutral fallback, deliberately not a purple/blue gradient (§1.1). */
        val Fallback = WallpaperPalette(
            primary = Color(0xFFE3B23C),
            secondary = Color(0xFF6FA8A0),
            tertiary = Color(0xFFC96A5B),
            neutral = Color(0xFF1A1A1C),
            isDark = true,
        )
    }
}

/** §3: four independently themable surface groups, each either wallpaper-driven or manually seeded. */
@Immutable
data class SurfaceThemeConfig(
    val useWallpaper: Boolean = true,
    val seed: Color? = null,
)

@Immutable
data class SurfaceColors(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

@Immutable
data class LumenThemeConfig(
    val mode: ThemeMode = ThemeMode.AUTO,
    val palette: WallpaperPalette? = null,
    val groups: Map<SurfaceRole, SurfaceThemeConfig> = emptyMap(),
    val typography: LumenTypography = LumenTypography(),
    val motion: MotionTokens = MotionTokens(),
    val hapticIntensity: HapticIntensity = HapticIntensity.STANDARD,
    val frosted: Map<SurfaceRole, FrostedTokens> = defaultFrostedTokens(),
    val smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
)

/**
 * §1.1 again: frosting is per-role, not global. The Capsule is the most opaque surface because it
 * must stay legible over anything; icons get none at all, because an icon grid does not need to
 * establish layering over itself.
 */
fun defaultFrostedTokens(): Map<SurfaceRole, FrostedTokens> = mapOf(
    // The Capsule paints itself camera-black and opaque (D25) — a translucent lens cannot swallow
    // a punch-hole camera, and opacity is what frees the backdrop recorder on a still home screen.
    SurfaceRole.CAPSULE to FrostedTokens.Off,
    SurfaceRole.SHEETS to FrostedTokens(
        blurRadius = 40.dp,
        refraction = 14.dp,
        band = 22.dp,
        tint = Color.Black.copy(alpha = 0.26f),
    ),
    SurfaceRole.SEARCH to FrostedTokens(
        blurRadius = 28.dp,
        refraction = 8.dp,
        band = 14.dp,
        tint = Color.White.copy(alpha = 0.10f),
    ),
    SurfaceRole.ICONS to FrostedTokens.Off,
)

/**
 * The root theme. Supplies Material 3 with the §3 type scale and superellipse shapes so stock
 * components inherit the launcher's identity, and publishes the motion, haptic and frosted tokens
 * every surface reads.
 */
@Composable
fun LumenTheme(
    config: LumenThemeConfig = LumenThemeConfig(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (config.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.TRUE_BLACK -> true
        ThemeMode.AUTO -> systemDark
    }
    val palette = config.palette ?: WallpaperPalette.Fallback

    val scheme = remember(config.mode, dark, palette, config.groups) {
        buildScheme(
            dark = dark,
            trueBlack = config.mode == ThemeMode.TRUE_BLACK,
            palette = palette,
            seed = config.groups[SurfaceRole.SHEETS]?.seed,
            useWallpaper = config.groups[SurfaceRole.SHEETS]?.useWallpaper ?: true,
        )
    }

    val surfaceColors = remember(config.groups, palette, dark) {
        SurfaceRole.entries.associateWith { role ->
            val groupConfig = config.groups[role] ?: SurfaceThemeConfig()
            val accent = when {
                !groupConfig.useWallpaper && groupConfig.seed != null -> groupConfig.seed
                else -> when (role) {
                    SurfaceRole.CAPSULE -> palette.primary
                    SurfaceRole.ICONS -> palette.secondary
                    SurfaceRole.SHEETS -> palette.primary
                    SurfaceRole.SEARCH -> palette.tertiary
                }
            }
            val container = if (dark) {
                lerp(Color.Black, accent, 0.10f)
            } else {
                lerp(Color.White, accent, 0.12f)
            }
            SurfaceColors(
                accent = accent,
                container = container,
                onContainer = if (container.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }

    val shapes = remember(config.smoothness) { buildShapes(config.smoothness) }
    val haptics = rememberHaptics(config.hapticIntensity)

    CompositionLocalProvider(
        LocalMotion provides config.motion,
        LocalHaptics provides haptics,
        LocalTypography provides config.typography,
        LocalFrostedTokens provides config.frosted,
        LocalSurfaceColors provides surfaceColors,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = config.typography.toMaterialTypography(),
            shapes = shapes,
            content = content,
        )
    }
}

private fun buildScheme(
    dark: Boolean,
    trueBlack: Boolean,
    palette: WallpaperPalette,
    seed: Color?,
    useWallpaper: Boolean,
): ColorScheme {
    val accent = if (!useWallpaper && seed != null) seed else palette.primary
    val secondary = if (!useWallpaper && seed != null) lerp(seed, palette.secondary, 0.4f) else palette.secondary

    val base = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
            secondary = secondary,
            tertiary = palette.tertiary,
            background = Color(0xFF0B0B0D),
            surface = Color(0xFF111114),
            surfaceVariant = Color(0xFF1C1C20),
            onSurface = Color(0xFFF2F2F5),
            onSurfaceVariant = Color(0xFFC2C2C9),
            outline = Color(0xFF3C3C44),
        )
    } else {
        lightColorScheme(
            primary = lerp(accent, Color.Black, 0.2f),
            onPrimary = Color.White,
            secondary = lerp(secondary, Color.Black, 0.15f),
            tertiary = lerp(palette.tertiary, Color.Black, 0.15f),
            background = Color(0xFFF6F6F8),
            surface = Color(0xFFFCFCFE),
            surfaceVariant = Color(0xFFE7E7EC),
            onSurface = Color(0xFF121215),
            onSurfaceVariant = Color(0xFF46464E),
            outline = Color(0xFFB6B6BE),
        )
    }

    return if (trueBlack) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF060608),
        )
    } else {
        base
    }
}

/** Material's shape scale, rebuilt on the superellipse so stock components match the launcher. */
private fun buildShapes(smoothness: Float): Shapes {
    fun corner(radius: Int) = SuperellipseShape(CornerSize(radius.dp), smoothness)
    // D39's single ladder: every container in the launcher sits on one of these five stops.
    return Shapes(
        extraSmall = corner(8),
        small = corner(14),
        medium = corner(20),
        large = corner(28),
        extraLarge = corner(36),
    )
}

val LocalSurfaceColors = staticCompositionLocalOf<Map<SurfaceRole, SurfaceColors>> { emptyMap() }
