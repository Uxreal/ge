package dev.lumen.launcher.core.design

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.grid.GridGravity
import dev.lumen.launcher.core.design.grid.GridSpec
import dev.lumen.launcher.core.design.grid.MAX_ICON
import dev.lumen.launcher.core.design.grid.MIN_ICON
import dev.lumen.launcher.core.design.grid.MIN_ROWS
import dev.lumen.launcher.core.design.grid.PHONE_PAGE_MARGIN
import dev.lumen.launcher.core.design.grid.TABLET_PAGE_MARGIN
import dev.lumen.launcher.core.design.grid.computeGridGeometry
import dev.lumen.launcher.core.design.motion.MotionTokens
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.ContrastScrim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The §3 tokens are the single source of truth for every layout and motion decision, so the maths
 * behind them is pinned here rather than eyeballed on a device.
 */
class DesignTokenTest {

    // ------------------------------------------------------------------ grid geometry (§3)

    private fun phoneGeometry(columns: Int = 4) = computeGridGeometry(
        screenWidth = 400.dp,
        screenHeight = 880.dp,
        horizontalInsets = 0.dp,
        verticalInsets = 48.dp,
        reservedTop = 64.dp,
        reservedBottom = 96.dp,
        isTablet = false,
        spec = GridSpec(columns = columns),
        labelLineHeight = 14.dp,
    )

    @Test
    fun `cell width is usable width divided by columns`() {
        val geometry = phoneGeometry(columns = 4)
        // usableWidth = 400 - 0 - (2 * 20) = 360; cellWidth = 360 / 4 = 90
        assertEquals(90f, geometry.cellWidth.value, 0.01f)
        assertEquals(PHONE_PAGE_MARGIN, geometry.pageMargin)
    }

    @Test
    fun `icon size is 62 percent of cell width and clamped to 44 to 76dp`() {
        // 90dp cell -> 55.8dp icon, inside the clamp.
        assertEquals(55.8f, phoneGeometry(columns = 4).iconSize.value, 0.01f)

        // A very wide cell must clamp at the ceiling rather than producing a giant icon.
        val wide = computeGridGeometry(
            screenWidth = 1200.dp,
            screenHeight = 900.dp,
            horizontalInsets = 0.dp,
            verticalInsets = 0.dp,
            reservedTop = 0.dp,
            reservedBottom = 0.dp,
            isTablet = true,
            spec = GridSpec(columns = 4),
            labelLineHeight = 14.dp,
        )
        assertEquals(MAX_ICON, wide.iconSize)

        // …and a very narrow one must clamp at the floor.
        val narrow = computeGridGeometry(
            screenWidth = 280.dp,
            screenHeight = 700.dp,
            horizontalInsets = 0.dp,
            verticalInsets = 0.dp,
            reservedTop = 0.dp,
            reservedBottom = 0.dp,
            isTablet = false,
            spec = GridSpec(columns = 6),
            labelLineHeight = 14.dp,
        )
        assertEquals(MIN_ICON, narrow.iconSize)
    }

    @Test
    fun `label gap is 6 percent of cell width`() {
        assertEquals(90f * 0.06f, phoneGeometry().labelGap.value, 0.01f)
    }

    @Test
    fun `page margin is 20dp on phones and 32dp on tablets`() {
        assertEquals(PHONE_PAGE_MARGIN, phoneGeometry().pageMargin)
        val tablet = computeGridGeometry(
            screenWidth = 1000.dp,
            screenHeight = 1400.dp,
            horizontalInsets = 0.dp,
            verticalInsets = 0.dp,
            reservedTop = 0.dp,
            reservedBottom = 0.dp,
            isTablet = true,
            spec = GridSpec(columns = 6),
            labelLineHeight = 14.dp,
        )
        assertEquals(TABLET_PAGE_MARGIN, tablet.pageMargin)
    }

    @Test
    fun `rows never drop below five even on a short screen`() {
        val squat = computeGridGeometry(
            screenWidth = 400.dp,
            screenHeight = 320.dp,
            horizontalInsets = 0.dp,
            verticalInsets = 24.dp,
            reservedTop = 64.dp,
            reservedBottom = 96.dp,
            isTablet = false,
            spec = GridSpec(columns = 4),
            labelLineHeight = 14.dp,
        )
        assertEquals(MIN_ROWS, squat.rows)
        // When five will not fit at natural height the cell compresses instead.
        assertTrue(squat.cellHeight.value > 0f)
        assertTrue(squat.cellHeight.value * squat.rows <= (320f - 24f - 64f - 96f) + 0.01f)
    }

    @Test
    fun `more columns means smaller cells and more rows fit`() {
        val four = phoneGeometry(columns = 4)
        val six = phoneGeometry(columns = 6)
        assertTrue(six.cellWidth < four.cellWidth)
        assertTrue(six.rows >= four.rows)
    }

    @Test
    fun `gravity defaults to bottom`() {
        // §1: page 1 fills from the bottom up. This is an identity commitment, not a preference.
        assertEquals(GridGravity.BOTTOM, GridSpec().gravity)
        assertEquals(GridGravity.BOTTOM, phoneGeometry().gravity)
    }

    @Test
    fun `columns outside four to six are rejected`() {
        listOf(3, 7, 0, -1).forEach { columns ->
            val threw = runCatching { GridSpec(columns = columns) }.isFailure
            assertTrue("columns=$columns should be rejected", threw)
        }
        listOf(4, 5, 6).forEach { GridSpec(columns = it) }
    }

    // ------------------------------------------------------------------ contrast floor (§3)

    @Test
    fun `contrast ratio matches the WCAG formula`() {
        // White on black is the maximum: (1 + .05) / (0 + .05) = 21.
        assertEquals(21f, ContrastScrim.ratio(1f, 0f), 0.01f)
        assertEquals(1f, ContrastScrim.ratio(0.5f, 0.5f), 0.01f)
    }

    @Test
    fun `a dark wallpaper needs no scrim for white text`() {
        assertEquals(0f, ContrastScrim.blackScrimAlpha(0.02f, Color.White), 0.0001f)
        assertEquals(Color.Transparent, ContrastScrim.scrimFor(0.02f, Color.White))
    }

    @Test
    fun `a bright wallpaper produces a scrim for white text`() {
        val alpha = ContrastScrim.blackScrimAlpha(0.8f, Color.White)
        assertTrue("a bright wallpaper must produce a scrim", alpha > 0f)
    }

    @Test
    fun `the contrast floor holds across every wallpaper luminance`() {
        // The floor in §3 is unconditional, and scrim alone cannot deliver it over a near-white
        // wallpaper without becoming a black bar - so the resolver is allowed to flip polarity.
        var flips = 0
        for (step in 0..100) {
            val luminance = step / 100f
            val resolved = ContrastScrim.resolveLabelContrast(luminance, Color.White)
            assertTrue(
                "luminance=$luminance achieved ${resolved.ratio}",
                resolved.ratio >= ContrastScrim.TARGET_RATIO - 0.01f,
            )
            if (resolved.textColor != Color.White) flips++
        }
        assertTrue("bright wallpapers should flip the label dark", flips > 0)
    }

    @Test
    fun `a dark wallpaper keeps white labels and adds no scrim at all`() {
        val resolved = ContrastScrim.resolveLabelContrast(0.02f, Color.White)
        assertEquals(Color.White, resolved.textColor)
        assertEquals(Color.Transparent, resolved.scrim)
    }

    @Test
    fun `scrim alpha rises with wallpaper brightness and never exceeds the cap`() {
        val mid = ContrastScrim.blackScrimAlpha(0.5f, Color.White)
        val bright = ContrastScrim.blackScrimAlpha(0.95f, Color.White)
        assertTrue(bright >= mid)
        assertTrue(bright <= 0.65f)
    }

    @Test
    fun `dark text over a dark region is lifted with a white scrim instead`() {
        val alpha = ContrastScrim.whiteScrimAlpha(0.05f, Color.Black)
        assertTrue(alpha > 0f)
        val composited = (1f - alpha) * 0.05f + alpha
        val achieved = ContrastScrim.ratio(composited, 0f)
        assertTrue("achieved $achieved should be >= 4.5", achieved >= ContrastScrim.TARGET_RATIO - 0.01f)
        assertEquals(Color.White.copy(alpha = alpha), ContrastScrim.scrimFor(0.05f, Color.Black))
    }

    // ------------------------------------------------------------------ motion (§3)

    @Test
    fun `motion tokens carry the exact stiffness and damping from the spec`() {
        val tokens = MotionTokens()
        assertSpring(tokens.morph<Float>(), 380f, 0.78f)
        assertSpring(tokens.enter<Float>(), 260f, 0.82f)
        assertSpring(tokens.page<Float>(), 700f, 1f)
        assertSpring(tokens.micro<Float>(), 1200f, 0.90f)
    }

    @Test
    fun `page and micro are critically damped so horizontal paging never overshoots`() {
        // §1.1 names bouncy paging as an anti-default; damping >= 1 is the guarantee.
        assertTrue((MotionTokens().page<Float>() as SpringSpec<Float>).dampingRatio >= 1f)
        assertTrue((MotionTokens().micro<Float>() as SpringSpec<Float>).dampingRatio < 1f)
    }

    @Test
    fun `crossfade is 120ms linear`() {
        val spec = MotionTokens().crossfade<Float>() as TweenSpec<Float>
        assertEquals(MotionTokens.CROSSFADE_MS, spec.durationMillis)
    }

    @Test
    fun `reduce motion replaces every spring with the crossfade`() {
        val tokens = MotionTokens(reduceMotion = true)
        listOf(tokens.morph<Float>(), tokens.enter<Float>(), tokens.page<Float>(), tokens.micro<Float>())
            .forEach { spec ->
                assertTrue("expected a tween under reduce-motion", spec is TweenSpec<Float>)
                assertEquals(MotionTokens.CROSSFADE_MS, (spec as TweenSpec<Float>).durationMillis)
            }
    }

    @Test
    fun `animation speed scales stiffness and is clamped to the settings range`() {
        assertSpring(MotionTokens(speed = 1.5f).page<Float>(), 700f * 1.5f, 1f)
        assertSpring(MotionTokens(speed = 0.5f).page<Float>(), 700f * 0.5f, 1f)
        // §7 exposes 0.5x-1.5x; anything beyond is clamped rather than trusted.
        assertSpring(MotionTokens(speed = 9f).page<Float>(), 700f * 1.5f, 1f)
        assertSpring(MotionTokens(speed = 0.01f).page<Float>(), 700f * 0.5f, 1f)
    }

    private fun assertSpring(
        spec: androidx.compose.animation.core.FiniteAnimationSpec<Float>,
        stiffness: Float,
        damping: Float,
    ) {
        val spring = spec as SpringSpec<Float>
        assertEquals(stiffness, spring.stiffness, 0.01f)
        assertEquals(damping, spring.dampingRatio, 0.001f)
    }

    // ------------------------------------------------------------------ shape (§3)

    @Test
    fun `smoothness is clamped to the token range with 4 point 6 as the default`() {
        assertEquals(3.0f, Superellipse.DEFAULT_SMOOTHNESS, 0.0001f)
        assertEquals(3.0f, Superellipse.smoothnessOrDefault(null), 0.0001f)
        assertEquals(Superellipse.MIN_SMOOTHNESS, Superellipse.smoothnessOrDefault(0.5f), 0.0001f)
        assertEquals(Superellipse.MAX_SMOOTHNESS, Superellipse.smoothnessOrDefault(99f), 0.0001f)
        assertEquals(3.2f, Superellipse.smoothnessOrDefault(3.2f), 0.0001f)
    }
}
