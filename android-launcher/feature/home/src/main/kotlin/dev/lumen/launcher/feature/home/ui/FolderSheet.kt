package dev.lumen.launcher.feature.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.core.data.model.FolderItem
import dev.lumen.launcher.core.design.icon.AppIcon
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.surface.FrostedSurface
import dev.lumen.launcher.core.design.surface.LocalBackdropCapture
import dev.lumen.launcher.core.design.surface.SurfaceRole
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.home.HomeViewModel
import kotlinx.coroutines.launch

/**
 * A folder, opened full-screen over the blurred home surface (§6): frosted sheet, inline rename,
 * children in a grid. Enters and leaves with `morph` — springy, from the folder's home.
 *
 * §9's non-drag equivalent for pulling an app out: each child exposes a "Remove from folder"
 * accessibility action, and the same operation is one drag away for touch users on the grid.
 */
@Composable
internal fun FolderSheet(
    vm: HomeViewModel,
    folder: FolderItem,
    iconSize: Dp,
    onDismiss: () -> Unit,
) {
    val motion = LocalMotion.current
    val typography = LocalTypography.current
    val capture = LocalBackdropCapture.current
    val scope = rememberCoroutineScope()
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    // morph in: scale from 0.86 with fade; reversed on dismiss before the callback fires.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, motion.morph()) }
    val close: () -> Unit = {
        scope.launch {
            progress.animateTo(0f, motion.morph())
            onDismiss()
        }
    }

    var label by remember(folder.id) { mutableStateOf(folder.label) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(progress.value.coerceIn(0f, 1f))
            .pointerInput(folder.id) { detectTapGestures(onTap = { close() }) },
        contentAlignment = Alignment.Center,
    ) {
        val sheet: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .scale(0.86f + 0.14f * progress.value)
                    .pointerInput(Unit) { detectTapGestures { /* consumed: taps inside stay inside */ } }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicTextField(
                    value = label,
                    onValueChange = {
                        label = it.take(24)
                        vm.renameFolder(folder.id, label)
                    },
                    textStyle = typography.capsuleTitle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (label.isEmpty()) {
                                androidx.compose.foundation.text.BasicText(
                                    text = "Folder name",
                                    style = typography.capsuleTitle.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(folder.items, key = { it.id }) { child ->
                        val icon = rememberAppIcon(vm.iconCache, child.key, iconSize)
                        Box(contentAlignment = Alignment.Center) {
                            AppIcon(
                                icon = icon,
                                label = vm.appLabel(child.key),
                                iconSize = iconSize,
                                labelLines = 1,
                                smoothness = prefs.smoothness,
                                accessibilityActions = listOf(
                                    androidx.compose.ui.semantics.CustomAccessibilityAction(
                                        "Remove from folder",
                                    ) {
                                        vm.move(child.id, folder.page, folder.cell)
                                        true
                                    },
                                    androidx.compose.ui.semantics.CustomAccessibilityAction("App info") {
                                        vm.appInfo(child.key, null)
                                        true
                                    },
                                ),
                                onClick = { rect ->
                                    vm.launch(child.key, rect)
                                    close()
                                },
                                onLongPress = {
                                    // Non-drag pull-out for touch too: long-press ejects the app
                                    // back onto the folder's page.
                                    vm.move(child.id, folder.page, folder.cell)
                                },
                                modifier = Modifier,
                            )
                        }
                    }
                }
            }
        }

        if (capture != null) {
            FrostedSurface(
                role = SurfaceRole.SHEETS,
                capture = capture,
                cornerRadius = 32.dp,
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(12.dp),
            ) {
                sheet()
            }
        } else {
            // No backdrop capture (previews, tests): a plain surface panel instead of frosted glass.
            Box(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
            ) {
                sheet()
            }
        }
    }
}
