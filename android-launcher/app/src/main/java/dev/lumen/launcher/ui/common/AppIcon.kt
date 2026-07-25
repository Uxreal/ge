package dev.lumen.launcher.ui.common

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.data.LocalServices
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.prefs.BadgePosition
import dev.lumen.launcher.data.prefs.BadgeStyle
import dev.lumen.launcher.data.prefs.IconShape
import dev.lumen.launcher.ui.LocalController
import dev.lumen.launcher.ui.LocalSettings
import dev.lumen.launcher.ui.glass.LauncherShapes
import dev.lumen.launcher.ui.theme.LocalMotion
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The single icon primitive: home grid, dock, folders, drawer, search results and the settings
 * preview all render through this, so one shape or size setting moves every surface at once.
 *
 * @param onClick receives the icon's bounds on screen, which the system uses as the origin of the
 *   app-open animation.
 */
@Composable
fun AppIcon(
    key: AppKey,
    size: Dp,
    modifier: Modifier = Modifier,
    label: String? = null,
    showLabel: Boolean = true,
    labelColorOverride: Color? = null,
    wiggle: Boolean = false,
    wigglePhase: Float = 0f,
    onClick: (Rect) -> Unit,
    onLongPress: ((Rect) -> Unit)? = null,
) {
    val settings = LocalSettings.current
    val controller = LocalController.current
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val resolvedLabel = label ?: controller.labelFor(key)

    var bounds by remember { mutableStateOf(Rect()) }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) motion.iconPressScale else 1f,
        animationSpec = motion.press(),
        label = "iconPress",
    )
    val wiggleAngle = if (wiggle && settings.motion.wiggleInEditMode) {
        // Offset each icon's phase by its identity so the grid jitters instead of pulsing in unison.
        val offset = (key.flat.hashCode() % 100) / 100f
        sin((wigglePhase + offset) * 2f * Math.PI.toFloat()) * 1.6f
    } else {
        0f
    }

    Column(
        modifier = modifier
            // One node for TalkBack: the label is the icon's description, not two separate reads.
            .semantics(mergeDescendants = true) {
                contentDescription = resolvedLabel
                role = Role.Button
                onClick(label = "Open $resolvedLabel") {
                    onClick(bounds)
                    true
                }
                onLongClick(label = "Options") {
                    onLongPress?.invoke(bounds)
                    onLongPress != null
                }
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
            .launcherPressable(
                onPressChange = { pressed = it },
                onClick = { onClick(bounds) },
                onLongPress = onLongPress?.let { action ->
                    {
                        haptics.longPress()
                        action(bounds)
                    }
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pressScale)
                .rotate(wiggleAngle),
            contentAlignment = Alignment.Center,
        ) {
            IconBitmap(key = key, size = size)
            Badge(key = key, iconSize = size)
        }

        if (showLabel && settings.labels.maxLines > 0) {
            IconLabel(
                text = resolvedLabel,
                colorOverride = labelColorOverride,
                maxWidth = size * 1.7f,
            )
        }
    }
}

/** Loads (and reloads when the icon pipeline's generation changes) the rendered icon bitmap. */
@Composable
fun IconBitmap(key: AppKey, size: Dp, modifier: Modifier = Modifier) {
    val services = LocalServices.current
    val settings = LocalSettings.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }.coerceAtLeast(1)
    val generation by services.icons.generation.collectAsStateWithLifecycle()

    var bitmap by remember(key, sizePx) { mutableStateOf(services.icons.cached(key, sizePx)) }
    LaunchedEffect(key, sizePx, generation) {
        bitmap = services.icons.cached(key, sizePx) ?: services.icons.load(key, sizePx)
    }

    val glassTile = settings.icons.glassTile
    val tileAlpha = settings.icons.glassTileAlpha
    val gloss = settings.icons.gloss
    val shape = settings.icons.shape
    val cornerPercent = settings.icons.cornerRadiusPercent
    val smoothing = settings.icons.squircleSmoothing
    val image = bitmap

    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                // `size` here is the Dp parameter, so the pixel geometry is taken explicitly.
                val geometry = this.size
                val tilePath = LauncherShapes.iconPath(shape, geometry, cornerPercent, smoothing)
                onDrawBehind {
                    if (glassTile) {
                        drawPath(
                            path = tilePath,
                            brush = Brush.linearGradient(
                                0f to Color.White.copy(alpha = 0.22f * tileAlpha),
                                1f to Color.White.copy(alpha = 0.06f * tileAlpha),
                                start = Offset.Zero,
                                end = Offset(geometry.width, geometry.height),
                            ),
                        )
                        drawPath(
                            path = tilePath,
                            color = Color.White.copy(alpha = 0.22f * tileAlpha),
                            style = Stroke(width = 1f),
                        )
                    }
                    if (image != null) {
                        val inset = if (glassTile) geometry.width * 0.12f else 0f
                        drawImage(
                            image = image,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(image.width, image.height),
                            dstOffset = IntOffset(inset.roundToInt(), inset.roundToInt()),
                            dstSize = IntSize(
                                (geometry.width - inset * 2f).roundToInt().coerceAtLeast(1),
                                (geometry.height - inset * 2f).roundToInt().coerceAtLeast(1),
                            ),
                        )
                    }
                    if (gloss > 0.001f) {
                        clipPath(tilePath) { drawGloss(gloss) }
                    }
                }
            },
    )
}

/** Early-iOS sheen. Off by default, but it reads beautifully against the glass surfaces. */
private fun DrawScope.drawGloss(amount: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.38f * amount),
            0.48f to Color.White.copy(alpha = 0.06f * amount),
            0.5f to Color.Transparent,
        ),
        size = Size(size.width, size.height * 0.5f),
    )
}

@Composable
private fun Badge(key: AppKey, iconSize: Dp) {
    val settings = LocalSettings.current
    val services = LocalServices.current
    if (settings.badges.style == BadgeStyle.NONE) return
    if (settings.apps.overrides[key.flat]?.badgesMuted == true) return

    val badges by services.badges.badges.collectAsStateWithLifecycle()
    val count = badges[key.packageName] ?: 0
    if (count <= 0) return

    val accent = MaterialTheme.colorScheme.primary
    val color = if (settings.badges.color == 0) accent else Color(settings.badges.color)
    val base = when (settings.badges.style) {
        BadgeStyle.DOT -> iconSize * 0.20f
        BadgeStyle.DOT_LARGE -> iconSize * 0.30f
        BadgeStyle.COUNT -> iconSize * 0.38f
        BadgeStyle.NONE -> 0.dp
    }
    val diameter = base * settings.badges.size.coerceIn(0.4f, 2f)

    val alignment = when (settings.badges.position) {
        BadgePosition.TOP_RIGHT -> Alignment.TopEnd
        BadgePosition.TOP_LEFT -> Alignment.TopStart
        BadgePosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        BadgePosition.BOTTOM_LEFT -> Alignment.BottomStart
    }

    Box(modifier = Modifier.size(iconSize), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .size(diameter)
                .drawWithCache {
                    val path = LauncherShapes.iconPath(IconShape.CIRCLE, size, 0.5f, 0f)
                    onDrawBehind {
                        drawPath(path, Color.Black.copy(alpha = 0.28f))
                        drawPath(path, color)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (settings.badges.style == BadgeStyle.COUNT) {
                val shown = count.coerceAtMost(settings.badges.maxCount)
                val suffix = if (count > settings.badges.maxCount) "+" else ""
                BasicText(
                    text = "$shown$suffix",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = (diameter.value * 0.5f).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

/** Icon caption, honouring every label setting (size, weight, shadow, caps, colour, lines). */
@Composable
fun IconLabel(
    text: String,
    colorOverride: Color? = null,
    maxWidth: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
) {
    val labels = LocalSettings.current.labels
    val fallback = MaterialTheme.colorScheme.onSurface
    val configured = if (labels.color == 0) fallback else Color(labels.color)
    val color = colorOverride ?: configured.copy(alpha = labels.alpha.coerceIn(0f, 1f))

    BasicText(
        text = if (labels.allCaps) text.uppercase() else text,
        modifier = if (maxWidth != Dp.Unspecified) modifier.widthIn(max = maxWidth) else modifier,
        style = TextStyle(
            color = color,
            fontSize = labels.fontSize.sp,
            fontWeight = FontWeight(labels.fontWeight.coerceIn(100, 900)),
            letterSpacing = labels.letterSpacing.sp,
            textAlign = TextAlign.Center,
            shadow = if (labels.shadow > 0.001f) {
                Shadow(
                    color = Color.Black.copy(alpha = 0.55f * labels.shadow),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f * labels.shadow,
                )
            } else {
                null
            },
        ),
        maxLines = labels.maxLines.coerceAtLeast(1),
        overflow = TextOverflow.Ellipsis,
    )
}
