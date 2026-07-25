package dev.lumen.launcher.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The §3 type scale, verbatim. These six styles are the only text styles in the launcher.
 *
 * The family is the platform variable sans (see DECISIONS D4) behind a single token, so swapping in
 * a bundled face with an optical-size axis later is a one-line change. §3's optical-sizing
 * requirement is unmet until then, and is tracked in `STATUS.md` rather than quietly ignored.
 */
@Immutable
data class LumenTypography(
    val family: FontFamily = FontFamily.Default,
    /** User-facing font scale on top of the system setting; §9 requires legibility through 200%. */
    val scale: Float = 1f,
) {
    private val platform = PlatformTextStyle(includeFontPadding = false)

    private val trim = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

    private fun style(
        sizeSp: Float,
        weight: Int,
        trackingEm: Float = 0f,
        lineHeightMultiple: Float? = null,
    ) = TextStyle(
        fontFamily = family,
        fontSize = (sizeSp * scale).sp,
        fontWeight = FontWeight(weight),
        letterSpacing = trackingEm.em,
        lineHeight = lineHeightMultiple?.let { (sizeSp * scale * it).sp } ?: TextStyle.Default.lineHeight,
        platformStyle = platform,
        lineHeightStyle = trim,
    )

    /** 13sp / 600 / −0.1 tracking — the compact Capsule pill. */
    val capsuleGlance: TextStyle get() = style(13f, 600, trackingEm = -0.1f / 13f)

    /** 17sp / 600 — the expanded Capsule card title. */
    val capsuleTitle: TextStyle get() = style(17f, 600)

    /** 12sp / 500 / 1.15 line-height — icon captions, one line by default, two optional. */
    val iconLabel: TextStyle get() = style(12f, 500, lineHeightMultiple = 1.15f)

    /** 34sp / 700 / −0.5 tracking — the §7 large collapsing header at rest. */
    val headerLarge: TextStyle get() = style(34f, 700, trackingEm = -0.5f / 34f)

    /** 20sp / 600 — the same header once collapsed. Never animate between the two: cross-fade. */
    val headerCollapsed: TextStyle get() = style(20f, 600)

    /** 15sp / 400 / 1.4 — body copy. */
    val body: TextStyle get() = style(15f, 400, lineHeightMultiple = 1.4f)

    /** 12sp / 500 — control-panel tile labels. */
    val tileLabel: TextStyle get() = style(12f, 500)

    /**
     * Material 3 components (sheets, dialogs, switches) read from `MaterialTheme.typography`, so the
     * scale is mapped across rather than letting M3 fall back to its own defaults.
     */
    fun toMaterialTypography(): Typography = Typography(
        displayLarge = headerLarge,
        displayMedium = headerLarge,
        displaySmall = headerCollapsed,
        headlineLarge = headerLarge,
        headlineMedium = headerCollapsed,
        headlineSmall = headerCollapsed,
        titleLarge = headerCollapsed,
        titleMedium = capsuleTitle,
        titleSmall = capsuleTitle,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = iconLabel,
        labelLarge = capsuleGlance,
        labelMedium = tileLabel,
        labelSmall = iconLabel,
    )
}

val LocalTypography = staticCompositionLocalOf { LumenTypography() }
