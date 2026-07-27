package dev.lumen.launcher.feature.capsule.ui

/** A display cutout bounding rect in window pixels. Plain ints so the policy is unit-testable. */
data class CutoutRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

/** Where the pill sits and how it is shaped, resolved once per attach. */
data class PillGeometry(
    val embraced: Boolean,
    val heightPx: Float,
    /** Window-Y of the pill's top edge. */
    val topPx: Int,
    /** Horizontal shift from screen centre, so the pill centres on the hole. */
    val xOffsetPx: Int,
    /** The central gap the header flows around: the camera plus breathing room, or a token gap. */
    val gapPx: Float,
)

/**
 * The docking policy, kept pure because it is exactly the kind of code that looks right and is
 * wrong. First verification run proved it: an emulator's wide notch was "embraced" into a
 * full-width black banner, because the only test was "is it centred".
 *
 * A cutout is worth embracing when it is a **punch-hole**: small (under a fifth of the screen
 * wide, under [maxHolePx] tall) and roughly centred. Anything else — wide notches, corner holes,
 * waterfall edges, no cutout at all — gets the fallback pill below the status bar. Swallowing a
 * camera the size of a fingertip is the trick; swallowing a notch the width of the screen is a
 * banner ad.
 */
object CapsuleGeometry {

    fun resolve(
        cutout: CutoutRect?,
        screenWidthPx: Int,
        statusBarPx: Int,
        holeMarginPx: Float,
        holeBreathPx: Float,
        fallbackHeightPx: Float,
        fallbackGapPx: Float,
        fallbackTopGapPx: Int,
        minTopPx: Int,
        maxHolePx: Int,
        /**
         * OEMs report the cutout rect with safety padding around the visual lens (the field
         * report from a real punch-hole: "still very large"). A roughly circular hole larger
         * than this is assumed to be padding and tightened to it, centred on the same point; a
         * clearly non-circular cutout (dual-lens pill) is taken at its reported word.
         */
        holeCapPx: Int = Int.MAX_VALUE,
    ): PillGeometry {
        if (cutout != null && !cutout.isEmpty && screenWidthPx > 0 &&
            isPunchHole(cutout, screenWidthPx, maxHolePx)
        ) {
            val aspect = cutout.width.toFloat() / cutout.height.toFloat()
            val roundish = aspect in 0.6f..1.5f
            val holeW = if (roundish) minOf(cutout.width, holeCapPx) else cutout.width
            val holeH = if (roundish) minOf(cutout.height, holeCapPx) else cutout.height
            val height = holeH + 2 * holeMarginPx
            return PillGeometry(
                embraced = true,
                heightPx = height,
                topPx = (cutout.centerY - height / 2f).toInt().coerceAtLeast(minTopPx),
                xOffsetPx = (cutout.centerX - screenWidthPx / 2f).toInt(),
                gapPx = holeW + 2 * holeBreathPx,
            )
        }
        return PillGeometry(
            embraced = false,
            heightPx = fallbackHeightPx,
            topPx = statusBarPx + fallbackTopGapPx,
            xOffsetPx = 0,
            gapPx = fallbackGapPx,
        )
    }

    private fun isPunchHole(cutout: CutoutRect, screenWidthPx: Int, maxHolePx: Int): Boolean {
        val small = cutout.width < screenWidthPx / 5 && cutout.height <= maxHolePx
        val centred = kotlin.math.abs(cutout.centerX - screenWidthPx / 2f) < screenWidthPx / 6f
        return small && centred
    }
}
