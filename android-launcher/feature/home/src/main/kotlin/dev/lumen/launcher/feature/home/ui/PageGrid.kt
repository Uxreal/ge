package dev.lumen.launcher.feature.home.ui

import android.graphics.Rect
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.GridItem
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.model.ShortcutItem
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.design.icon.AppIcon
import dev.lumen.launcher.core.design.icon.LetterTile
import dev.lumen.launcher.core.design.icon.MaskedIcon
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.home.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * One home page. Items are absolutely positioned from their cells; every position change animates
 * with the `page` token, which is what makes the PACKED live-reflow read as icons making room.
 */
@Composable
internal fun PageGrid(
    vm: HomeViewModel,
    homeState: HomeState,
    items: List<GridItem>,
    page: Int,
    metrics: GridMetrics,
    pagerState: PagerState,
    onReleaseWidget: (Int) -> Unit,
    widgetContent: @Composable (WidgetItem, Modifier, Boolean, (Int, Int) -> Unit) -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        items.forEach { item ->
            key(item.id) {
                val topLeft = metrics.cellTopLeft(item.cell)
                val target = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
                val animated by animateIntOffsetAsState(
                    targetValue = target,
                    animationSpec = LocalMotion.current.page(),
                    label = "cell",
                )
                val cellModifier = Modifier
                    .offset { animated }
                    .size(
                        width = metrics.geometry.cellWidth * item.cell.spanX,
                        height = metrics.geometry.cellHeight * item.cell.spanY,
                    )

                when (item) {
                    is WidgetItem -> Box(cellModifier) {
                        widgetContent(
                            item,
                            Modifier
                                .fillMaxSize()
                                .dragSource(vm, homeState, item, page, metrics),
                            homeState.editMode,
                        ) { sx, sy -> vm.resizeWidget(item.id, sx, sy) }
                        if (homeState.editMode) {
                            RemoveBadge {
                                vm.removeWidget(item.id) { widgetId -> onReleaseWidget(widgetId) }
                            }
                        }
                    }

                    else -> Box(cellModifier, contentAlignment = Alignment.TopCenter) {
                        GridCellItem(vm, homeState, item, page, metrics)
                    }
                }
            }
        }
    }
    // Keep density referenced for future per-page scaling without a warning.
    remember(density) { density }
}

@Composable
private fun key(key: Any, content: @Composable () -> Unit) =
    androidx.compose.runtime.key(key) { content() }

/** An app, folder or shortcut in its cell, with tap, drag source, wiggle, and the edit-mode X. */
@Composable
private fun GridCellItem(
    vm: HomeViewModel,
    homeState: HomeState,
    item: GridItem,
    page: Int,
    metrics: GridMetrics,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val wigglePhase = rememberWigglePhase(homeState.editMode)
    val session = homeState.drag
    val isCombineTarget = session?.combineTargetId == item.id
    val combineScale by animateFloatAsState(
        targetValue = if (isCombineTarget) 1.18f else 1f,
        animationSpec = LocalMotion.current.micro(),
        label = "combineDilate",
    )

    Box(
        modifier = Modifier.scale(combineScale),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (item) {
            is AppItem -> {
                val label = vm.appLabel(item.key)
                val icon = rememberAppIcon(vm.iconCache, item.key, metrics.geometry.iconSize)
                val luminance = vm.wallpaper.luminanceAt(0.5f, 0.8f)
                var bounds by remember { mutableStateOf(Rect()) }
                AppIcon(
                    icon = icon,
                    label = label,
                    iconSize = metrics.geometry.iconSize,
                    labelGap = metrics.geometry.labelGap,
                    showLabel = !prefs.hideLabels,
                    labelLines = prefs.labelLines,
                    backgroundLuminance = luminance,
                    smoothness = prefs.smoothness,
                    wiggle = homeState.editMode,
                    wigglePhase = wigglePhase,
                    interactive = false,
                    accessibilityActions = appAccessibilityActions(vm, homeState, item, page),
                    onClick = { vm.launch(item.key, it) },
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val position = coords.positionInRoot()
                            bounds = Rect(
                                position.x.roundToInt(),
                                position.y.roundToInt(),
                                (position.x + coords.size.width).roundToInt(),
                                (position.y + coords.size.height).roundToInt(),
                            )
                        }
                        .tapSource { vm.launch(item.key, bounds) }
                        .dragSource(vm, homeState, item, page, metrics),
                )
                if (homeState.editMode && vm.currentModel() == HomeModel.FREEFORM) {
                    RemoveBadge { vm.removeItem(item.id) }
                } else if (homeState.editMode && vm.canUninstall(item.key)) {
                    RemoveBadge { vm.requestUninstall(item.key) }
                }
            }

            is FolderItem -> {
                FolderIcon(
                    vm = vm,
                    folder = item,
                    iconSize = metrics.geometry.iconSize,
                    showLabel = !prefs.hideLabels,
                    wiggle = homeState.editMode,
                    wigglePhase = wigglePhase,
                    modifier = Modifier
                        .tapSource { homeState.openFolderId = item.id }
                        .dragSource(vm, homeState, item, page, metrics),
                )
                if (homeState.editMode && vm.currentModel() == HomeModel.FREEFORM) {
                    RemoveBadge { vm.removeItem(item.id) }
                }
            }

            is ShortcutItem -> {
                // No Phase 1 flow creates these; render defensively so a restored layout never crashes.
                LetterTile(label = item.label, size = metrics.geometry.iconSize)
            }

            is WidgetItem -> Unit // handled by the caller
        }
    }
}

/** Tap → launch/open. Separate from the drag detector so a quick tap never waits for the lift. */
private fun Modifier.tapSource(onTap: () -> Unit): Modifier =
    pointerInput(onTap) { detectTapGestures(onTap = { onTap() }) }

/**
 * The §5 drag source. Outside wiggle mode the lift needs the 280ms long-press (the pager owns plain
 * horizontal motion); inside wiggle mode items lift on first movement.
 */
private fun Modifier.dragSource(
    vm: HomeViewModel,
    homeState: HomeState,
    item: GridItem,
    page: Int,
    metrics: GridMetrics,
): Modifier = composed {
    val haptics = LocalHaptics.current
    val motion = LocalMotion.current
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf(Offset.Zero) }

    val begin: (Offset) -> Unit = { pointerLocal ->
        if (homeState.drag == null) {
            haptics.lift()
            homeState.editMode = true
            homeState.drag = DragSession(
                item = item,
                sourcePage = page,
                grabOffset = pointerLocal,
                start = origin,
            )
        }
    }
    val move: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit = { change, delta ->
        change.consume()
        homeState.drag?.takeIf { it.item.id == item.id && !it.dropping }?.let { session ->
            scope.launch { session.position.snapTo(session.position.value + delta) }
            session.velocity.addPosition(change.uptimeMillis, session.position.value)
            updateHover(vm, homeState, session, metrics)
        }
    }
    val end: () -> Unit = {
        homeState.drag?.takeIf { it.item.id == item.id && !it.dropping }?.let { session ->
            commitDrop(vm, homeState, session, metrics, scope, haptics, motion)
        }
    }

    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .pointerInput(item.id, homeState.editMode) {
            if (homeState.editMode) {
                detectDragGestures(
                    onDragStart = begin,
                    onDrag = move,
                    onDragEnd = end,
                    onDragCancel = end,
                )
            } else {
                detectDragGesturesAfterLongPress(
                    onDragStart = begin,
                    onDrag = move,
                    onDragEnd = end,
                    onDragCancel = end,
                )
            }
        }
}

/** §5: X badges hit at 48dp even though the visual is 20dp. */
@Composable
private fun RemoveBadge(onRemove: () -> Unit) {
    val haptics = LocalHaptics.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(onRemove) {
                detectTapGestures(onTap = {
                    haptics.commit()
                    onRemove()
                })
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier
                .offset(6.dp, 6.dp)
                .size(20.dp)
                .drawWithCache {
                    val circle = Superellipse.path(size, size.minDimension / 2f, 2f)
                    onDrawBehind {
                        drawPath(circle, Color(0xE6303034))
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        val inset = size.width * 0.30f
                        drawLine(
                            Color.White,
                            Offset(inset, inset),
                            Offset(size.width - inset, size.height - inset),
                            strokeWidth = stroke.width,
                        )
                        drawLine(
                            Color.White,
                            Offset(size.width - inset, inset),
                            Offset(inset, size.height - inset),
                            strokeWidth = stroke.width,
                        )
                    }
                },
        )
    }
}

/** §9: the non-drag equivalents, so Switch Access and keyboard users can rearrange. */
private fun appAccessibilityActions(
    vm: HomeViewModel,
    homeState: HomeState,
    item: AppItem,
    page: Int,
): List<CustomAccessibilityAction> = buildList {
    add(
        CustomAccessibilityAction("Move to next page") {
            vm.move(item.id, page + 1, item.cell)
            true
        },
    )
    if (page > 0) {
        add(
            CustomAccessibilityAction("Move to previous page") {
                vm.move(item.id, page - 1, item.cell)
                true
            },
        )
    }
    add(
        CustomAccessibilityAction("App info") {
            vm.appInfo(item.key, null)
            true
        },
    )
    if (vm.canUninstall(item.key)) {
        add(
            CustomAccessibilityAction("Uninstall") {
                vm.requestUninstall(item.key)
                true
            },
        )
    }
    if (vm.currentModel() == HomeModel.FREEFORM) {
        add(
            CustomAccessibilityAction("Remove from home") {
                vm.removeItem(item.id)
                true
            },
        )
    }
}

/** Folder tile: a superellipse well with a 2×2 mini-grid of its first four icons. */
@Composable
internal fun FolderIcon(
    vm: HomeViewModel,
    folder: FolderItem,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    wiggle: Boolean = false,
    wigglePhase: Float = 0f,
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val typography = LocalTypography.current

    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .wiggleRotation(wiggle, wigglePhase, folder.id)
                .drawWithCache {
                    val path = Superellipse.path(
                        size = size,
                        radius = size.minDimension * dev.lumen.launcher.core.design.icon.DEFAULT_MASK_PERCENT,
                        smoothness = prefs.smoothness,
                    )
                    onDrawBehind { drawPath(path, container) }
                },
            contentAlignment = Alignment.Center,
        ) {
            val mini = iconSize * 0.36f
            val pad = iconSize * 0.06f
            folder.items.take(4).forEachIndexed { index, child ->
                val icon = rememberAppIcon(vm.iconCache, child.key, mini)
                MaskedIcon(
                    icon = icon,
                    size = mini,
                    smoothness = prefs.smoothness,
                    modifier = Modifier
                        .align(if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .offset(
                            x = if (index % 2 == 0) pad else -pad,
                            y = if (index < 2) -(mini / 2 + pad / 2) else (mini / 2 + pad / 2),
                        ),
                )
            }
        }
        if (showLabel) {
            androidx.compose.foundation.text.BasicText(
                text = folder.label.ifEmpty { "Folder" },
                style = typography.iconLabel.copy(
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                ),
                maxLines = 1,
                modifier = Modifier.width(iconSize * 1.7f),
            )
        }
    }
}

private fun Modifier.wiggleRotation(enabled: Boolean, phase: Float, seed: String): Modifier =
    if (!enabled) {
        this
    } else {
        val offset = (seed.hashCode() and 0xFF) / 255f
        rotate(kotlin.math.sin((phase + offset) * 2f * Math.PI.toFloat()) * 1.5f)
    }

/** §5: ±1.5° at ~0.9Hz. One shared clock; each icon offsets its phase by identity. */
@Composable
internal fun rememberWigglePhase(active: Boolean): Float {
    if (!active) return 0f
    val transition = rememberInfiniteTransition(label = "wiggle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1111, easing = LinearEasing), // ~0.9Hz
        ),
        label = "wigglePhase",
    )
    return phase
}
