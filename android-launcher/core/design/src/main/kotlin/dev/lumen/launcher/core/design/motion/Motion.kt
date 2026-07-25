package dev.lumen.launcher.core.design.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The complete motion vocabulary. §3: *every* animation in the app uses one of these tokens — no
 * bespoke durations except cross-fades.
 *
 * | Token       | Stiffness / Damping        | Overshoot |
 * |-------------|----------------------------|-----------|
 * | [morph]     | 380 / 0.78                 | yes       |
 * | [enter]     | 260 / 0.82                 | yes       |
 * | [page]      | 700 / 1.0                  | **no**    |
 * | [micro]     | 1200 / 0.90                | no        |
 * | `follow`    | none — 1:1 with the finger | —         |
 * | [crossfade] | 120ms linear               | —         |
 *
 * Only [morph] and [enter] may overshoot. [page] is critically damped on purpose: bouncy horizontal
 * paging is named as an anti-default in §1.1.
 */
@Immutable
data class MotionTokens(
    /** §7 global animation speed, 0.5×–1.5×. Higher is faster. */
    val speed: Float = 1f,
    /**
     * §3: reduce-motion replaces every spring with [crossfade] and disables wallpaper parallax, and
     * must still feel deliberate — hence a real 120ms cross-fade rather than a zero-duration snap.
     */
    val reduceMotion: Boolean = false,
) {
    private fun stiffness(base: Float): Float = base * speed.coerceIn(0.5f, 1.5f)

    /** Capsule state changes, folder open/close. */
    fun <T> morph(): FiniteAnimationSpec<T> = springOrCrossfade(MORPH_STIFFNESS, MORPH_DAMPING)

    /** Sheets, control panel, search. */
    fun <T> enter(): FiniteAnimationSpec<T> = springOrCrossfade(ENTER_STIFFNESS, ENTER_DAMPING)

    /** Horizontal paging and drawer scroll settle. Critically damped — never overshoots. */
    fun <T> page(): FiniteAnimationSpec<T> = springOrCrossfade(PAGE_STIFFNESS, PAGE_DAMPING)

    /** Toggles, badges, wiggle-mode entry. */
    fun <T> micro(): FiniteAnimationSpec<T> = springOrCrossfade(MICRO_STIFFNESS, MICRO_DAMPING)

    /** Any text or icon content swap — and the whole animation system under reduce-motion. */
    fun <T> crossfade(): FiniteAnimationSpec<T> =
        tween(durationMillis = CROSSFADE_MS, easing = LinearEasing)

    /** The raw spring, for callers that need to hand a release velocity to [Animatable.animateTo]. */
    fun <T> morphSpring(): SpringSpec<T> =
        spring(dampingRatio = MORPH_DAMPING, stiffness = stiffness(MORPH_STIFFNESS))

    fun <T> pageSpring(): SpringSpec<T> =
        spring(dampingRatio = PAGE_DAMPING, stiffness = stiffness(PAGE_STIFFNESS))

    private fun <T> springOrCrossfade(base: Float, damping: Float): FiniteAnimationSpec<T> =
        if (reduceMotion) {
            crossfade()
        } else {
            spring(dampingRatio = damping, stiffness = stiffness(base))
        }

    companion object {
        const val CROSSFADE_MS = 120

        const val MORPH_STIFFNESS = 380f
        const val MORPH_DAMPING = 0.78f
        const val ENTER_STIFFNESS = 260f
        const val ENTER_DAMPING = 0.82f
        const val PAGE_STIFFNESS = 700f
        const val PAGE_DAMPING = 1f
        const val MICRO_STIFFNESS = 1200f
        const val MICRO_DAMPING = 0.90f
    }
}

/**
 * The `follow` → `morph` handoff from §3: a released drag carries its velocity into the spring
 * rather than restarting from rest, which is what keeps a flick continuous.
 */
suspend fun <T, V : AnimationVector> Animatable<T, V>.releaseToMorph(
    target: T,
    velocity: T,
    tokens: MotionTokens,
) {
    animateTo(
        targetValue = target,
        animationSpec = tokens.morphSpring(),
        initialVelocity = velocity,
    )
}

val LocalMotion = staticCompositionLocalOf { MotionTokens() }
