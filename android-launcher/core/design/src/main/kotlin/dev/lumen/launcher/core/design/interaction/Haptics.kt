package dev.lumen.launcher.core.design.interaction

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * §3 asks for a haptic *vocabulary*, not one buzz: six verbs, one owner, one global intensity.
 *
 * These are predictive by design — [tick] fires as a drag crosses a cell boundary and [lift] as the
 * long-press threshold is reached, during the gesture rather than on release. That is what makes the
 * grid feel like it has physical detents.
 */
@Stable
class Haptics(
    private val view: View,
    private val vibrator: Vibrator?,
    private val intensity: HapticIntensity,
) {
    private val amplitudeControl: Boolean = vibrator?.hasAmplitudeControl() == true

    /** A drag crosses a cell boundary. The lightest thing the actuator can do. */
    fun tick() = play(HapticFeedbackConstants.CLOCK_TICK, amplitude = 0.35f, durationMs = 8)

    /** A drop lands in its cell. */
    fun snap() = play(HapticFeedbackConstants.KEYBOARD_TAP, amplitude = 0.70f, durationMs = 14)

    /** A long-press became a drag: the item is now in the user's hand. */
    fun lift() = play(HapticFeedbackConstants.LONG_PRESS, amplitude = 0.85f, durationMs = 20)

    /** The Capsule changed state. */
    fun state() = play(HapticFeedbackConstants.CONTEXT_CLICK, amplitude = 0.50f, durationMs = 10)

    /** Page bounce at the first or last page. */
    fun edge() = play(HapticFeedbackConstants.GESTURE_END, amplitude = 0.45f, durationMs = 10)

    /** A destructive action was confirmed. The only double-beat in the vocabulary. */
    fun commit() {
        if (intensity == HapticIntensity.OFF) return
        val device = vibrator
        val effect = doubleBeat()
        if (effect != null && device != null) {
            runCatching { device.vibrate(effect) }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    /**
     * Prefers a precisely scaled waveform so intensity is a real dial rather than three presets,
     * and falls back to the platform constant when the device has no amplitude control.
     */
    private fun play(constant: Int, amplitude: Float, durationMs: Int) {
        if (intensity == HapticIntensity.OFF) return
        val scaled = (amplitude * intensity.scale).coerceIn(0f, 1f)
        val device = vibrator
        if (amplitudeControl && device != null) {
            val amp = (scaled * 255f).toInt().coerceIn(1, 255)
            runCatching { device.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), amp)) }
        } else {
            view.performHapticFeedback(constant)
        }
    }

    private fun doubleBeat(): VibrationEffect? {
        if (!amplitudeControl) return null
        val amp = (0.9f * intensity.scale * 255f).toInt().coerceIn(1, 255)
        return VibrationEffect.createWaveform(
            longArrayOf(0, 12, 40, 18),
            intArrayOf(0, amp, 0, amp),
            -1,
        )
    }
}

/** Global intensity, including off (§7 motion & haptic tuning). */
@Immutable
enum class HapticIntensity(val scale: Float) {
    OFF(0f),
    LIGHT(0.55f),
    STANDARD(1f),
    STRONG(1.4f),
}

@Composable
fun rememberHaptics(intensity: HapticIntensity = HapticIntensity.STANDARD): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, intensity) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        Haptics(view, vibrator?.takeIf { it.hasVibrator() }, intensity)
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> {
    error("Haptics not provided — wrap the surface in LumenTheme")
}
