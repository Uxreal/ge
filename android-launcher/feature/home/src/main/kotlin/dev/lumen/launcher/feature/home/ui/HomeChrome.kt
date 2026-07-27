package dev.lumen.launcher.feature.home.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.interaction.launcherPressable
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.Depth
import dev.lumen.launcher.core.design.theme.LocalTypography
import kotlinx.coroutines.launch

/** Page dots. The current dot grows with `micro`; tapping a dot jumps with `page`. */
@Composable
internal fun PageIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pagerState.pageCount) { index ->
            val active = pagerState.currentPage == index
            val diameter by animateDpAsState(
                targetValue = if (active) 7.dp else 5.dp,
                animationSpec = motion.micro(),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Page ${index + 1} of ${pagerState.pageCount}"
                    }
                    .pointerInput(index) {
                        detectTapGestures {
                            scope.launch {
                                pagerState.animateScrollToPage(index, animationSpec = motion.page())
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(diameter)
                        .drawWithCache {
                            val dot = Superellipse.path(size, size.minDimension / 2f, 2f)
                            onDrawBehind {
                                drawPath(dot, Color.White.copy(alpha = if (active) 0.95f else 0.45f))
                            }
                        },
                )
            }
        }
    }
}

/** Wiggle-mode actions. Small pill chips: Widgets, Settings, Done. */
@Composable
internal fun EditModeBar(
    onWidgets: () -> Unit,
    onSettings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EditChip("Widgets", onWidgets)
        EditChip("Settings", onSettings)
        EditChip("Done", onDone, emphasized = true)
    }
}

@Composable
private fun EditChip(label: String, onClick: () -> Unit, emphasized: Boolean = false) {
    val typography = LocalTypography.current
    val haptics = LocalHaptics.current
    val background = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
    }
    val foreground = if (emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .semantics { role = Role.Button }
            .drawWithCache {
                val pill = Superellipse.path(size, size.height / 2f)
                onDrawBehind {
                    drawPath(pill, background)
                    drawPath(
                        pill,
                        Depth.HairlineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f),
                    )
                }
            }
            .launcherPressable(onClick = {
                haptics.state()
                onClick()
            })
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        BasicText(text = label, style = typography.tileLabel.copy(color = foreground))
    }
}

/** FREEFORM's visible way into the drawer: a grabber pill that also opens on tap. */
@Composable
internal fun DrawerHandle(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 26.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Open app drawer"
            }
            .pointerInput(onOpenDrawer) { detectTapGestures(onTap = { onOpenDrawer() }) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .drawWithCache {
                    val pill = Superellipse.path(size, size.height / 2f, 2f)
                    onDrawBehind { drawPath(pill, Color.White.copy(alpha = 0.65f)) }
                },
        )
    }
}

/** An empty FREEFORM page explains itself instead of showing bare wallpaper. */
@Composable
internal fun EmptyHomeHint(onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    val typography = LocalTypography.current
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .pointerInput(onOpenDrawer) { detectTapGestures(onTap = { onOpenDrawer() }) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(
            text = "Your home is empty",
            style = typography.capsuleTitle.copy(
                color = Color.White,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    blurRadius = 6f,
                ),
            ),
        )
        BasicText(
            text = "Swipe up for your apps, then hold one and choose Add to Home",
            style = typography.body.copy(
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    blurRadius = 6f,
                ),
            ),
        )
    }
}

/**
 * The dock (D38): up to five pinned apps on a translucent superellipse bar, constant across
 * pages. Tap launches; long-press removes the pin (the drawer's "Add to Dock" puts it back —
 * cheap operations do not need a confirmation ceremony). No labels: dock apps are the ones whose
 * icons you already know.
 */
@Composable
internal fun DockBar(
    vm: dev.lumen.launcher.feature.home.HomeViewModel,
    apps: List<dev.lumen.launcher.core.data.model.AppInfo>,
    modifier: Modifier = Modifier,
) {
    val haptics = dev.lumen.launcher.core.design.interaction.LocalHaptics.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                MaterialTheme.shapes.extraLarge,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        apps.forEach { app ->
            val icon = rememberAppIcon(vm.iconCache, app.key, DOCK_ICON)
            dev.lumen.launcher.core.design.icon.AppIcon(
                icon = icon,
                label = app.label,
                iconSize = DOCK_ICON,
                showLabel = false,
                onClick = { bounds -> vm.launch(app.key, bounds) },
                onLongPress = {
                    haptics.commit()
                    vm.removeFromDock(app.key)
                },
            )
        }
    }
}

private val DOCK_ICON = 50.dp
