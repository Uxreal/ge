package dev.lumen.launcher.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import dev.lumen.launcher.data.prefs.MotionSettings

/**
 * Motion tokens for the whole launcher.
 *
 * Apple's home screen is spring-driven — nothing eases, everything settles — while Material's
 * emphasised curves own the surfaces that appear and disappear. Lumen keeps both: [spatial] for
 * anything that moves through space, [effect] for fades and colour, both scaled by the user's
 * speed slider and collapsed to near-instant when Reduce Motion is on.
 */
@Immutable
data class MotionSpec(
    val speed: Float = 1f,
    val stiffness: Float = 380f,
    val damping: Float = 0.82f,
    val reduceMotion: Boolean = false,
    val parallaxDepth: Float = 0.4f,
    val iconPressScale: Float = 0.9f,
    val blurDuringTransitions: Boolean = true,
) {
    private val scaledStiffness: Float get() = (stiffness * speed).coerceIn(20f, 6000f)

    fun <T> spatial(): SpringSpec<T> = spring(
        dampingRatio = damping.coerceIn(0.2f, 2f),
        stiffness = if (reduceMotion) 3000f else scaledStiffness,
    )

    fun <T> effect(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = if (reduceMotion) 3000f else scaledStiffness * 1.5f,
    )

    /** Snappier spring for press/release feedback. */
    fun <T> press(): SpringSpec<T> = spring(
        dampingRatio = 0.7f,
        stiffness = if (reduceMotion) 4000f else scaledStiffness * 3f,
    )

    fun <T> quick(): FiniteAnimationSpec<T> =
        tween(durationMillis = if (reduceMotion) 0 else (170f / speed).toInt().coerceIn(40, 900))

    fun <T> emphasised(): FiniteAnimationSpec<T> =
        tween(durationMillis = if (reduceMotion) 0 else (420f / speed).toInt().coerceIn(60, 1600))

    val pageSpring: AnimationSpec<Float> get() = spatial()

    companion object {
        fun from(settings: MotionSettings) = MotionSpec(
            speed = settings.speed.coerceIn(0.25f, 3f),
            stiffness = settings.springStiffness,
            damping = settings.springDamping,
            reduceMotion = settings.reduceMotion,
            parallaxDepth = settings.parallaxDepth,
            iconPressScale = settings.iconPressScale,
            blurDuringTransitions = settings.blurDuringTransitions,
        )
    }
}

val LocalMotion = staticCompositionLocalOf { MotionSpec() }
