package dev.lumen.launcher.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.core.data.model.AppItem
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.design.grid.GridGeometry
import dev.lumen.launcher.core.design.icon.LetterTile
import dev.lumen.launcher.core.design.icon.MaskedIcon
import dev.lumen.launcher.core.design.interaction.Haptics
import dev.lumen.launcher.feature.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hover resolution for an in-flight drag: which cell, which item, which edge. Called on every drag
 * frame; the dwell timers in [DragEffects] act on what this writes.
 */
internal fun updateHover(
    vm: HomeViewModel,
    homeState: HomeState,
    session: DragSession,
    metrics: GridMetrics,
) {
    val pointer = session.position.value + session.grabOffset
    session.edgeDirection = when {
        pointer.x < metrics.edgeZone -> -1
        pointer.x > metrics.containerWidth - metrics.edgeZone -> 1
        else -> 0
    }

    val cell = metrics.cellAt(pointer)
    session.hoverCell = cell
    if (cell == null) {
        session.hoverItemId = null
        return
    }

    val state = vm.state.value
    val under = state.itemsOn(session.hoverPage)
        .firstOrNull { it.id != session.item.id && it.cell.overlaps(cell) }

    // Combine only makes sense onto apps and folders, and only for a dragged app; and only when the
    // finger sits in the inner region of the target's cell — the outer ring reads as "make room".
    val combinable = under != null &&
        session.item is AppItem &&
        (under is AppItem || under is FolderItem) &&
        run {
            val origin = metrics.cellTopLeft(under.cell)
            val fx = (pointer.x - origin.x) / metrics.cellW
            val fy = (pointer.y - origin.y) / metrics.cellH
            fx in 0.22f..0.78f && fy in 0.15f..0.85f
        }
    session.hoverItemId = if (combinable) under?.id else null
}

/**
 * The §5 dwell clocks, each an effect keyed on what it watches:
 * folder dwell 520ms, hover/reflow dwell 200ms, page-edge 400ms then every 600ms.
 */
@Composable
internal fun DragEffects(
    vm: HomeViewModel,
    homeState: HomeState,
    pagerState: PagerState,
    metrics: GridMetrics,
    haptics: Haptics,
) {
    val session = homeState.drag ?: return

    // Folder-creation dwell: 520ms held over the same target, with the target dilating as the
    // affordance (the dilation renders in PageGrid from combineTargetId).
    LaunchedEffect(session, session.hoverItemId) {
        session.combineTargetId = null
        val target = session.hoverItemId ?: return@LaunchedEffect
        delay(FOLDER_DWELL_MS)
        session.combineTargetId = target
        haptics.tick()
    }

    // Hover dwell: 200ms on a cell commits the live PACKED reflow preview (FREEFORM shows none —
    // nothing may move until the drop, so the preview would be a lie).
    LaunchedEffect(session, session.hoverCell, session.hoverItemId, session.hoverPage) {
        if (session.hoverItemId != null) return@LaunchedEffect
        val cell = session.hoverCell ?: run {
            homeState.preview = null
            return@LaunchedEffect
        }
        delay(SWAP_DWELL_MS)
        if (vm.currentModel() == HomeModel.PACKED) {
            homeState.preview = vm.previewMove(session.item.id, session.hoverPage, cell)
            haptics.tick()
        }
    }

    // Page-edge auto-advance: 400ms dwell, then a flip every 600ms while the finger stays.
    LaunchedEffect(session, session.edgeDirection) {
        val direction = session.edgeDirection
        if (direction == 0) return@LaunchedEffect
        delay(EDGE_DWELL_MS)
        while (isActive && session.edgeDirection == direction && !session.dropping) {
            val target = pagerState.currentPage + direction
            if (target in 0 until pagerState.pageCount) {
                pagerState.animateScrollToPage(target)
                session.hoverPage = target
                haptics.tick()
            }
            delay(EDGE_REPEAT_MS)
        }
    }

    // Keep hoverPage synced with pager settles that were not edge-driven.
    LaunchedEffect(session, pagerState.currentPage) {
        if (session.edgeDirection == 0) session.hoverPage = pagerState.currentPage
    }
}

/**
 * The drop, resolved in §5's priority order: folder combine, then a placed move, then the spring
 * back to origin (no haptic — the *absence* is the "that didn't work" signal). The release velocity
 * carries into the settle spring: `follow` → `morph`, §3's handoff.
 */
internal fun commitDrop(
    vm: HomeViewModel,
    homeState: HomeState,
    session: DragSession,
    metrics: GridMetrics,
    scope: CoroutineScope,
    haptics: Haptics,
    motion: dev.lumen.launcher.core.design.motion.MotionTokens,
) {
    session.dropping = true
    val velocity = session.velocity.calculateVelocity()
    val velocityOffset = Offset(velocity.x, velocity.y)

    scope.launch {
        val combineTarget = session.combineTargetId
        val cell = session.hoverCell
        when {
            combineTarget != null -> {
                val targetItem = vm.state.value.find(combineTarget)
                val settle = targetItem?.let { metrics.cellTopLeft(it.cell) }
                if (settle != null) {
                    session.position.animateTo(settle, motion.morphSpring(), velocityOffset)
                }
                vm.combine(combineTarget, session.item.id)
                haptics.snap()
            }

            cell != null -> {
                val iconPx = metrics.cellW * 0.62f
                val settle = metrics.iconTopLeftIn(cell, iconPx)
                session.position.animateTo(settle, motion.morphSpring(), velocityOffset)
                vm.move(session.item.id, session.hoverPage, cell)
                haptics.snap()
            }

            else -> {
                // Invalid drop: spring home with morph, and deliberately no haptic (§5).
                val home = metrics.cellTopLeft(session.item.cell)
                session.position.animateTo(home, motion.morphSpring(), velocityOffset)
            }
        }
        homeState.preview = null
        homeState.drag = null
    }
}

/** The item under the finger: 1:1 with the pointer, slightly grown and translucent. */
@Composable
internal fun DragOverlay(
    vm: HomeViewModel,
    session: DragSession,
    geometry: GridGeometry,
) {
    val position by session.position.asState()
    Box(
        modifier = Modifier
            .offset { position.round() }
            .scale(1.08f)
            .alpha(0.92f),
    ) {
        when (val item = session.item) {
            is AppItem -> {
                val icon = rememberAppIcon(vm.iconCache, item.key, geometry.iconSize)
                MaskedIcon(icon = icon, size = geometry.iconSize)
            }

            is FolderItem -> {
                FolderIcon(
                    vm = vm,
                    folder = item,
                    iconSize = geometry.iconSize,
                    showLabel = false,
                )
            }

            else -> LetterTile(label = "?", size = geometry.iconSize)
        }
    }
}
