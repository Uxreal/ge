package dev.lumen.launcher.core.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * §3's contrast floor: 4.5:1 for all text on all wallpapers, implemented as a scrim computed from
 * the wallpaper region *behind the text* rather than a fixed 20% black.
 *
 * A fixed scrim is the usual shortcut and it fails at both ends — it muddies a dark wallpaper that
 * needed nothing, and still leaves white labels illegible on a bright one. This solves for the
 * actual alpha required, per region, and returns zero when the wallpaper is already dark enough.
 */
object ContrastScrim {

    const val TARGET_RATIO = 4.5f

    /**
     * Alpha of a black scrim needed to bring [textColor] to [targetRatio] over a background of
     * relative luminance [backgroundLuminance] (0..1, as produced by [Color.luminance]).
     *
     * WCAG contrast is `(lighter + 0.05) / (darker + 0.05)`. For light text the background must fall
     * to at most `(Lt + 0.05) / ratio - 0.05`; compositing black at alpha `a` scales the
     * background's luminance by `(1 - a)`, so the required alpha follows directly.
     *
     * @return 0f when no scrim is needed, capped at [maxAlpha] so a scrim never becomes a blackout.
     */
    fun blackScrimAlpha(
        backgroundLuminance: Float,
        textColor: Color = Color.White,
        targetRatio: Float = TARGET_RATIO,
        maxAlpha: Float = 0.65f,
    ): Float {
        val lb = backgroundLuminance.coerceIn(0f, 1f)
        val lt = textColor.luminance().coerceIn(0f, 1f)
        if (lt <= lb) return 0f // Dark text on a lighter background: see whiteScrimAlpha.

        val allowed = (lt + 0.05f) / targetRatio - 0.05f
        if (lb <= allowed) return 0f
        if (allowed <= 0f) return maxAlpha

        return (1f - allowed / lb).coerceIn(0f, maxAlpha)
    }

    /** The mirror case: dark text over a background that is too dark, lifted with a white scrim. */
    fun whiteScrimAlpha(
        backgroundLuminance: Float,
        textColor: Color = Color.Black,
        targetRatio: Float = TARGET_RATIO,
        maxAlpha: Float = 0.65f,
    ): Float {
        val lb = backgroundLuminance.coerceIn(0f, 1f)
        val lt = textColor.luminance().coerceIn(0f, 1f)
        if (lt >= lb) return 0f

        // Background must rise to at least ratio * (Lt + 0.05) - 0.05.
        val required = targetRatio * (lt + 0.05f) - 0.05f
        if (lb >= required) return 0f
        if (required >= 1f) return maxAlpha

        // Compositing white at alpha a: L' = (1 - a) * Lb + a.
        return ((required - lb) / (1f - lb)).coerceIn(0f, maxAlpha)
    }

    /**
     * Picks the scrim for a region: black for light text, white for dark text. Returns the colour
     * already carrying its alpha, or [Color.Transparent] when the region is legible as-is.
     */
    fun scrimFor(
        backgroundLuminance: Float,
        textColor: Color,
        targetRatio: Float = TARGET_RATIO,
    ): Color {
        val lt = textColor.luminance()
        return if (lt > backgroundLuminance) {
            val alpha = blackScrimAlpha(backgroundLuminance, textColor, targetRatio)
            if (alpha <= 0f) Color.Transparent else Color.Black.copy(alpha = alpha)
        } else {
            val alpha = whiteScrimAlpha(backgroundLuminance, textColor, targetRatio)
            if (alpha <= 0f) Color.Transparent else Color.White.copy(alpha = alpha)
        }
    }

    /** A label colour and the scrim behind it, together guaranteeing the contrast floor. */
    data class LabelContrast(
        val textColor: Color,
        val scrim: Color,
        val ratio: Float,
    )

    /**
     * Resolves a label against a wallpaper region so the §3 floor actually holds.
     *
     * Scrim alone is not always enough: over a near-white wallpaper, white text would need a scrim so
     * heavy it becomes a black bar. So the preferred colour is tried first with the lightest scrim
     * that works, and if that cannot reach [targetRatio] the polarity flips — dark labels on a bright
     * wallpaper, which is both legible and what the region actually wants. [maxAlpha] therefore stays
     * low enough that a scrim never reads as a slab.
     */
    fun resolveLabelContrast(
        backgroundLuminance: Float,
        preferred: Color = Color.White,
        targetRatio: Float = TARGET_RATIO,
        maxAlpha: Float = 0.5f,
    ): LabelContrast {
        val keepPreferred = attempt(backgroundLuminance, preferred, targetRatio, maxAlpha)
        if (keepPreferred.ratio >= targetRatio) return keepPreferred

        val flipped = if (preferred.luminance() > 0.5f) Color.Black else Color.White
        val alternative = attempt(backgroundLuminance, flipped, targetRatio, maxAlpha)
        return if (alternative.ratio > keepPreferred.ratio) alternative else keepPreferred
    }

    private fun attempt(
        backgroundLuminance: Float,
        textColor: Color,
        targetRatio: Float,
        maxAlpha: Float,
    ): LabelContrast {
        val lb = backgroundLuminance.coerceIn(0f, 1f)
        val lt = textColor.luminance().coerceIn(0f, 1f)
        return if (lt > lb) {
            val alpha = blackScrimAlpha(lb, textColor, targetRatio, maxAlpha)
            val composited = lb * (1f - alpha)
            LabelContrast(
                textColor = textColor,
                scrim = if (alpha <= 0f) Color.Transparent else Color.Black.copy(alpha = alpha),
                ratio = ratio(lt, composited),
            )
        } else {
            val alpha = whiteScrimAlpha(lb, textColor, targetRatio, maxAlpha)
            val composited = (1f - alpha) * lb + alpha
            LabelContrast(
                textColor = textColor,
                scrim = if (alpha <= 0f) Color.Transparent else Color.White.copy(alpha = alpha),
                ratio = ratio(composited, lt),
            )
        }
    }

    /** Actual contrast ratio between two relative luminances, for tests and debug overlays. */
    fun ratio(luminanceA: Float, luminanceB: Float): Float {
        val hi = maxOf(luminanceA, luminanceB).coerceIn(0f, 1f)
        val lo = minOf(luminanceA, luminanceB).coerceIn(0f, 1f)
        return (hi + 0.05f) / (lo + 0.05f)
    }
}
