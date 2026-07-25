package dev.lumen.launcher.ui.drag

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.ui.common.IconBitmap
import dev.lumen.launcher.ui.glass.LauncherShapes
import kotlin.math.roundToInt

/**
 * The thing under the finger during a drag. Lives at the very top of the composition — above every
 * overlay — so an icon dragged out of the app drawer keeps following the finger while the drawer
 * itself slides away underneath.
 */
@Composable
fun DragOverlay(modifier: Modifier = Modifier) {
    val drag = LocalDragState.current
    val session = drag.session ?: return
    val density = LocalDensity.current
    val previewSize = with(density) { session.previewSizePx.coerceAtLeast(1f).toDp() }
    val pointer = drag.pointer

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (pointer.x - session.grabOffset.x).roundToInt(),
                        y = (pointer.y - session.grabOffset.y).roundToInt(),
                    )
                }
                // Lifted off the surface: slightly larger, slightly transparent.
                .scale(1.14f)
                .alpha(0.92f),
        ) {
            when (val payload = session.payload) {
                is DragPayload.App -> IconBitmap(key = payload.key, size = previewSize)

                is DragPayload.Shortcut,
                is DragPayload.Folder,
                is DragPayload.Widget,
                -> Box(
                    modifier = Modifier
                        .size(
                            width = previewSize * session.spanX,
                            height = previewSize * session.spanY,
                        )
                        .drawBehind {
                            val path = LauncherShapes.squirclePath(
                                size = size,
                                radius = minOf(size.width, size.height) * 0.22f,
                                smoothing = 0.72f,
                            )
                            drawPath(path, Color.White.copy(alpha = 0.22f))
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.42f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                            )
                        },
                )
            }
        }
    }
}
