package com.hermes.voice.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hermes.voice.engine.VoiceState

/**
 * The state of the voice session, as one glanceable object: colour says what
 * mode it is in, the outer ring tracks microphone level while listening, and a
 * slow pulse marks thinking and speaking.
 */
@Composable
fun VoiceOrb(
    state: VoiceState,
    level: Float,
    size: androidx.compose.ui.unit.Dp = 76.dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val target = when (state) {
        VoiceState.IDLE -> scheme.onSurfaceVariant
        VoiceState.LISTENING -> scheme.primary
        VoiceState.THINKING -> scheme.secondary
        VoiceState.SPEAKING -> scheme.tertiary
    }
    val color by animateColorAsState(target, tween(320), label = "orbColor")

    val infinite = rememberInfiniteTransition(label = "orbPulse")
    val pulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )
    val reactive by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f),
        label = "orbLevel",
    )

    val energy = when (state) {
        VoiceState.LISTENING -> 0.25f + reactive * 0.75f
        VoiceState.THINKING -> 0.3f + pulse * 0.35f
        VoiceState.SPEAKING -> 0.35f + pulse * 0.5f
        VoiceState.IDLE -> 0.16f
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val radius = this.size.minDimension / 2f
            val core = radius * (0.42f + energy * 0.16f)

            // Outer halo grows with energy.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.30f * energy + 0.05f), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            // Level ring.
            drawCircle(
                color = color.copy(alpha = 0.35f + energy * 0.35f),
                radius = core * (1.25f + energy * 0.45f),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )
            // Core.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, color.copy(alpha = 0.55f)),
                    center = Offset(center.x - core * 0.25f, center.y - core * 0.3f),
                    radius = core * 1.6f,
                ),
                radius = core,
                center = center,
            )
        }
    }
}
