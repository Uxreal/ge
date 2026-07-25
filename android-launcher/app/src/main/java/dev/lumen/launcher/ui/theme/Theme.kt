package dev.lumen.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.lumen.launcher.data.prefs.AccentSource
import dev.lumen.launcher.data.prefs.CornerStyle
import dev.lumen.launcher.data.prefs.LauncherSettings
import dev.lumen.launcher.data.prefs.ThemeMode
import dev.lumen.launcher.data.prefs.TypographyStyle
import dev.lumen.launcher.ui.LocalSettings
import dev.lumen.launcher.ui.glass.LauncherShapes

/**
 * Palette: Google's system on the inside, Apple's restraint on the outside.
 *
 * When Material You is available the scheme comes straight from the wallpaper, exactly like the
 * Pixel launcher. Otherwise a hand-tuned scheme is generated from the user's accent so the look
 * holds up on every device — and either way the surfaces stay low-chroma and near-black so glass
 * has something quiet to sit on.
 */
@Composable
fun LumenTheme(
    settings: LauncherSettings = LocalSettings.current,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.theme.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme = remember(settings.theme, dark, context) {
        buildColorScheme(
            dark = dark,
            dynamic = settings.theme.dynamicColor &&
                settings.theme.accentSource == AccentSource.MATERIAL_YOU &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            accent = Color(settings.theme.accentColor),
            secondary = Color(settings.theme.secondaryColor),
            contrast = settings.theme.contrast,
            amoled = settings.theme.amoledBlack,
            monochrome = settings.theme.monochromeSurfaces,
            context = context,
        )
    }

    val typography = remember(settings.theme.typography, settings.theme.fontScale, settings.theme.fontWeightBias) {
        buildTypography(
            style = settings.theme.typography,
            scale = settings.theme.fontScale,
            weightBias = settings.theme.fontWeightBias,
        )
    }

    val shapes = remember(settings.theme.cornerStyle, settings.theme.cornerScale) {
        buildShapes(settings.theme.cornerStyle, settings.theme.cornerScale)
    }

    val motion = remember(settings.motion) { MotionSpec.from(settings.motion) }

    CompositionLocalProvider(LocalMotion provides motion) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}

private fun buildColorScheme(
    dark: Boolean,
    dynamic: Boolean,
    accent: Color,
    secondary: Color,
    contrast: Float,
    amoled: Boolean,
    monochrome: Boolean,
    context: android.content.Context,
): ColorScheme {
    var scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        dark -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF07101F),
            primaryContainer = accent.copy(alpha = 0.28f).compositeOverBlack(),
            secondary = secondary,
            tertiary = lerp(accent, secondary, 0.5f),
            background = Color(0xFF07090F),
            surface = Color(0xFF0C1017),
            surfaceVariant = Color(0xFF1A1F29),
            onSurface = Color(0xFFEEF1F6),
            onSurfaceVariant = Color(0xFFBEC5D2),
            outline = Color(0xFF3A414E),
        )

        else -> lightColorScheme(
            primary = accent.darken(0.18f),
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.18f).compositeOverWhite(),
            secondary = secondary.darken(0.12f),
            tertiary = lerp(accent, secondary, 0.5f).darken(0.15f),
            background = Color(0xFFF7F8FB),
            surface = Color(0xFFFDFDFF),
            surfaceVariant = Color(0xFFE6E9F0),
            onSurface = Color(0xFF101319),
            onSurfaceVariant = Color(0xFF454B57),
            outline = Color(0xFFB9BFCB),
        )
    }

    if (amoled && dark) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
        )
    }

    if (monochrome) {
        scheme = scheme.copy(
            surfaceVariant = scheme.surfaceVariant.desaturate(),
            surfaceContainer = scheme.surfaceContainer.desaturate(),
            surfaceContainerHigh = scheme.surfaceContainerHigh.desaturate(),
        )
    }

    if (contrast != 0f) {
        val amount = contrast.coerceIn(-1f, 1f)
        scheme = if (amount > 0f) {
            scheme.copy(
                onSurface = lerp(scheme.onSurface, if (dark) Color.White else Color.Black, amount),
                onSurfaceVariant = lerp(
                    scheme.onSurfaceVariant,
                    if (dark) Color.White else Color.Black,
                    amount * 0.8f,
                ),
                outline = lerp(scheme.outline, if (dark) Color.White else Color.Black, amount * 0.5f),
            )
        } else {
            scheme.copy(
                onSurface = lerp(scheme.onSurface, scheme.surface, -amount * 0.4f),
                onSurfaceVariant = lerp(scheme.onSurfaceVariant, scheme.surface, -amount * 0.5f),
            )
        }
    }

    return scheme
}

private fun Color.compositeOverBlack(): Color =
    lerp(Color.Black, copy(alpha = 1f), alpha)

private fun Color.compositeOverWhite(): Color =
    lerp(Color.White, copy(alpha = 1f), alpha)

private fun Color.darken(amount: Float): Color = lerp(this, Color.Black, amount)

private fun Color.desaturate(): Color {
    val luma = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return lerp(this, Color(luma, luma, luma, alpha), 0.75f)
}

internal fun fontFamilyFor(style: TypographyStyle): FontFamily = when (style) {
    TypographyStyle.SYSTEM -> FontFamily.Default
    // Google Sans Rounded on Pixels, Roboto's rounded variant elsewhere, graceful fallback if absent.
    TypographyStyle.ROUNDED -> FontFamily(Font(DeviceFontFamilyName("sans-serif-rounded")))
    TypographyStyle.GEOMETRIC -> FontFamily.SansSerif
    TypographyStyle.SERIF -> FontFamily.Serif
    TypographyStyle.MONO -> FontFamily.Monospace
}

private fun buildTypography(style: TypographyStyle, scale: Float, weightBias: Int): Typography {
    val family = fontFamilyFor(style)
    val s = scale.coerceIn(0.7f, 1.6f)
    fun weight(base: Int) = FontWeight((base + weightBias).coerceIn(100, 900))
    val platform = PlatformTextStyle(includeFontPadding = false)
    val base = Typography()

    return Typography(
        displayLarge = base.displayLarge.copy(
            fontFamily = family, fontSize = 54.sp * s, fontWeight = weight(400), platformStyle = platform,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = family, fontSize = 42.sp * s, fontWeight = weight(400), platformStyle = platform,
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = family, fontSize = 34.sp * s, fontWeight = weight(500), platformStyle = platform,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = family, fontSize = 30.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = family, fontSize = 26.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = family, fontSize = 22.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = family, fontSize = 20.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = family, fontSize = 16.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = family, fontSize = 14.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = family, fontSize = 16.sp * s, fontWeight = weight(400), platformStyle = platform,
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = family, fontSize = 14.sp * s, fontWeight = weight(400), platformStyle = platform,
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = family, fontSize = 12.sp * s, fontWeight = weight(400), platformStyle = platform,
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = family, fontSize = 14.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = family, fontSize = 12.sp * s, fontWeight = weight(600), platformStyle = platform,
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = family, fontSize = 11.sp * s, fontWeight = weight(500), platformStyle = platform,
        ),
    )
}

private fun buildShapes(style: CornerStyle, scale: Float): Shapes {
    val s = scale.coerceIn(0f, 2f)
    val smoothing = when (style) {
        CornerStyle.SQUIRCLE -> 0.78f
        CornerStyle.ROUNDED -> 0f
        CornerStyle.SHARP -> 0f
    }
    fun corner(dp: Float) = LauncherShapes.squircle(
        radius = androidx.compose.ui.unit.Dp(if (style == CornerStyle.SHARP) 0f else dp * s),
        smoothing = smoothing,
    )
    return Shapes(
        extraSmall = corner(6f),
        small = corner(12f),
        medium = corner(18f),
        large = corner(26f),
        extraLarge = corner(36f),
    )
}
