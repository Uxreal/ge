package dev.lumen.launcher.feature.capsule.ui

import android.app.PendingIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import dev.lumen.launcher.feature.capsule.CapsuleCard
import dev.lumen.launcher.feature.capsule.CapsuleDeck
import dev.lumen.launcher.feature.capsule.CapsuleGlyph
import dev.lumen.launcher.feature.capsule.CapsuleState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The Capsule (§1, §4): a camera-black pill that docks ON the punch-hole cutout, not under it.
 *
 * The material is deliberately opaque. §4 says GLANCE "grows around the cutout", and the only way
 * a pill absorbs a camera is by matching it — near-black, so the hole reads as part of the surface
 * instead of a hole punched through it. A frosted lens can never do that, and dropping the frost
 * also frees the backdrop recorder whenever no sheet is open (D25).
 *
 * Motion: the header row (text left of the camera, glyph right of it) never changes between
 * states. Expansion is ONE coordinated move — the container widens and the card slides out from
 * under the header on the same `morph` spring — instead of three animations racing (D26). Pressing
 * squishes the pill 4% on the `micro` spring, which is most of what "feels fast" means.
 */
@Composable
fun CapsuleHost(
    deck: CapsuleDeck,
    modifier: Modifier = Modifier,
    onPin: (String) -> Unit = {},
    onDismiss: (CapsuleCard) -> Unit = {},
    onLaunch: (PendingIntent) -> Unit = {},
) {
    val motion = LocalMotion.current
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val view = LocalView.current
    val front = deck.front

    var expanded by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }

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

    // Where the pill docks. Resolved once after attach through the pure, tested policy in
    // [CapsuleGeometry]: a small centred punch-hole is embraced (the pill swallows the camera);
    // notches, corner holes and cutout-less screens get a plain pill under the status bar.
    var geometry by remember { mutableStateOf<PillGeometry?>(null) }
    LaunchedEffect(view) {
        repeat(20) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                val rect = insets.displayCutout?.boundingRectTop
                val width = view.width.takeIf { it > 0 } ?: view.resources.displayMetrics.widthPixels
                geometry = with(density) {
                    CapsuleGeometry.resolve(
                        cutout = rect?.let { CutoutRect(it.left, it.top, it.right, it.bottom) },
                        screenWidthPx = width,
                        statusBarPx = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top,
                        holeMarginPx = HOLE_MARGIN.toPx(),
                        holeBreathPx = HOLE_BREATH.toPx(),
                        fallbackHeightPx = FALLBACK_HEIGHT.toPx(),
                        fallbackGapPx = FALLBACK_GAP.toPx(),
                        fallbackTopGapPx = 4.dp.roundToPx(),
                        minTopPx = 2.dp.roundToPx(),
                        maxHolePx = MAX_HOLE.roundToPx(),
                    )
                }
                return@LaunchedEffect
            }
            delay(50)
        }
    }
    val resolved = geometry ?: return

    val pillHeight = with(density) { resolved.heightPx.toDp() }
    val topOffsetPx = resolved.topPx
    val xOffsetPx = resolved.xOffsetPx
    val gap = with(density) { resolved.gapPx.toDp() }

    val configuration = LocalConfiguration.current
    val expandedWidth = min(configuration.screenWidthDp - 32, 356).dp

    AnimatedVisibility(
        visible = deck.state != CapsuleState.DORMANT && front != null,
        enter = fadeIn(motion.crossfade()),
        exit = fadeOut(motion.crossfade()),
        modifier = modifier,
    ) {
        val card = front ?: return@AnimatedVisibility
        val pressScale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = motion.micro(),
            label = "capsule-press",
        )

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .offset { IntOffset(xOffsetPx + dragOffset.x.roundToInt(), topOffsetPx) }
                .graphicsLayer {
                    translationY = dragOffset.y
                    scaleX = pressScale
                    scaleY = pressScale
                },
        ) {
            // Deck depth: slivers of the cards behind, peeking from under the pill's bottom edge.
            deck.cards.drop(1).take(MAX_SHOULDERS).forEachIndexed { index, _ ->
                Shoulder(depth = index + 1)
            }

            CapsulePill(
                card = card,
                expanded = expanded,
                showActions = showActions,
                deckSize = deck.cards.size,
                pillHeight = pillHeight,
                gap = gap,
                expandedWidth = expandedWidth,
                onLaunch = onLaunch,
                modifier = Modifier
                    .capsuleGestures(
                        deck = deck,
                        expanded = expanded,
                        onPressed = { pressed = it },
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
            )
        }
    }
}

/**
 * The pill. One column: a header row that persists across states, and a card that slides out
 * from under it when expanded. The camera gap stays dead-centre in both states because
 * [CutoutRow] balances its sides.
 */
@Composable
private fun CapsulePill(
    card: CapsuleCard,
    expanded: Boolean,
    showActions: Boolean,
    deckSize: Int,
    pillHeight: Dp,
    gap: Dp,
    expandedWidth: Dp,
    onLaunch: (PendingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val smoothness = Superellipse.DEFAULT_SMOOTHNESS

    // The accent must survive a camera-black background whatever the theme picked.
    val accent = remember(colors.primary, card.accentArgb) {
        val themed = if (card.accentArgb != 0) {
            lerp(colors.primary, Color(card.accentArgb), 0.55f)
        } else {
            colors.primary
        }
        if (themed.luminance() < 0.30f) lerp(themed, Color.White, 0.45f) else themed
    }

    val corner by animateDpAsState(
        targetValue = if (expanded) EXPANDED_CORNER else pillHeight / 2,
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
    val rimStroke = with(density) { 1.5.dp.toPx() }

    Column(
        modifier = modifier
            .then(if (expanded) Modifier.width(expandedWidth) else Modifier)
            .drawWithCache {
                val cornerPx = corner.toPx().coerceAtMost(size.minDimension / 2f)
                val path = Superellipse.path(size, cornerPx, smoothness)
                onDrawBehind {
                    drawPath(path, PILL_FILL)
                    drawPath(path, PILL_EDGE, style = Stroke(width = 1f))
                    if (determinate != null || comet != null) {
                        drawRimProgress(
                            path = path,
                            cornerPx = cornerPx,
                            fraction = determinate,
                            comet = comet,
                            color = accent,
                            strokeWidth = rimStroke,
                        )
                    }
                }
            }
            .animateContentSize(motion.morph<IntSize>())
            .semantics {
                contentDescription = buildString {
                    append(card.title)
                    if (card.subtitle.isNotEmpty()) append(", ").append(card.subtitle)
                    if (deckSize > 1) append(", 1 of ").append(deckSize)
                }
            },
    ) {
        // The header: never swaps, never rescales. Time sits left of the camera, glyph right.
        CutoutRow(
            gap = gap,
            modifier = Modifier
                .then(if (expanded) Modifier.fillMaxWidth() else Modifier)
                .height(pillHeight),
            left = {
                BasicText(
                    text = card.collapsedText.ifEmpty { card.title },
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = typography.capsuleGlance.copy(color = PILL_TEXT),
                    modifier = Modifier.padding(start = 14.dp, end = 8.dp),
                )
            },
            right = {
                Glyph(
                    glyph = card.glyph,
                    tint = accent,
                    boxSize = GLYPH_SIZE,
                    modifier = Modifier.padding(start = 8.dp, end = 14.dp),
                )
            },
        )

        // The card, sliding out from under the header on the same spring the width rides.
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(motion.morph()) + fadeIn(motion.crossfade()),
            exit = shrinkVertically(motion.morph()) + fadeOut(motion.crossfade()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (card.title.isNotEmpty() && card.title != card.collapsedText) {
                    BasicText(
                        text = card.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = typography.capsuleTitle.copy(color = PILL_TEXT),
                    )
                }
                if (card.subtitle.isNotEmpty()) {
                    BasicText(
                        text = card.subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = typography.body.copy(color = PILL_TEXT_DIM),
                    )
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
    }
}

/**
 * A row whose central gap stays exactly centred no matter how the two sides differ — the gap is
 * where the camera lives. Both sides get the width of the wider one, so the composition is
 * symmetric by construction rather than by hoping the content balances.
 */
@Composable
private fun CutoutRow(
    gap: Dp,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            Box(contentAlignment = Alignment.CenterEnd) { left() }
            Box(contentAlignment = Alignment.CenterStart) { right() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val forced = constraints.minWidth > 0
        val sideBudget = if (forced) {
            ((constraints.minWidth - gapPx) / 2).coerceAtLeast(0)
        } else {
            (constraints.maxWidth - gapPx).coerceAtLeast(0) / 2
        }
        val loose = Constraints(maxWidth = sideBudget, maxHeight = constraints.maxHeight)
        val l = measurables[0].measure(loose)
        val r = measurables[1].measure(loose)

        val side = if (forced) sideBudget else maxOf(l.width, r.width)
        val width = if (forced) constraints.minWidth else 2 * side + gapPx
        val height = constraints.minHeight.coerceAtLeast(maxOf(l.height, r.height))

        layout(width, height) {
            l.placeRelative(side - l.width, (height - l.height) / 2)
            r.placeRelative(side + gapPx, (height - r.height) / 2)
        }
    }
}

/** A card behind the front one: the same silhouette, narrowed, peeking below the bottom edge. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Shoulder(depth: Int) {
    val smoothness = Superellipse.DEFAULT_SMOOTHNESS
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = 1f - SHOULDER_INSET * depth
                translationY = SHOULDER_DROP_DP * depth * this.density
                alpha = 0.8f / depth
            }
            .drawWithCache {
                val path = Superellipse.path(size, size.height / 2f, smoothness)
                onDrawBehind { drawPath(path, SHOULDER_FILL) }
            },
    )
}

@Composable
private fun ActionChip(label: String, accent: Color, onClick: () -> Unit) {
    val typography = LocalTypography.current
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.16f), MaterialTheme.shapes.small)
            .pointerInput(label) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        BasicText(text = label, style = typography.tileLabel.copy(color = accent))
    }
}

@Composable
private fun Glyph(glyph: CapsuleGlyph, tint: Color, boxSize: Dp, modifier: Modifier = Modifier) {
    // A pushed `iconUri` is not decoded here — §5 forbids main-thread decodes and the Capsule has
    // no worker, so an image glyph renders as the push mark until the shell resolves it.
    val symbol = when (glyph) {
        is CapsuleGlyph.Builtin -> glyph.symbol
        is CapsuleGlyph.Image -> BuiltinSymbol.SPARK
        CapsuleGlyph.None -> return
    }
    Box(
        modifier = modifier
            .size(boxSize)
            .drawWithCache {
                onDrawBehind { drawBuiltinSymbol(symbol, tint, size.minDimension) }
            },
    )
}

/**
 * §4's gestures on one node. Both axes are rubber-banded — the pill is docked to a camera and must
 * never appear to leave it; a swipe nudges it a few dp and springs back while the deck reorders
 * underneath. The dominant axis at release decides what happened.
 *
 * Drag-down-to-peel (§4) is deliberately absent (D22): the peeled home card is Phase 3, and a
 * gesture that starts something it cannot finish is worse than none. Drag down expands instead.
 */
private fun Modifier.capsuleGestures(
    deck: CapsuleDeck,
    expanded: Boolean,
    onPressed: (Boolean) -> Unit,
    onOffset: (Offset) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onShuffle: (String) -> Unit,
    onDismiss: () -> Unit,
): Modifier = this
    .pointerInput(deck.signature) {
        detectTapGestures(
            onPress = {
                onPressed(true)
                tryAwaitRelease()
                onPressed(false)
            },
            onTap = { onTap() },
            onLongPress = { onLongPress() },
        )
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
                onOffset(Offset(rubberBand(total.x, X_BAND_PX), rubberBand(total.y, Y_BAND_PX)))
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

/** Progressive resistance toward a hard limit — the dock made tactile. */
private fun rubberBand(raw: Float, limitPx: Float): Float =
    sign(raw) * limitPx * (1f - 1f / (1f + abs(raw) / limitPx))

// Camera-black, matching the punch-hole it swallows. Not theme-dependent: the camera has no theme.
private val PILL_FILL = Color(0xFA0B0B0D)
private val PILL_EDGE = Color(0x1AFFFFFF)
private val PILL_TEXT = Color(0xF5FFFFFF)
private val PILL_TEXT_DIM = Color(0x9EFFFFFF)
private val SHOULDER_FILL = Color(0xE617171B)

private const val MAX_SHOULDERS = 2
private const val SHOULDER_INSET = 0.07f
private const val SHOULDER_DROP_DP = 3.5f
private const val COMET_PERIOD_MS = 1_400
private const val AUTO_COLLAPSE_MS = 2_500L
private const val X_BAND_PX = 36f
private const val Y_BAND_PX = 64f

/** Material above and below the hole; the pill's height derives from the hole, not a constant. */
private val HOLE_MARGIN = 5.dp

/** Air between the hole and the nearest content on each side. */
private val HOLE_BREATH = 7.dp

/** Anything taller than this is a notch wearing a costume, not a punch-hole. */
private val MAX_HOLE = 44.dp

private val FALLBACK_HEIGHT = 32.dp
private val FALLBACK_GAP = 10.dp
private val EXPANDED_CORNER = 24.dp
private val GLYPH_SIZE = 14.dp
private val SHUFFLE_THRESHOLD = 40.dp
private val DISMISS_THRESHOLD = 42.dp
