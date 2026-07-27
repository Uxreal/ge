package dev.lumen.launcher.feature.capsule

import dev.lumen.launcher.feature.capsule.ui.CapsuleGeometry
import dev.lumen.launcher.feature.capsule.ui.CutoutRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The docking policy, pinned against real hardware shapes. The first emulator run showed why this
 * cannot live as inline heuristics: a wide emulated notch passed a centred-only check and the pill
 * "embraced" it into a full-width banner.
 */
class CapsuleGeometryTest {

    // A 1080px-wide phone at ~2.6x density, Z Flip-style: a 66px hole centred at x=540, y=50.
    private val screenWidth = 1080
    private val statusBar = 82

    private fun resolve(cutout: CutoutRect?) = CapsuleGeometry.resolve(
        cutout = cutout,
        screenWidthPx = screenWidth,
        statusBarPx = statusBar,
        holeMarginPx = 13f,
        holeBreathPx = 18f,
        fallbackHeightPx = 84f,
        fallbackGapPx = 26f,
        fallbackTopGapPx = 10,
        minTopPx = 5,
        maxHolePx = 115,
        holeCapPx = holeCap,
    )

    private var holeCap: Int = Int.MAX_VALUE

    @Test
    fun `a centred punch-hole is embraced`() {
        val geometry = resolve(CutoutRect(left = 507, top = 17, right = 573, bottom = 83))

        assertTrue(geometry.embraced)
        // Hole height 66 + 13 above + 13 below.
        assertEquals(92f, geometry.heightPx)
        // Hole centre y=50, pill half-height 46 → top at 4, clamped to the 5px minimum.
        assertEquals(5, geometry.topPx)
        // Hole is dead centre, no shift.
        assertEquals(0, geometry.xOffsetPx)
        // Hole width 66 + 18 breathing each side.
        assertEquals(102f, geometry.gapPx)
    }

    @Test
    fun `a slightly off-centre hole shifts the pill onto it`() {
        val geometry = resolve(CutoutRect(left = 527, top = 17, right = 593, bottom = 83))

        assertTrue(geometry.embraced)
        assertEquals(20, geometry.xOffsetPx)
    }

    @Test
    fun `a wide notch is not embraced`() {
        // The emulator's emu01: centred but a third of the screen wide. Embracing it produced a
        // full-width black banner on the first verification run.
        val geometry = resolve(CutoutRect(left = 390, top = 0, right = 690, bottom = 100))

        assertFalse(geometry.embraced)
        assertEquals(statusBar + 10, geometry.topPx)
        assertEquals(84f, geometry.heightPx)
    }

    @Test
    fun `a corner hole is not embraced`() {
        // Pixel-5 style: small, but hugging the left edge.
        val geometry = resolve(CutoutRect(left = 20, top = 20, right = 86, bottom = 86))

        assertFalse(geometry.embraced)
    }

    @Test
    fun `a tall cutout is not embraced even when narrow and centred`() {
        val geometry = resolve(CutoutRect(left = 500, top = 0, right = 580, bottom = 200))

        assertFalse(geometry.embraced)
    }

    @Test
    fun `no cutout falls back below the status bar`() {
        val geometry = resolve(null)

        assertFalse(geometry.embraced)
        assertEquals(statusBar + 10, geometry.topPx)
        assertEquals(0, geometry.xOffsetPx)
        assertEquals(26f, geometry.gapPx)
    }

    @Test
    fun `an empty rect falls back`() {
        assertFalse(resolve(CutoutRect(540, 0, 540, 0)).embraced)
    }

    @Test
    fun `a generously reported round hole is tightened to the cap`() {
        // OEMs pad the reported rect around the visual lens; the pill should hug the lens.
        holeCap = 40
        val geometry = resolve(CutoutRect(left = 500, top = 10, right = 580, bottom = 90))

        assertTrue(geometry.embraced)
        // Effective hole 40 + 13 margin each side, centred on the reported centre (y=50).
        assertEquals(66f, geometry.heightPx)
        assertEquals(17, geometry.topPx)
        assertEquals(76f, geometry.gapPx)
    }

    @Test
    fun `a wide dual-lens cutout is not tightened`() {
        holeCap = 40
        // Aspect 2.5: clearly not a circle; the cap would clip real camera glass.
        val geometry = resolve(CutoutRect(left = 465, top = 20, right = 615, bottom = 80))

        assertTrue(geometry.embraced)
        assertEquals(60 + 26f, geometry.heightPx)
        assertEquals(150 + 36f, geometry.gapPx)
    }
}
