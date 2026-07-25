package dev.lumen.launcher.ui.glass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.lumen.launcher.data.prefs.IconShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Geometry shared by the glass panels, icon masks and the shader.
 *
 * Corners are generated as true superellipses rather than circular arcs: `|x/r|^n + |y/r|^n = 1`.
 * `n = 2` is Material's circular corner, `n ≈ 5` is Apple's continuous "squircle" curvature. The
 * same exponent is handed to the AGSL signed-distance field, so the refraction rim lines up with
 * the clip path exactly instead of drifting apart at the corners.
 */
object LauncherShapes {

    const val CIRCULAR_EXPONENT = 2f
    const val SQUIRCLE_EXPONENT = 5f

    fun exponent(smoothing: Float): Float =
        CIRCULAR_EXPONENT + (SQUIRCLE_EXPONENT - CIRCULAR_EXPONENT) * smoothing.coerceIn(0f, 1f)

    /** Rounded rectangle whose corners follow a superellipse of the given [smoothing]. */
    fun squirclePath(
        size: Size,
        radius: Float,
        smoothing: Float,
        segmentsPerCorner: Int = 16,
    ): Path {
        val path = Path()
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return path

        val r = radius.coerceIn(0f, minOf(w, h) / 2f)
        if (r <= 0.01f) {
            path.addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
            path.close()
            return path
        }
        val n = exponent(smoothing)
        val inv = 2f / n

        // Corner centres, walked clockwise from the top-right.
        val centres = arrayOf(
            Offset(w - r, r) to 0,      // top-right     : angles  -90°..0°
            Offset(w - r, h - r) to 1,  // bottom-right  : angles    0°..90°
            Offset(r, h - r) to 2,      // bottom-left   : angles   90°..180°
            Offset(r, r) to 3,          // top-left      : angles  180°..270°
        )

        var started = false
        for ((centre, quadrant) in centres) {
            for (i in 0..segmentsPerCorner) {
                val t = i.toFloat() / segmentsPerCorner
                val angle = (-PI / 2 + quadrant * PI / 2 + t * PI / 2).toFloat()
                val c = cos(angle)
                val s = sin(angle)
                val x = centre.x + r * abs(c).pow(inv) * (if (c < 0f) -1f else 1f)
                val y = centre.y + r * abs(s).pow(inv) * (if (s < 0f) -1f else 1f)
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
        }
        path.close()
        return path
    }

    /** Polar-sampled path used for the more playful icon masks. */
    private fun polarPath(
        size: Size,
        samples: Int = 180,
        radiusAt: (Float) -> Float,
    ): Path {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = size.width / 2f
        val ry = size.height / 2f
        for (i in 0 until samples) {
            val a = (2.0 * PI * i / samples).toFloat()
            val r = radiusAt(a)
            val x = cx + cos(a) * rx * r
            val y = cy + sin(a) * ry * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    /** Mask outline for an icon shape, sized to [size]. */
    fun iconPath(
        shape: IconShape,
        size: Size,
        cornerPercent: Float,
        smoothing: Float,
    ): Path {
        val minSide = minOf(size.width, size.height)
        return when (shape) {
            IconShape.SQUIRCLE, IconShape.SYSTEM ->
                squirclePath(size, minSide * cornerPercent.coerceIn(0f, 0.5f), smoothing)

            IconShape.CIRCLE -> squirclePath(size, minSide / 2f, 0f)

            IconShape.ROUNDED_SQUARE ->
                squirclePath(size, minSide * cornerPercent.coerceIn(0f, 0.5f) * 0.6f, 0f)

            IconShape.SQUARE -> squirclePath(size, minSide * 0.06f, 0f)

            IconShape.TEARDROP -> teardropPath(size, minSide * 0.5f)

            IconShape.HEXAGON -> polarPath(size) { a ->
                val sector = PI.toFloat() / 3f
                val local = (a % sector) - sector / 2f
                val hex = cos(sector / 2f) / cos(local)
                // Nudge toward a circle so the vertices read as softly rounded.
                hex * 0.88f + 0.12f
            }

            IconShape.COOKIE -> polarPath(size) { a -> 0.93f + 0.07f * cos(12f * a) }

            IconShape.CLOVER -> polarPath(size) { a -> 0.80f + 0.20f * cos(4f * a) }

            IconShape.PEBBLE -> polarPath(size) { a ->
                0.94f + 0.06f * cos(3f * a + 0.7f) + 0.03f * sin(2f * a)
            }
        }
    }

    /** Three round corners and one tighter corner, like the Pixel teardrop mask. */
    private fun teardropPath(size: Size, radius: Float): Path {
        val path = Path()
        val w = size.width
        val h = size.height
        val r = radius.coerceAtMost(minOf(w, h) / 2f)
        val small = r * 0.28f
        path.moveTo(small, 0f)
        path.lineTo(w - r, 0f)
        path.quadraticBezierTo(w, 0f, w, r)
        path.lineTo(w, h - r)
        path.quadraticBezierTo(w, h, w - r, h)
        path.lineTo(r, h)
        path.quadraticBezierTo(0f, h, 0f, h - r)
        path.lineTo(0f, small)
        path.quadraticBezierTo(0f, 0f, small, 0f)
        path.close()
        return path
    }

    /** A [Shape] with superellipse corners of a fixed radius. */
    fun squircle(radius: Dp, smoothing: Float = 0.72f): Shape = SquircleShape(radius, smoothing)

    /** A [Shape] whose corner radius is a fraction of its shortest side. */
    fun squirclePercent(percent: Float, smoothing: Float = 0.72f): Shape =
        SquirclePercentShape(percent, smoothing)
}

private class SquircleShape(private val radius: Dp, private val smoothing: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(
            LauncherShapes.squirclePath(size, with(density) { radius.toPx() }, smoothing),
        )
}

private class SquirclePercentShape(private val percent: Float, private val smoothing: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(
            LauncherShapes.squirclePath(size, minOf(size.width, size.height) * percent, smoothing),
        )
}
