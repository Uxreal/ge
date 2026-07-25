package dev.lumen.launcher.feature.home.ui

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    onRequestWidgetPicker: () -> Unit = {},
    onRequestSettings: () -> Unit = {},
    onReleaseWidget: (Int) -> Unit = {},
    widgetContent: @Composable (WidgetItem, Modifier, Boolean, (Int, Int) -> Unit) -> Unit =
        { _, m, _, _ -> Box(m) },
) {
    val workspace by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
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
            reservedBottom = INDICATOR_STRIP,
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
                    .emptySpaceGestures(homeState, haptics),
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

        PageIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -insets.calculateBottomPadding()),
        )

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

internal const val LIFT_MS = 280L
internal const val SWAP_DWELL_MS = 200L
internal const val FOLDER_DWELL_MS = 520L
internal const val EDGE_DWELL_MS = 400L
internal const val EDGE_REPEAT_MS = 600L
internal val CAPSULE_STRIP = 56.dp
internal val INDICATOR_STRIP = 28.dp

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
