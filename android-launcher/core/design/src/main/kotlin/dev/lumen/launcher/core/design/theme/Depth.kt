package dev.lumen.launcher.core.design.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * §3's depth model: layered translucency plus **exactly one** soft shadow token, and no elevation
 * stack. Material's 2/4/8dp ladder is an anti-default (§1.1) — depth here comes from translucency
 * and a single consistent drop, so surfaces read as sheets of material rather than stacked cards.
 */
object Depth {
    /** The one shadow: `0 12dp 40dp rgba(0,0,0,.18)`. */
    val ShadowOffsetY: Dp = 12.dp
    val ShadowBlur: Dp = 40.dp
    val ShadowColor: Color = Color.Black.copy(alpha = 0.18f)

    /** §3: a 1dp inner top highlight is what stops a frosted surface reading as a flat slab. */
    val InnerHighlight: Dp = 1.dp
    val InnerHighlightColor: Color = Color.White.copy(alpha = 0.28f)

    /** §3: a 0.5dp hairline border, the other half of that trick. */
    val Hairline: Dp = 0.5.dp
    val HairlineColor: Color = Color.White.copy(alpha = 0.14f)
}

/**
 * Draws the single shadow token beneath [path].
 *
 * Compose's `Modifier.shadow` models elevation, not an offset/blur pair, so the token is drawn
 * directly: the silhouette, offset down and blurred, behind the content.
 */
fun DrawScope.drawDepthShadow(path: Path, offsetYPx: Float, blurPx: Float) {
    // A three-pass falloff approximates a Gaussian closely enough at this radius and costs no
    // render-effect layer, which keeps it usable inside a scrolling grid.
    val passes = 3
    translate(top = offsetYPx) {
        for (pass in passes downTo 1) {
            val spread = blurPx * pass / passes
            drawPath(
                path = path,
                color = Depth.ShadowColor.copy(alpha = Depth.ShadowColor.alpha / passes),
                style = Stroke(width = spread),
            )
        }
        drawPath(path, Depth.ShadowColor.copy(alpha = Depth.ShadowColor.alpha * 0.9f))
    }
}

/**
 * The inner top highlight and hairline border from §3, applied to any superellipse silhouette.
 * Both are drawn *inside* the shape so they survive clipping.
 */
fun DrawScope.drawSurfaceEdges(path: Path, highlightPx: Float, hairlinePx: Float) {
    if (highlightPx > 0f) {
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                0f to Depth.InnerHighlightColor,
                0.5f to Color.Transparent,
                startY = 0f,
                endY = size.height * 0.35f,
            ),
            style = Stroke(width = highlightPx * 2f),
        )
    }
    if (hairlinePx > 0f) {
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                0f to Depth.HairlineColor,
                1f to Depth.HairlineColor.copy(alpha = Depth.HairlineColor.alpha * 0.45f),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            style = Stroke(width = hairlinePx),
        )
    }
}

/** Convenience wrapper so callers can drop the token onto a shaped container. */
@Composable
fun DepthShadow(
    pathFor: DrawScope.() -> Path,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.drawWithCache {
            onDrawBehind {
                drawDepthShadow(
                    path = pathFor(),
                    offsetYPx = Depth.ShadowOffsetY.toPx(),
                    blurPx = Depth.ShadowBlur.toPx(),
                )
            }
        },
    ) {
        content()
    }
}
