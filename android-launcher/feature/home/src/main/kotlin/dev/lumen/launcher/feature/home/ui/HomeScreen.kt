package dev.lumen.launcher.feature.home.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.data.model.WidgetItem
import dev.lumen.launcher.core.design.grid.GridGeometry
import dev.lumen.launcher.core.design.grid.GridSpec
import dev.lumen.launcher.core.design.grid.computeGridGeometry
import dev.lumen.launcher.core.design.interaction.Haptics
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.surface.LocalBackdropCapture
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.home.HomeViewModel
import kotlin.math.abs

/**
 * The home surface: bottom-anchored pages (§1), the §5 drag system at its exact timings, wiggle
 * mode, folders, and the reserved strip at the top where the Capsule lives from Phase 2 on.
 *
 * Widgets render through [widgetContent] because `:feature:home` must not depend on
 * `:feature:widgets` (§2's module boundaries) — the app shell supplies the renderer.
 */
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    homeState: HomeState,
    modifier: Modifier = Modifier,
    onPagerChanged: (page: Int, offset: Float, count: Int) -> Unit = { _, _, _ -> },
    /** FREEFORM: swipe up anywhere on the home surface opens the drawer. Null disables it. */
    onOpenDrawer: (() -> Unit)? = null,
    onRequestWidgetPicker: () -> Unit = {},
    onRequestSettings: () -> Unit = {},
    onReleaseWidget: (Int) -> Unit = {},
    widgetContent: @Composable (WidgetItem, Modifier, Boolean, (Int, Int) -> Unit) -> Unit =
        { _, m, _, _ -> Box(m) },
) {
    val workspace by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val allApps by vm.apps.collectAsStateWithLifecycle()
    val dock = remember(allApps, prefs.dockKeys) { vm.dockApps() }
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val typography = LocalTypography.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        val labelLine = with(density) { typography.iconLabel.lineHeight.toDp() }
        val geometry = computeGridGeometry(
            screenWidth = maxWidth,
            screenHeight = maxHeight,
            horizontalInsets = insets.calculateLeftPadding(LayoutDirection.Ltr) +
                insets.calculateRightPadding(LayoutDirection.Ltr),
            verticalInsets = insets.calculateTopPadding() + insets.calculateBottomPadding(),
            reservedTop = CAPSULE_STRIP,
            reservedBottom = INDICATOR_STRIP +
                if (dock.isEmpty() && onOpenDrawer == null) 0.dp else DOCK_STRIP,
            isTablet = maxWidth >= 600.dp,
            spec = GridSpec(
                columns = prefs.columns,
                labelLines = prefs.labelLines,
                showLabels = !prefs.hideLabels,
            ),
            labelLineHeight = labelLine,
        )
        LaunchedEffect(geometry.columns, geometry.rows) {
            vm.onGridDimensions(geometry.columns, geometry.rows)
        }

        val metrics = remember(geometry, constraints, density) {
            GridMetrics(
                density = density,
                geometry = geometry,
                insets = insets,
                containerWidth = constraints.maxWidth.toFloat(),
                containerHeight = constraints.maxHeight.toFloat(),
            )
        }

        val drag = homeState.drag
        val rendered = homeState.preview ?: workspace
        // One trailing empty page while dragging or editing — the drop target for "a new page".
        val pageCount = rendered.pageCount + if (drag != null || homeState.editMode) 1 else 0
        val pagerState = rememberPagerState(pageCount = { pageCount })

        LaunchedEffect(pagerState) {
            snapshotFlow {
                Triple(pagerState.currentPage, pagerState.currentPageOffsetFraction, pagerState.pageCount)
            }.collect { (page, offset, count) -> onPagerChanged(page, offset, count) }
        }
        LaunchedEffect(homeState) {
            for (unit in homeState.scrollToFirst) {
                pagerState.animateScrollToPage(0)
            }
        }
        // §3 haptics: `edge` when a fling bumps the first or last page.
        LaunchedEffect(pagerState) {
            var previous = 0f
            snapshotFlow { pagerState.currentPageOffsetFraction }.collect { fraction ->
                val page = pagerState.currentPage
                val atEdge = (page == 0 && fraction < -0.12f) ||
                    (page == pagerState.pageCount - 1 && fraction > 0.12f)
                if (atEdge && abs(previous) <= 0.12f) haptics.edge()
                previous = fraction
            }
        }

        // §5: lift is 280ms, not the platform's long-press default.
        val configuration = LocalViewConfiguration.current
        val liftConfiguration = remember(configuration) {
            object : ViewConfiguration by configuration {
                override val longPressTimeoutMillis: Long get() = LIFT_MS
            }
        }

        // A frosted surface has no way to know the pixels behind it moved, so the pager holds a
        // redraw ticket for exactly as long as it is scrolling. Without this the Capsule's blur
        // freezes mid-swipe; with it always on, a still home screen would re-record every frame.
        val capture = LocalBackdropCapture.current
        DisposableEffect(capture) {
            onDispose { capture?.setAnimating(PAGER_TICKET, false) }
        }
        LaunchedEffect(pagerState, capture) {
            snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                capture?.setAnimating(PAGER_TICKET, scrolling)
            }
        }

        CompositionLocalProvider(LocalViewConfiguration provides liftConfiguration) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = drag == null,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = motion.page(),
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .emptySpaceGestures(homeState, haptics)
                    .drawerSwipe(homeState, onOpenDrawer),
            ) { page ->
                PageGrid(
                    vm = vm,
                    homeState = homeState,
                    items = rendered.itemsOn(page).filterNot { it.id == drag?.item?.id },
                    page = page,
                    metrics = metrics,
                    pagerState = pagerState,
                    onReleaseWidget = onReleaseWidget,
                    widgetContent = widgetContent,
                    modifier = Modifier.pageDepth(pagerState, page, motion.reduceMotion),
                )
            }
        }

        DragEffects(
            vm = vm,
            homeState = homeState,
            pagerState = pagerState,
            metrics = metrics,
            haptics = haptics,
        )

        drag?.let { session ->
            DragOverlay(vm = vm, session = session, geometry = geometry)
        }

        // FREEFORM, empty page: say how to fill it instead of presenting bare wallpaper.
        if (onOpenDrawer != null && workspace.seeded && workspace.items.isEmpty() &&
            drag == null && !homeState.editMode
        ) {
            EmptyHomeHint(
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        PageIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -insets.calculateBottomPadding()),
        )

        if ((dock.isNotEmpty() || onOpenDrawer != null) && drag == null) {
            DockBar(
                vm = vm,
                apps = dock,
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -insets.calculateBottomPadding() - INDICATOR_STRIP),
            )
        }

        // The drawer's visible affordance: a small handle above the page dots.
        if (onOpenDrawer != null && drag == null) {
            DrawerHandle(
                onOpenDrawer = onOpenDrawer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -insets.calculateBottomPadding() - 22.dp),
            )
        }

        if (homeState.editMode && drag == null) {
            EditModeBar(
                onWidgets = onRequestWidgetPicker,
                onSettings = onRequestSettings,
                onDone = { homeState.editMode = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = insets.calculateTopPadding() + 8.dp),
            )
        }

        homeState.openFolderId?.let { folderId ->
            val folder = workspace.find(folderId) as? FolderItem
            if (folder == null) {
                homeState.openFolderId = null
            } else {
                FolderSheet(
                    vm = vm,
                    folder = folder,
                    iconSize = geometry.iconSize,
                    onDismiss = { homeState.openFolderId = null },
                )
            }
        }
    }
}

/**
 * The launcher's page transition. §1.1 rules out bouncy horizontal paging, so the character has to
 * come from depth rather than from spring overshoot: the outgoing page recedes and dims while its
 * contents lag the swipe, so pages read as sheets sliding over one another instead of a filmstrip.
 *
 * Reduce-motion drops all of it and pages flat, which is the point of the setting.
 */
private fun Modifier.pageDepth(pagerState: PagerState, page: Int, reduceMotion: Boolean): Modifier {
    if (reduceMotion) return this
    return graphicsLayer {
        val offset =
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val distance = abs(offset).coerceIn(0f, 1f)

        // Contents move at 80% of the page, so the grid trails the gesture by a few dp.
        translationX = offset * size.width * PAGE_PARALLAX
        val recede = 1f - PAGE_RECEDE * distance
        scaleX = recede
        scaleY = recede
        alpha = 1f - PAGE_DIM * distance
        // A shallow turn, with the camera far enough back that it reads as depth rather than as a
        // 3D effect. Pivot on the trailing edge so adjacent pages hinge against each other.
        cameraDistance = PAGE_CAMERA * density
        rotationY = offset * PAGE_TURN_DEG
        transformOrigin = TransformOrigin(if (offset > 0f) 1f else 0f, 0.5f)
    }
}

/**
 * FREEFORM's drawer gesture: an upward swipe anywhere on the home surface (like the Pixel
 * launcher). Horizontal motion belongs to the pager and item drags consume their own events first,
 * so this only sees what nothing else wanted.
 *
 * Distance alone was the wrong test: a fast flick covers less ground before the finger leaves the
 * glass than a slow drag does, so the quick, confident swipe — the one people actually make — was
 * the one that failed. A short throw counts when it is fast enough.
 */
private fun Modifier.drawerSwipe(homeState: HomeState, onOpenDrawer: (() -> Unit)?): Modifier {
    if (onOpenDrawer == null) return this
    return pointerInput(homeState, onOpenDrawer) {
        val slowThreshold = 72.dp.toPx()
        val flickThreshold = 24.dp.toPx()
        var total = 0f
        var startedAt = 0L
        var fired = false

        detectVerticalDragGestures(
            onDragStart = {
                total = 0f
                fired = false
                startedAt = 0L
            },
            onVerticalDrag = { change, dragAmount ->
                if (startedAt == 0L) startedAt = change.uptimeMillis
                total += dragAmount
                if (fired || homeState.drag != null || homeState.editMode) return@detectVerticalDragGestures

                val elapsed = (change.uptimeMillis - startedAt).coerceAtLeast(1L)
                val upwardSpeed = -total / elapsed * 1000f // px per second
                val flicked = total < -flickThreshold && upwardSpeed > FLICK_SPEED_PX_S
                if (total < -slowThreshold || flicked) {
                    fired = true
                    change.consume()
                    onOpenDrawer()
                }
            },
        )
    }
}

/** Long-press on empty wallpaper enters wiggle mode; a tap leaves it (§5). */
private fun Modifier.emptySpaceGestures(homeState: HomeState, haptics: Haptics): Modifier =
    pointerInput(homeState) {
        detectTapGestures(
            onTap = { if (homeState.editMode) homeState.editMode = false },
            onLongPress = {
                if (!homeState.editMode && homeState.drag == null) {
                    haptics.lift()
                    homeState.editMode = true
                }
            },
        )
    }

/** Fast enough to read as a flick rather than a scroll that changed its mind. */
private const val FLICK_SPEED_PX_S = 850f

// Tuned down after the first hardware report ("feels slow"): heavy dim and recede read as drag,
// not depth. The cue only needs to be legible mid-swipe, and mid-swipe is fast.
private const val PAGE_PARALLAX = 0.12f
private const val PAGE_RECEDE = 0.05f
private const val PAGE_DIM = 0.18f
private const val PAGE_TURN_DEG = 3.5f
private const val PAGE_CAMERA = 18f
private const val PAGER_TICKET = "home-pager"

internal const val LIFT_MS = 280L
internal const val SWAP_DWELL_MS = 200L
internal const val FOLDER_DWELL_MS = 520L
internal const val EDGE_DWELL_MS = 400L
internal const val EDGE_REPEAT_MS = 600L
internal val CAPSULE_STRIP = 56.dp
internal val INDICATOR_STRIP = 28.dp
internal val DOCK_STRIP = 66.dp

/** Pixel-space geometry shared by every page and the drag layer. Coordinates are window-root. */
internal class GridMetrics(
    density: Density,
    val geometry: GridGeometry,
    insets: PaddingValues,
    val containerWidth: Float,
    val containerHeight: Float,
) {
    val cellW = with(density) { geometry.cellWidth.toPx() }
    val cellH = with(density) { geometry.cellHeight.toPx() }
    val marginX = with(density) {
        (geometry.pageMargin + insets.calculateLeftPadding(LayoutDirection.Ltr)).toPx()
    }
    private val topInset = with(density) { insets.calculateTopPadding().toPx() }
    private val bottomInset = with(density) { insets.calculateBottomPadding().toPx() }
    private val capsuleStrip = with(density) { CAPSULE_STRIP.toPx() }
    private val indicatorStrip = with(density) { INDICATOR_STRIP.toPx() }
    val edgeZone = with(density) { 28.dp.toPx() }

    /** Top of the grid block — bottom-anchored below the Capsule strip (§1). */
    val gridTop: Float = run {
        val available = containerHeight - topInset - capsuleStrip - bottomInset - indicatorStrip
        topInset + capsuleStrip + (available - geometry.rows * cellH).coerceAtLeast(0f)
    }

    fun cellAt(position: Offset): Cell? {
        val x = ((position.x - marginX) / cellW).toInt()
        val y = ((position.y - gridTop) / cellH).toInt()
        if (x !in 0 until geometry.columns || y !in 0 until geometry.rows) return null
        return Cell(x, y)
    }

    fun cellTopLeft(cell: Cell): Offset =
        Offset(marginX + cell.x * cellW, gridTop + cell.y * cellH)

    /** Where a 1×1 item's icon sits inside its cell (centred horizontally, at the top). */
    fun iconTopLeftIn(cell: Cell, iconSizePx: Float): Offset {
        val cellOrigin = cellTopLeft(cell)
        return Offset(cellOrigin.x + (cellW - iconSizePx) / 2f, cellOrigin.y)
    }
}
