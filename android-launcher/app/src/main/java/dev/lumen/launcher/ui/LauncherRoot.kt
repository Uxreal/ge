package dev.lumen.launcher.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.data.LauncherServices
import dev.lumen.launcher.data.LocalLauncherActivity
import dev.lumen.launcher.data.LocalServices
import dev.lumen.launcher.data.model.Cell
import dev.lumen.launcher.data.model.ItemContainer
import dev.lumen.launcher.data.model.WidgetItem
import dev.lumen.launcher.data.model.WidgetProviderInfo
import dev.lumen.launcher.data.prefs.LauncherSettings
import dev.lumen.launcher.data.workspace.WorkspaceOps
import dev.lumen.launcher.ui.common.LocalHaptics
import dev.lumen.launcher.ui.common.rememberHaptics
import dev.lumen.launcher.ui.debug.DebugOverlay
import dev.lumen.launcher.ui.drag.DragOverlay
import dev.lumen.launcher.ui.drag.DragState
import dev.lumen.launcher.ui.drag.LocalDragState
import dev.lumen.launcher.ui.drawer.AppDrawer
import dev.lumen.launcher.ui.glass.backdropSource
import dev.lumen.launcher.ui.glass.rememberBackdropState
import dev.lumen.launcher.ui.home.FolderOverlay
import dev.lumen.launcher.ui.home.HomeScreen
import dev.lumen.launcher.ui.home.ItemContextMenu
import dev.lumen.launcher.ui.onboarding.SetupBanner
import dev.lumen.launcher.ui.settings.SettingsScreen
import dev.lumen.launcher.ui.theme.LocalMotion
import dev.lumen.launcher.ui.theme.LumenTheme
import dev.lumen.launcher.ui.wallpaper.WallpaperLayer
import dev.lumen.launcher.ui.widgets.WidgetPickerSheet

/**
 * Composition root.
 *
 * The layering here is deliberate and load-bearing: the wallpaper and workspace live *inside* the
 * [backdropSource] subtree, and every overlay (drawer, settings, folders, menus) lives *outside* it.
 * That is what lets a glass drawer refract the home screen underneath it without the capture
 * feeding back into itself.
 */
@Composable
fun LauncherRoot(
    activity: Activity,
    services: LauncherServices,
    registerHomePress: (() -> Unit) -> Unit,
) {
    val settings by services.settings.settings.collectAsStateWithLifecycle()
    val workspace by services.workspace.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val backdrop = rememberBackdropState()
    val controller = remember(services) {
        LauncherController(services, scope, { activity })
    }
    val dragState = remember { DragState() }

    // Keep the workspace populated as apps come and go.
    LaunchedEffect(services) {
        services.workspace.ensureInitialized(
            apps = services.apps.apps.value,
            columns = services.settings.settings.value.grid.columns,
            rows = services.settings.settings.value.grid.rows,
            dockColumns = services.settings.settings.value.dock.columns,
        )
    }
    LaunchedEffect(services) {
        services.apps.installed.collect { key ->
            if (services.settings.settings.value.home.autoPlaceNewApps) {
                val grid = services.settings.settings.value.grid
                services.workspace.mutate { state ->
                    WorkspaceOps.autoPlace(state, listOf(key), grid.columns, grid.rows)
                }
            }
        }
    }
    LaunchedEffect(services) {
        services.apps.uninstalled.collect { key ->
            services.workspace.mutate { state ->
                WorkspaceOps.dissolveThinFolders(WorkspaceOps.removeAppEverywhere(state, key))
            }
        }
    }

    DisposableEffect(controller) {
        registerHomePress {
            if (!controller.onBack()) controller.currentPage = 0
        }
        onDispose { registerHomePress {} }
    }

    CompositionLocalProvider(
        LocalServices provides services,
        LocalSettings provides settings,
        LocalController provides controller,
        LocalBackdrop provides backdrop,
        LocalDragState provides dragState,
        LocalLauncherActivity provides activity,
    ) {
        LumenTheme(settings) {
            val haptics = rememberHaptics()
            val motion = LocalMotion.current
            val overlayOpen = controller.overlay != LauncherOverlay.None
            val overlayProgress by animateFloatAsState(
                targetValue = if (overlayOpen) 1f else 0f,
                animationSpec = motion.effect(),
                label = "overlayProgress",
            )

            // Glass needs a live backdrop while an overlay animates in or out.
            LaunchedEffect(overlayProgress) {
                backdrop.setContinuous("overlay", overlayProgress > 0.002f && overlayProgress < 0.998f)
            }
            // …and for the whole duration of a drag, since the workspace moves under the panels.
            LaunchedEffect(dragState.isDragging) {
                backdrop.setContinuous("drag", dragState.isDragging)
            }

            // Hoisted out of the widget-picker callback: it reads configuration and density.
            val approximateCell = approximateCellSize(settings)

            CompositionLocalProvider(LocalHaptics provides haptics) {
                BackHandler(enabled = true) { controller.onBack() }

                Box(modifier = Modifier.fillMaxSize()) {
                    // ---- refractable content -------------------------------------------------
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .backdropSource(backdrop),
                    ) {
                        WallpaperLayer(
                            pageScroll = controller.currentPage.toFloat(),
                            pageCount = workspace.pageCount.coerceAtLeast(1),
                            overlayProgress = overlayProgress,
                        )
                        HomeScreen()
                    }

                    // ---- lenses -------------------------------------------------------------
                    AppDrawer(
                        visible = controller.overlay == LauncherOverlay.Drawer,
                        onDismiss = { controller.closeOverlay() },
                    )

                    when (val overlay = controller.overlay) {
                        is LauncherOverlay.Settings -> SettingsScreen(
                            onClose = { controller.closeOverlay() },
                        )

                        is LauncherOverlay.WidgetPicker -> WidgetPickerSheet(
                            onDismiss = { controller.closeOverlay() },
                            onPicked = { appWidgetId, provider ->
                                placeWidget(controller, provider, appWidgetId, approximateCell)
                                controller.closeOverlay()
                            },
                        )

                        is LauncherOverlay.Folder -> FolderOverlay(
                            folderId = overlay.folderId,
                            origin = overlay.origin,
                            onDismiss = { controller.closeOverlay() },
                        )

                        is LauncherOverlay.ItemMenu -> ItemContextMenu(
                            itemId = overlay.itemId,
                            anchor = overlay.anchor,
                            onDismiss = { controller.closeOverlay() },
                        )

                        LauncherOverlay.None, LauncherOverlay.Drawer -> Unit
                    }

                    // Only shows while another launcher still owns the HOME intent.
                    if (controller.overlay == LauncherOverlay.None && !controller.editMode) {
                        SetupBanner(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .windowInsetsPadding(WindowInsets.statusBars),
                        )
                    }

                    // Above everything, including overlays: the item under the finger.
                    DragOverlay()

                    if (settings.advanced.showFps || settings.advanced.debugOverlay) {
                        DebugOverlay()
                    }
                }
            }
        }
    }
}

/** Rough cell size in pixels, used only to pick a sensible initial span for a new widget. */
@Composable
private fun approximateCellSize(settings: LauncherSettings): Pair<Int, Int> {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    return remember(configuration, density, settings.grid.columns, settings.grid.rows) {
        with(density) {
            val width = configuration.screenWidthDp.dp.toPx() / settings.grid.columns.coerceAtLeast(1)
            // The workspace occupies roughly the screen minus status bar, dock and indicator.
            val height = (configuration.screenHeightDp.dp.toPx() * 0.72f) /
                settings.grid.rows.coerceAtLeast(1)
            width.toInt() to height.toInt()
        }
    }
}

private fun placeWidget(
    controller: LauncherController,
    provider: WidgetProviderInfo,
    appWidgetId: Int,
    cell: Pair<Int, Int>,
) {
    val settings = controller.settings
    val (spanX, spanY) = controller.services.widgets.defaultSpan(
        provider.providerFlat,
        cell.first.coerceAtLeast(1),
        cell.second.coerceAtLeast(1),
    )
    controller.mutateWorkspace { state ->
        val pageIndex = controller.currentPage.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
        val page = state.pages.getOrNull(pageIndex)
        val item = WidgetItem(
            id = WorkspaceOps.newId("widget"),
            appWidgetId = appWidgetId,
            providerFlat = provider.providerFlat,
            label = provider.label,
            profile = provider.profile,
            cell = Cell(0, 0, spanX, spanY),
        )
        val target = page?.let {
            WorkspaceOps.firstFreeCell(it.items, settings.grid.columns, settings.grid.rows, spanX, spanY)
        }
        if (page != null && target != null) {
            WorkspaceOps.add(state, ItemContainer.Page(page.id), item.withCell(target))
        } else {
            val grown = WorkspaceOps.addPage(state)
            WorkspaceOps.add(
                grown,
                ItemContainer.Page(grown.pages.last().id),
                item.withCell(Cell(0, 0, spanX, spanY)),
            )
        }
    }
}
