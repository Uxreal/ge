package dev.lumen.launcher.feature.widgets

import android.appwidget.AppWidgetHostView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.shape.Superellipse

/**
 * A hosted widget in the grid: the real [AppWidgetHostView] clipped to the launcher's shape, with a
 * §6 resize handle in wiggle mode that snaps to cells. A widget whose provider vanished renders as
 * a quiet placeholder rather than an empty hole.
 */
@Composable
fun WidgetFrame(
    hostManager: WidgetHostManager,
    item: WidgetItem,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onResize: (Int, Int) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val haptics = LocalHaptics.current
    val shape = MaterialTheme.shapes.large

    // The frame is sized by the grid to span × cell, so one cell is measured size over span.
    var measured by remember(item.id) { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val cellWidthPx = if (measured.width > 0) measured.width.toFloat() / item.cell.spanX else 1f
    val cellHeightPx = if (measured.height > 0) measured.height.toFloat() / item.cell.spanY else 1f

    Box(modifier = modifier.onSizeChanged { measured = it }) {
        var failed by remember(item.appWidgetId) { mutableStateOf(false) }
        if (!failed) {
            AndroidView(
                factory = { viewContext ->
                    hostManager.createView(viewContext, item.appWidgetId)
                        ?: android.widget.FrameLayout(viewContext).also { failed = true }
                },
                update = { view ->
                    if (view is AppWidgetHostView && measured.width > 0) {
                        val widthDp = (measured.width / density.density).toInt()
                        val heightDp = (measured.height / density.density).toInt()
                        @Suppress("DEPRECATION")
                        runCatching {
                            view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val path = Superellipse.path(size, 24.dp.toPx())
                        onDrawBehind {
                            drawPath(
                                path,
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f),
                            )
                            drawPath(
                                path,
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f),
                                style = Stroke(width = 1.5f),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.text.BasicText(
                    text = item.label.ifEmpty { "Widget unavailable" },
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }

        if (editing) {
            // Selection outline plus the bottom-end resize handle, when the provider allows resize.
            val info = remember(item.appWidgetId) { hostManager.infoFor(item.appWidgetId) }
            val resizable = info == null ||
                info.resizeMode != android.appwidget.AppWidgetProviderInfo.RESIZE_NONE
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val path = Superellipse.path(size, 24.dp.toPx())
                        onDrawBehind {
                            drawPath(
                                path,
                                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect
                                        .dashPathEffect(floatArrayOf(14f, 10f)),
                                ),
                            )
                        }
                    },
            )
            if (resizable) {
                var accumulated by remember { mutableStateOf(Offset.Zero) }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(44.dp)
                        .pointerInput(item.id, cellWidthPx, cellHeightPx) {
                            detectDragGestures(
                                onDragStart = { accumulated = Offset.Zero },
                                onDrag = { change, delta ->
                                    change.consume()
                                    accumulated += delta
                                    val dx = (accumulated.x / cellWidthPx).toInt()
                                    val dy = (accumulated.y / cellHeightPx).toInt()
                                    if (dx != 0 || dy != 0) {
                                        haptics.tick()
                                        onResize(item.cell.spanX + dx, item.cell.spanY + dy)
                                        accumulated = Offset.Zero
                                    }
                                },
                                onDragEnd = { haptics.snap() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .drawWithCache {
                                val dot = Superellipse.path(size, size.minDimension / 2f, 2f)
                                onDrawBehind {
                                    drawPath(dot, androidx.compose.ui.graphics.Color.White)
                                    drawPath(
                                        dot,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                                        style = Stroke(width = 2f),
                                    )
                                }
                            },
                    )
                }
            }
        }
    }
}
