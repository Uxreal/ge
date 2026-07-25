package dev.lumen.launcher.feature.widgets

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.drawable.toBitmap
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.interaction.launcherPressable
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.surface.FrostedSurface
import dev.lumen.launcher.core.design.surface.LocalBackdropCapture
import dev.lumen.launcher.core.design.surface.SurfaceRole
import dev.lumen.launcher.core.design.theme.LocalTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The widget picker: providers grouped by app, real preview images decoded off the main thread,
 * and the widget's cell span shown up front. Picking hands the provider to the shell, which owns
 * the bind/configure Activity flow.
 */
@Composable
fun WidgetPickerSheet(
    hostManager: WidgetHostManager,
    visible: Boolean,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val typography = LocalTypography.current
    val capture = LocalBackdropCapture.current
    val providers by hostManager.providers.collectAsStateWithLifecycle()

    LaunchedEffect(visible) { if (visible) hostManager.refreshProviders() }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = motion.enter()) { it / 2 } + fadeIn(motion.crossfade()),
        exit = slideOutVertically(animationSpec = motion.enter()) { it / 2 } + fadeOut(motion.crossfade()),
        modifier = modifier,
    ) {
        val grouped = remember(providers) {
            providers.groupBy { hostManager.appLabelOf(it) }.toSortedMap()
        }

        val list: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    BasicText(
                        text = "Widgets",
                        style = typography.headerLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
                    )
                }
                grouped.forEach { (appLabel, group) ->
                    item(key = "header-$appLabel") {
                        BasicText(
                            text = appLabel,
                            style = typography.tileLabel.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(group, key = { it.provider.flattenToString() }) { provider ->
                        ProviderRow(hostManager, provider, onPick)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        ) {
            if (capture != null) {
                FrostedSurface(
                    role = SurfaceRole.SHEETS,
                    capture = capture,
                    cornerRadius = 0.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    list()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    list()
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    hostManager: WidgetHostManager,
    provider: AppWidgetProviderInfo,
    onPick: (AppWidgetProviderInfo) -> Unit,
) {
    val typography = LocalTypography.current
    val haptics = LocalHaptics.current

    var preview by remember(provider) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(provider) {
        preview = withContext(Dispatchers.IO) {
            runCatching {
                hostManager.previewOf(provider)?.toBitmap(width = 320, height = 200)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                MaterialTheme.shapes.medium,
            )
            .launcherPressable(onClick = {
                haptics.state()
                onPick(provider)
            })
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = 96.dp, height = 60.dp), contentAlignment = Alignment.Center) {
            preview?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column {
            BasicText(
                text = hostManager.labelOf(provider),
                style = typography.body.copy(color = MaterialTheme.colorScheme.onSurface),
            )
            val cells = "${(provider.minWidth / 180).coerceAtLeast(1)} × ${(provider.minHeight / 110).coerceAtLeast(1)}"
            BasicText(
                text = cells,
                style = typography.tileLabel.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
