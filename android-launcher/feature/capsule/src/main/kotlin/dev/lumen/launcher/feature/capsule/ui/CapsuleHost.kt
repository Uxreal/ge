package dev.lumen.launcher.feature.capsule.ui

import android.app.PendingIntent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.surface.BackdropCapture
import dev.lumen.launcher.core.design.surface.LocalFrostedTokens
import dev.lumen.launcher.core.design.surface.SurfaceRole
import dev.lumen.launcher.core.design.surface.frosted
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import dev.lumen.launcher.feature.capsule.CapsuleCard
import dev.lumen.launcher.feature.capsule.CapsuleDeck
import dev.lumen.launcher.feature.capsule.CapsuleGlyph
import dev.lumen.launcher.feature.capsule.CapsuleState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sign

/**
 * The Capsule (§1, §4) — the single pill under the status bar that is the launcher's only handle on
 * what the device is doing.
 *
 * Three deliberate departures from the shape everyone else ships (§1.1 names the flat black rounded
 * rectangle as an anti-default):
 *
 *  * **The outline is the progress indicator.** No bar is added inside the pill; a segment of the
 *    superellipse silhouette itself is stroked. See [drawRimProgress].
 *  * **The deck is drawn as shoulders, not as stacked cards.** Cards behind the front one appear as
 *    narrower slivers emerging from underneath it, so depth reads at a glance without a second
 *    unreadable pill competing for attention.
 *  * **The container morphs; the text cross-fades.** §1.1 forbids scaling type during a container
 *    morph, so width, height and corner radius animate on `morph` while content swaps on the 120ms
 *    cross-fade.
 */
@Composable
fun CapsuleHost(
    deck: CapsuleDeck,
    capture: BackdropCapture?,
    modifier: Modifier = Modifier,
    onPin: (String) -> Unit = {},
    onDismiss: (CapsuleCard) -> Unit = {},
    onLaunch: (PendingIntent) -> Unit = {},
) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val front = deck.front

    var expanded by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // §4: a source arriving at ≥800 expands itself once, for 2.5s, then settles back to GLANCE.
    LaunchedEffect(deck.signature, deck.autoExpand) {
        if (deck.autoExpand) {
            haptics.state()
            expanded = true
            delay(AUTO_COLLAPSE_MS)
            expanded = false
            showActions = false
        }
    }
    if (deck.cards.isEmpty() && expanded) {
        expanded = false
        showActions = false
    }

    // A frosted surface cannot know that the pixels behind it moved, so the Capsule holds a redraw
    // ticket while its own geometry is in motion — and releases it the moment it settles.
    val animating = expanded || dragOffset != Offset.Zero
    DisposableEffect(capture, animating) {
        capture?.setAnimating(TICKET, animating)
        onDispose { capture?.setAnimating(TICKET, false) }
    }

    AnimatedVisibility(
        visible = deck.state != CapsuleState.DORMANT && front != null,
        enter = fadeIn(motion.crossfade()),
        exit = fadeOut(motion.crossfade()),
        modifier = modifier,
    ) {
        val card = front ?: return@AnimatedVisibility

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dragOffset.x
                        translationY = dragOffset.y
                        // A card being flicked away fades as it goes; one dragged down to expand
                        // does not.
                        alpha = 1f - (-dragOffset.y / DISMISS_FADE_PX).coerceIn(0f, 0.7f)
                    }
                    .capsuleGestures(
                        deck = deck,
                        expanded = expanded,
                        onOffset = { dragOffset = it },
                        onTap = {
                            haptics.state()
                            expanded = !expanded
                            if (!expanded) showActions = false
                        },
                        onLongPress = {
                            haptics.lift()
                            expanded = true
                            showActions = true
                        },
                        onShuffle = { key ->
                            haptics.state()
                            onPin(key)
                        },
                        onDismiss = {
                            haptics.commit()
                            onDismiss(card)
                        },
                    ),
            ) {
                // The shoulders: sized to the front pill, narrowed and pushed down. Declared first
                // so they draw behind it.
                deck.cards.drop(1).take(MAX_SHOULDERS).forEachIndexed { index, behind ->
                    Shoulder(depth = index + 1, accentArgb = behind.accentArgb)
                }

                CapsuleBody(
                    card = card,
                    expanded = expanded,
                    showActions = showActions,
                    deckSize = deck.cards.size,
                    capture = capture,
                    onLaunch = onLaunch,
                )
            }

            if (deck.cards.size > 1) {
                val onSurface = MaterialTheme.colorScheme.onSurface
                val dotPx = with(density) { 4.dp.toPx() }
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .widthIn(min = 48.dp)
                        .drawBehind {
                            drawDotRail(
                                count = deck.cards.size,
                                activeIndex = 0,
                                color = onSurface,
                                dot = dotPx,
                                gap = dotPx,
                            )
                        },
                )
            }
        }
    }
}

/**
 * A card behind the front one. Deliberately not a smaller copy of the pill: the same silhouette,
 * narrowed and dropped a few dp, so the deck reads as thickness rather than as a pile of competing
 * cards.
 */
@Composable
private fun BoxScope.Shoulder(depth: Int, accentArgb: Int) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val tint = if (accentArgb != 0) lerp(base, Color(accentArgb), 0.35f) else base
    val smoothness = Superellipse.DEFAULT_SMOOTHNESS

    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = 1f - SHOULDER_INSET * depth
                translationY = SHOULDER_DROP_DP * depth * this.density
                alpha = 0.9f / depth
            }
            .drawWithCache {
                val path = Superellipse.path(size, size.height / 2f, smoothness)
                onDrawBehind { drawPath(path, tint.copy(alpha = 0.6f)) }
            },
    )
}

/** The pill itself: frosted container, glyph, text, and the rim as the progress indicator. */
@Composable
private fun CapsuleBody(
    card: CapsuleCard,
    expanded: Boolean,
    showActions: Boolean,
    deckSize: Int,
    capture: BackdropCapture?,
    onLaunch: (PendingIntent) -> Unit,
) {
    val motion = LocalMotion.current
    val colors = MaterialTheme.colorScheme
    val tokens = LocalFrostedTokens.current[SurfaceRole.CAPSULE]
    val smoothness = Superellipse.DEFAULT_SMOOTHNESS

    // §4.1: an accent is blended toward the theme, never used raw — a source cannot repaint the
    // launcher's chrome whatever colour it likes.
    val accent = if (card.accentArgb != 0) {
        lerp(colors.primary, Color(card.accentArgb), 0.55f)
    } else {
        colors.primary
    }

    val corner by animateDpAsState(
        targetValue = if (expanded) EXPANDED_CORNER else GLANCE_HEIGHT / 2,
        animationSpec = motion.morph(),
        label = "capsule-corner",
    )

    val comet = if (card.progress == CapsuleCard.INDETERMINATE) {
        rememberInfiniteTransition(label = "capsule-comet").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(COMET_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "comet",
        ).value
    } else {
        null
    }
    val determinate = card.progress.takeIf { it in 0f..1f }
    val rimStroke = with(LocalDensity.current) { 2.dp.toPx() }

    // Draw modifiers come before `animateContentSize` so they see the animated size and the
    // silhouette grows with the container instead of snapping to its final width.
    val painted = if (tokens != null && capture != null) {
        Modifier.frosted(capture, tokens, cornerRadius = corner, smoothness = smoothness)
    } else {
        Modifier.drawWithCache {
            val path = Superellipse.path(size, corner.toPx(), smoothness)
            onDrawBehind { drawPath(path, colors.surface.copy(alpha = 0.94f)) }
        }
    }

    Box(
        modifier = painted
            .drawBehind {
                if (determinate == null && comet == null) return@drawBehind
                drawRimProgress(
                    path = Superellipse.path(size, corner.toPx(), smoothness),
                    cornerPx = corner.toPx().coerceAtMost(size.minDimension / 2f),
                    fraction = determinate,
                    comet = comet,
                    color = accent,
                    strokeWidth = rimStroke,
                )
            }
            .animateContentSize(motion.morph<IntSize>())
            .heightIn(min = GLANCE_HEIGHT)
            .widthIn(
                min = GLANCE_MIN_WIDTH,
                max = if (expanded) EXPANDED_MAX_WIDTH else GLANCE_MAX_WIDTH,
            )
            .then(if (expanded) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            .semantics {
                contentDescription = buildString {
                    append(card.title)
                    if (card.subtitle.isNotEmpty()) append(", ").append(card.subtitle)
                    if (deckSize > 1) append(", 1 of ").append(deckSize)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = { fadeIn(motion.crossfade()) togetherWith fadeOut(motion.crossfade()) },
            label = "capsule-content",
        ) { isExpanded ->
            if (isExpanded) {
                ExpandedContent(card, accent, showActions, onLaunch)
            } else {
                GlanceContent(card, accent)
            }
        }
    }
}

@Composable
private fun GlanceContent(card: CapsuleCard, accent: Color) {
    val typography = LocalTypography.current
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(card.glyph, accent, GLYPH_GLANCE)
        BasicText(
            text = card.collapsedText.ifEmpty { card.title },
            maxLines = 1,
            overflow = TextOverflow.Clip,
            style = typography.capsuleGlance.copy(color = onSurface),
        )
    }
}

@Composable
private fun ExpandedContent(
    card: CapsuleCard,
    accent: Color,
    showActions: Boolean,
    onLaunch: (PendingIntent) -> Unit,
) {
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(card.glyph, accent, GLYPH_EXPANDED)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = card.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = typography.capsuleTitle.copy(color = colors.onSurface),
                )
                if (card.subtitle.isNotEmpty()) {
                    BasicText(
                        text = card.subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = typography.tileLabel.copy(color = colors.onSurfaceVariant),
                    )
                }
            }
        }

        if (showActions && card.actions.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                card.actions.take(3).forEach { action ->
                    ActionChip(
                        label = action.label,
                        accent = accent,
                        onClick = { action.intent?.let(onLaunch) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, accent: Color, onClick: () -> Unit) {
    val typography = LocalTypography.current
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.18f), MaterialTheme.shapes.small)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        BasicText(text = label, style = typography.tileLabel.copy(color = accent))
    }
}

@Composable
private fun Glyph(glyph: CapsuleGlyph, accent: Color, boxSize: Dp) {
    // A pushed `iconUri` is not decoded here: §5 forbids main-thread decodes, and the Capsule has
    // no worker of its own, so an image glyph renders as the push mark until the shell resolves it.
    val symbol = when (glyph) {
        is CapsuleGlyph.Builtin -> glyph.symbol
        is CapsuleGlyph.Image -> BuiltinSymbol.SPARK
        CapsuleGlyph.None -> return
    }
    Box(
        modifier = Modifier
            .size(boxSize)
            .drawBehind { drawBuiltinSymbol(symbol, accent, size.minDimension) },
    )
}

/**
 * §4's gesture set on one detector, because horizontal and vertical recognisers on the same node
 * fight over the first event. The dominant axis at release decides what happened.
 *
 * Drag-down-to-peel (§4) is not here: the peeled home card is a Phase 3 item and shipping half of
 * it would leave a gesture that starts something it cannot finish. Drag down expands instead.
 */
private fun Modifier.capsuleGestures(
    deck: CapsuleDeck,
    expanded: Boolean,
    onOffset: (Offset) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onShuffle: (String) -> Unit,
    onDismiss: () -> Unit,
): Modifier = this
    .pointerInput(deck.signature) {
        detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
    }
    .pointerInput(deck.signature, expanded) {
        val shuffleThreshold = SHUFFLE_THRESHOLD.toPx()
        val dismissThreshold = DISMISS_THRESHOLD.toPx()
        var total = Offset.Zero

        detectDragGestures(
            onDragStart = { total = Offset.Zero },
            onDragCancel = {
                total = Offset.Zero
                onOffset(Offset.Zero)
            },
            onDrag = { change, amount ->
                total += amount
                change.consume()
                // Horizontal follows 1:1 (§3's `follow`); vertical is rubber-banded, because the
                // pill is docked and pretending otherwise makes the dock feel broken.
                onOffset(Offset(total.x, rubberBand(total.y)))
            },
            onDragEnd = {
                val horizontal = abs(total.x) > abs(total.y)
                when {
                    horizontal && abs(total.x) > shuffleThreshold && deck.cards.size > 1 -> {
                        val next = if (total.x < 0) deck.cards[1] else deck.cards.last()
                        onShuffle(next.dedupeKey)
                    }

                    !horizontal && total.y < -dismissThreshold ->
                        if (deck.front?.dismissible == true) onDismiss()

                    !horizontal && total.y > dismissThreshold && !expanded -> onTap()
                }
                total = Offset.Zero
                onOffset(Offset.Zero)
            },
        )
    }

/** Progressive resistance: the first few dp track the finger, the rest compress toward a limit. */
private fun rubberBand(raw: Float): Float {
    val limit = 64f
    return sign(raw) * limit * (1f - 1f / (1f + abs(raw) / limit))
}

private const val TICKET = "capsule"
private const val MAX_SHOULDERS = 2
private const val SHOULDER_INSET = 0.06f
private const val SHOULDER_DROP_DP = 4f
private const val COMET_PERIOD_MS = 1_400
private const val AUTO_COLLAPSE_MS = 2_500L
private const val DISMISS_FADE_PX = 90f

private val GLANCE_HEIGHT = 34.dp
private val EXPANDED_CORNER = 26.dp
private val GLANCE_MIN_WIDTH = 96.dp
private val GLANCE_MAX_WIDTH = 260.dp
private val EXPANDED_MAX_WIDTH = 420.dp
private val GLYPH_GLANCE = 15.dp
private val GLYPH_EXPANDED = 26.dp
private val SHUFFLE_THRESHOLD = 40.dp
private val DISMISS_THRESHOLD = 42.dp
