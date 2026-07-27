package dev.lumen.launcher.feature.capsule.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import kotlin.math.cos
import kotlin.math.sin

/**
 * Everything the Capsule draws itself.
 *
 * The launcher ships no icon font and no vector assets for these — five glyphs drawn from
 * primitives cost nothing to decode, scale to any density without a raster step, and inherit the
 * text colour so they stay legible against whatever the contrast solver picked.
 */
internal fun DrawScope.drawBuiltinSymbol(symbol: BuiltinSymbol, color: Color, boxSize: Float) {
    val r = boxSize / 2f
    val center = Offset(r, r)
    val stroke = (boxSize * 0.09f).coerceAtLeast(1f)

    when (symbol) {
        BuiltinSymbol.CLOCK -> {
            drawCircle(color, radius = r - stroke / 2f, center = center, style = Stroke(stroke))
            drawLine(color, center, center + Offset(0f, -r * 0.52f), stroke, StrokeCap.Round)
            drawLine(color, center, center + Offset(r * 0.38f, 0f), stroke, StrokeCap.Round)
        }

        BuiltinSymbol.BATTERY, BuiltinSymbol.CHARGING -> {
            val bodyW = boxSize * 0.78f
            val bodyH = boxSize * 0.46f
            val left = (boxSize - bodyW) / 2f
            val top = (boxSize - bodyH) / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(bodyW - boxSize * 0.10f, bodyH),
                cornerRadius = CornerRadius(boxSize * 0.10f),
                style = Stroke(stroke),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(left + bodyW - boxSize * 0.07f, top + bodyH * 0.30f),
                size = Size(boxSize * 0.06f, bodyH * 0.40f),
                cornerRadius = CornerRadius(boxSize * 0.03f),
            )
            if (symbol == BuiltinSymbol.CHARGING) {
                val bolt = Path().apply {
                    moveTo(center.x + boxSize * 0.05f, top + bodyH * 0.14f)
                    lineTo(center.x - boxSize * 0.09f, center.y + bodyH * 0.06f)
                    lineTo(center.x - boxSize * 0.01f, center.y + bodyH * 0.06f)
                    lineTo(center.x - boxSize * 0.07f, top + bodyH * 0.90f)
                    lineTo(center.x + boxSize * 0.10f, center.y - bodyH * 0.04f)
                    lineTo(center.x + boxSize * 0.01f, center.y - bodyH * 0.04f)
                    close()
                }
                drawPath(bolt, color)
            }
        }

        BuiltinSymbol.ALARM -> {
            val dialR = r * 0.72f
            drawCircle(color, dialR, center, style = Stroke(stroke))
            drawLine(color, center, center + Offset(0f, -dialR * 0.55f), stroke, StrokeCap.Round)
            drawLine(color, center, center + Offset(dialR * 0.40f, 0f), stroke, StrokeCap.Round)
            // The two bells, as short arcs springing off the top corners of the dial.
            listOf(-1f, 1f).forEach { side ->
                val angle = Math.toRadians(side * 45.0)
                val from = center + Offset(
                    (sin(angle) * dialR).toFloat(),
                    (-cos(angle) * dialR).toFloat(),
                )
                drawLine(color, from, from + Offset(side * r * 0.24f, -r * 0.24f), stroke, StrokeCap.Round)
            }
        }

        BuiltinSymbol.NOTE -> {
            // An eighth note: stem, flag, and a head sitting low-left.
            val headR = boxSize * 0.16f
            val headC = Offset(center.x - boxSize * 0.14f, boxSize * 0.72f)
            val stemTop = boxSize * 0.14f
            drawCircle(color, headR, headC)
            drawLine(
                color,
                Offset(headC.x + headR * 0.9f, headC.y),
                Offset(headC.x + headR * 0.9f, stemTop),
                stroke,
                StrokeCap.Round,
            )
            val flag = Path().apply {
                moveTo(headC.x + headR * 0.9f, stemTop)
                quadraticTo(
                    center.x + boxSize * 0.30f, stemTop + boxSize * 0.10f,
                    center.x + boxSize * 0.22f, stemTop + boxSize * 0.34f,
                )
            }
            drawPath(flag, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }

        BuiltinSymbol.PLAY -> {
            val s = boxSize * 0.34f
            val triangle = Path().apply {
                moveTo(center.x - s * 0.7f, center.y - s)
                lineTo(center.x + s, center.y)
                lineTo(center.x - s * 0.7f, center.y + s)
                close()
            }
            drawPath(triangle, color)
        }

        BuiltinSymbol.PAUSE -> {
            val barW = boxSize * 0.16f
            val barH = boxSize * 0.56f
            listOf(-1f, 1f).forEach { side ->
                drawRoundRect(
                    color = color,
                    topLeft = Offset(center.x + side * boxSize * 0.16f - barW / 2f, center.y - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f),
                )
            }
        }

        BuiltinSymbol.NEXT -> {
            val s = boxSize * 0.26f
            listOf(-0.22f, 0.24f).forEach { shift ->
                val triangle = Path().apply {
                    moveTo(center.x + boxSize * shift - s * 0.6f, center.y - s)
                    lineTo(center.x + boxSize * shift + s * 0.8f, center.y)
                    lineTo(center.x + boxSize * shift - s * 0.6f, center.y + s)
                    close()
                }
                drawPath(triangle, color)
            }
        }

        BuiltinSymbol.PREV -> {
            val s = boxSize * 0.26f
            listOf(0.22f, -0.24f).forEach { shift ->
                val triangle = Path().apply {
                    moveTo(center.x + boxSize * shift + s * 0.6f, center.y - s)
                    lineTo(center.x + boxSize * shift - s * 0.8f, center.y)
                    lineTo(center.x + boxSize * shift + s * 0.6f, center.y + s)
                    close()
                }
                drawPath(triangle, color)
            }
        }

        BuiltinSymbol.SPARK -> {
            // A four-point star with concave sides — the mark for "something pushed this".
            val outer = r * 0.92f
            val inner = r * 0.30f
            val star = Path()
            for (i in 0 until 8) {
                val radius = if (i % 2 == 0) outer else inner
                val angle = Math.toRadians(i * 45.0 - 90.0)
                val point = center + Offset(
                    (cos(angle) * radius).toFloat(),
                    (sin(angle) * radius).toFloat(),
                )
                if (i == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
            }
            star.close()
            drawPath(star, color)
        }
    }
}

/**
 * The Capsule's signature: **its own outline is the progress indicator.**
 *
 * Rather than adding a bar — one more rectangle inside a shape the whole design exists to avoid —
 * a segment of the superellipse silhouette is stroked at full accent while the rest sits at a low
 * alpha. Determinate progress traces clockwise from top-centre; indeterminate sends a short comet
 * round the rim.
 *
 * [fraction] is 0f..1f for determinate. [comet] is the head position for indeterminate, or null.
 */
internal fun DrawScope.drawRimProgress(
    path: Path,
    cornerPx: Float,
    fraction: Float?,
    comet: Float?,
    color: Color,
    strokeWidth: Float,
) {
    val measure = PathMeasure().apply { setPath(path, forceClosed = true) }
    val length = measure.length
    if (length <= 0f) return

    // `Superellipse` starts its outline where the top edge meets the top-right corner and walks
    // clockwise, so top-centre is that much of the top edge *before* the start.
    val originOffset = (length - (size.width / 2f - cornerPx)).coerceIn(0f, length)
    val style = Stroke(width = strokeWidth, cap = StrokeCap.Round)

    drawPath(path, color.copy(alpha = 0.16f), style = Stroke(width = strokeWidth))

    fun segment(startFraction: Float, sweepFraction: Float) {
        if (sweepFraction <= 0f) return
        val start = (originOffset + startFraction * length) % length
        val end = start + sweepFraction * length
        val destination = Path()
        if (end <= length) {
            measure.getSegment(start, end, destination, true)
            drawPath(destination, color, style = style)
        } else {
            measure.getSegment(start, length, destination, true)
            drawPath(destination, color, style = style)
            val wrapped = Path()
            measure.getSegment(0f, end - length, wrapped, true)
            drawPath(wrapped, color, style = style)
        }
    }

    when {
        fraction != null -> segment(0f, fraction.coerceIn(0f, 1f))
        comet != null -> segment(comet % 1f, COMET_SWEEP)
    }
}

private const val COMET_SWEEP = 0.22f
