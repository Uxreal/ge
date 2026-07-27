package dev.lumen.launcher.core.design.icon

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.interaction.launcherPressable
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.ContrastScrim
import dev.lumen.launcher.core.design.theme.LocalTypography
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The one icon primitive: home grid, drawer, folders and search results all render through this.
 *
 * The caller supplies an already-decoded, **unmasked** bitmap. §3 requires masking at draw time so a
 * shape or theme change is instant and invalidates no cache, which is why the mask is applied here
 * and not in the icon pipeline — and why `:core:design` needs no data dependency to draw an icon.
 *
 * @param onClick receives the icon's window bounds, which the system uses as the origin of the
 *   app-open animation.
 * @param backgroundLuminance luminance of the wallpaper region behind the label, when known. Drives
 *   the §3 contrast floor: the label gets exactly the scrim it needs and nothing more.
 */
@Composable
fun AppIcon(
    icon: ImageBitmap?,
    label: String,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    labelGap: Dp = 4.dp,
    showLabel: Boolean = true,
    labelLines: Int = 1,
    labelColor: Color = Color.White,
    backgroundLuminance: Float? = null,
    cornerPercent: Float = DEFAULT_MASK_PERCENT,
    smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
    wiggle: Boolean = false,
    wigglePhase: Float = 0f,
    /**
     * When false the icon renders and announces but consumes no pointer input — the caller owns the
     * gesture (the home grid's drag layer needs raw events for §5's lift/dwell timings).
     */
    interactive: Boolean = true,
    /** §9: non-drag equivalents (move/remove/app info) surfaced to TalkBack and Switch Access. */
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    externallyPressed: Boolean = false,
    onClick: (Rect) -> Unit,
    onLongPress: ((Rect) -> Unit)? = null,
) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val typography = LocalTypography.current

    var bounds by remember { mutableStateOf(Rect()) }
    var pressed by remember { mutableStateOf(false) }

    // §5: the icon scales to 1.08 on lift, with the micro token.
    val pressScale by animateFloatAsState(
        targetValue = if (pressed || externallyPressed) PRESS_SCALE else 1f,
        animationSpec = motion.micro(),
        label = "iconPress",
    )

    // §5: ±1.5° at ~0.9Hz, each icon phase-offset by its identity — uniform phase looks wrong.
    val wiggleAngle = if (wiggle) {
        val offset = (label.hashCode() and 0xFF) / 255f
        sin((wigglePhase + offset) * TWO_PI) * WIGGLE_DEGREES
    } else {
        0f
    }

    Column(
        modifier = modifier
            // §9: one merged node per icon, so TalkBack reads it once as a button, and both actions
            // have non-drag equivalents.
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                onClick(label = "Open $label") {
                    onClick(bounds)
                    true
                }
                onLongClick(label = "Options") {
                    onLongPress?.invoke(bounds)
                    onLongPress != null
                }
                if (accessibilityActions.isNotEmpty()) customActions = accessibilityActions
            }
            .onGloballyPositioned { coords ->
                val position = coords.positionInWindow()
                bounds = Rect(
                    position.x.roundToInt(),
                    position.y.roundToInt(),
                    (position.x + coords.size.width).roundToInt(),
                    (position.y + coords.size.height).roundToInt(),
                )
            }
            .then(
                if (interactive) {
                    Modifier.launcherPressable(
                        onPressChange = { pressed = it },
                        onClick = { onClick(bounds) },
                        onLongPress = onLongPress?.let { action ->
                            {
                                haptics.lift()
                                action(bounds)
                            }
                        },
                    )
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(labelGap),
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .scale(pressScale)
                .rotate(wiggleAngle)
                .drawWithCache {
                    val geometry = size
                    val mask = Superellipse.path(
                        size = geometry,
                        radius = minOf(geometry.width, geometry.height) * cornerPercent,
                        smoothness = smoothness,
                    )
                    onDrawBehind {
                        val image = icon ?: return@onDrawBehind
                        clipPath(mask) {
                            drawImage(
                                image = image,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(image.width, image.height),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(
                                    geometry.width.roundToInt().coerceAtLeast(1),
                                    geometry.height.roundToInt().coerceAtLeast(1),
                                ),
                            )
                        }
                    }
                },
        )

        if (showLabel) {
            // §3's contrast floor: over a bright wallpaper this flips the label dark rather than
            // stacking on scrim until it reads as a bar.
            val contrast = backgroundLuminance
                ?.let { ContrastScrim.resolveLabelContrast(it, labelColor) }
                ?: ContrastScrim.LabelContrast(labelColor, Color.Transparent, Float.MAX_VALUE)
            val scrim = contrast.scrim
            BasicText(
                text = label,
                modifier = Modifier
                    .widthIn(max = iconSize * LABEL_WIDTH_RATIO)
                    .then(
                        if (scrim != Color.Transparent) {
                            Modifier.drawWithCache {
                                val pill = Superellipse.path(size, size.height / 2f, smoothness)
                                onDrawBehind { drawPath(pill, scrim) }
                            }
                        } else {
                            Modifier
                        },
                    ),
                style = typography.iconLabel.copy(
                    color = contrast.textColor,
                    textAlign = TextAlign.Center,
                ),
                maxLines = labelLines.coerceIn(1, 2),
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A bare masked icon with no label or interaction — folder previews, drag shadows, pickers. */
@Composable
fun MaskedIcon(
    icon: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerPercent: Float = DEFAULT_MASK_PERCENT,
    smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val geometry = this.size
                val mask = Superellipse.path(
                    size = geometry,
                    radius = minOf(geometry.width, geometry.height) * cornerPercent,
                    smoothness = smoothness,
                )
                onDrawBehind {
                    val image = icon ?: return@onDrawBehind
                    clipPath(mask) {
                        drawImage(
                            image = image,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(image.width, image.height),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(
                                geometry.width.roundToInt().coerceAtLeast(1),
                                geometry.height.roundToInt().coerceAtLeast(1),
                            ),
                        )
                    }
                }
            },
    )
}

/** Letter tile for apps with no usable icon, and the fallback for a missing monochrome layer. */
@Composable
fun LetterTile(
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    cornerPercent: Float = DEFAULT_MASK_PERCENT,
    smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
) {
    val typography = LocalTypography.current
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val geometry = this.size
                val mask = Superellipse.path(
                    size = geometry,
                    radius = minOf(geometry.width, geometry.height) * cornerPercent,
                    smoothness = smoothness,
                )
                onDrawBehind { drawPath(mask, background) }
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label.firstOrNull()?.uppercase() ?: "?",
            style = typography.capsuleTitle.copy(color = foreground),
        )
    }
}

const val DEFAULT_MASK_PERCENT = 0.34f
private const val PRESS_SCALE = 1.08f
private const val WIGGLE_DEGREES = 1.5f
private const val LABEL_WIDTH_RATIO = 1.7f
private const val TWO_PI = 6.2831855f
