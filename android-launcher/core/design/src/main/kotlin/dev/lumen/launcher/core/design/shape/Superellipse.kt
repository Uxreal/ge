package dev.lumen.launcher.core.design.shape

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * §3's shape: a superellipse, `|x/a|^n + |y/b|^n = 1`, not a rounded rectangle.
 *
 * `n` is exposed as the **smoothness** token — [MIN_SMOOTHNESS] is circle-ish, [MAX_SMOOTHNESS] is
 * near-square, and [DEFAULT_SMOOTHNESS] is the launcher's default. A circular corner arc (`n = 2`)
 * meets the straight edge with a visible curvature break; higher `n` blends the two, which is the
 * whole reason the shape reads as considered rather than as a default rounded rect.
 *
 * Paths are cached per (size, radii, n) — §3 requires building the path once per size, since this
 * geometry is evaluated for every icon on every page.
 */
object Superellipse {

    const val MIN_SMOOTHNESS = 2.0f
    const val MAX_SMOOTHNESS = 6.0f
    const val DEFAULT_SMOOTHNESS = 4.6f

    /** Segments per corner. 16 is indistinguishable from smooth at icon sizes and above. */
    private const val SEGMENTS = 16
    private const val CACHE_LIMIT = 128

    private data class Key(
        val width: Int,
        val height: Int,
        val topLeft: Int,
        val topRight: Int,
        val bottomRight: Int,
        val bottomLeft: Int,
        val smoothness: Int,
    )

    private val cache = object : LinkedHashMap<Key, Path>(CACHE_LIMIT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Path>?): Boolean =
            size > CACHE_LIMIT
    }

    fun smoothnessOrDefault(value: Float?): Float =
        (value ?: DEFAULT_SMOOTHNESS).coerceIn(MIN_SMOOTHNESS, MAX_SMOOTHNESS)

    /** Uniform-radius superellipse. */
    fun path(size: Size, radius: Float, smoothness: Float = DEFAULT_SMOOTHNESS): Path =
        path(size, radius, radius, radius, radius, smoothness)

    /**
     * Per-corner variant, so the Material shape scale (sheets with only the top corners rounded, and
     * so on) shares one curvature definition with the launcher's own surfaces.
     */
    fun path(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
        smoothness: Float = DEFAULT_SMOOTHNESS,
    ): Path {
        if (size.width <= 0f || size.height <= 0f) return Path()

        val key = Key(
            width = size.width.roundToInt(),
            height = size.height.roundToInt(),
            topLeft = topLeft.roundToInt(),
            topRight = topRight.roundToInt(),
            bottomRight = bottomRight.roundToInt(),
            bottomLeft = bottomLeft.roundToInt(),
            smoothness = (smoothness * 10f).roundToInt(),
        )
        cache[key]?.let { return it }

        val built = build(size, topLeft, topRight, bottomRight, bottomLeft, smoothness)
        cache[key] = built
        return built
    }

    private fun build(
        size: Size,
        topLeft: Float,
        topRight: Float,
        bottomRight: Float,
        bottomLeft: Float,
        smoothness: Float,
    ): Path {
        val path = Path()
        val w = size.width
        val h = size.height
        val limit = minOf(w, h) / 2f

        val tl = topLeft.coerceIn(0f, limit)
        val tr = topRight.coerceIn(0f, limit)
        val br = bottomRight.coerceIn(0f, limit)
        val bl = bottomLeft.coerceIn(0f, limit)

        if (tl <= 0.01f && tr <= 0.01f && br <= 0.01f && bl <= 0.01f) {
            path.addRect(Rect(0f, 0f, w, h))
            path.close()
            return path
        }

        val n = smoothness.coerceIn(MIN_SMOOTHNESS, MAX_SMOOTHNESS)
        val exponent = 2f / n

        // Corner centre, radius, and quadrant index, walked clockwise from the top-right.
        val corners = listOf(
            Triple(Offset(w - tr, tr), tr, 0),
            Triple(Offset(w - br, h - br), br, 1),
            Triple(Offset(bl, h - bl), bl, 2),
            Triple(Offset(tl, tl), tl, 3),
        )

        var started = false
        for ((centre, r, quadrant) in corners) {
            for (i in 0..SEGMENTS) {
                val t = i.toFloat() / SEGMENTS
                val angle = (-PI / 2 + quadrant * PI / 2 + t * PI / 2).toFloat()
                val c = cos(angle)
                val s = sin(angle)
                // x = r·|cos θ|^(2/n)·sgn, y likewise: substituting gives |x/r|^n + |y/r|^n = 1.
                val x = centre.x + r * abs(c).pow(exponent) * (if (c < 0f) -1f else 1f)
                val y = centre.y + r * abs(s).pow(exponent) * (if (s < 0f) -1f else 1f)
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

    /** A shape with a fixed corner radius. */
    fun shape(radius: Dp, smoothness: Float = DEFAULT_SMOOTHNESS): CornerBasedShape =
        SuperellipseShape(CornerSize(radius), smoothness)

    /** A shape whose radius is a fraction of its shortest side — used for icon masks. */
    fun percentShape(percent: Float, smoothness: Float = DEFAULT_SMOOTHNESS): CornerBasedShape =
        SuperellipseShape(CornerSize((percent.coerceIn(0f, 0.5f) * 100f).roundToInt()), smoothness)
}

/**
 * A [CornerBasedShape] rather than a bare `Shape`, so it can be handed to `MaterialTheme.shapes`:
 * every Material 3 component in the launcher then inherits the same curvature, and per-corner
 * overrides keep working.
 */
class SuperellipseShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
    private val smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(all: CornerSize, smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS) :
        this(all, all, all, all, smoothness)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val ltr = layoutDirection == LayoutDirection.Ltr
        return Outline.Generic(
            Superellipse.path(
                size = size,
                topLeft = if (ltr) topStart else topEnd,
                topRight = if (ltr) topEnd else topStart,
                bottomRight = if (ltr) bottomEnd else bottomStart,
                bottomLeft = if (ltr) bottomStart else bottomEnd,
                smoothness = smoothness,
            ),
        )
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): SuperellipseShape = SuperellipseShape(topStart, topEnd, bottomEnd, bottomStart, smoothness)

    override fun toString(): String = "SuperellipseShape(n=$smoothness)"
}
