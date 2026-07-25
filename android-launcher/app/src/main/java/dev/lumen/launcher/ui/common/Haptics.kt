package dev.lumen.launcher.ui.common

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import dev.lumen.launcher.ui.LocalSettings

/**
 * Haptics tuned like iOS: a light tick for transient feedback, a firmer thud when something
 * commits. Everything routes through `View.performHapticFeedback` so the system's own haptic
 * settings and OEM actuator tuning are respected.
 */
@Stable
class Haptics(
    private val view: View,
    private val enabled: Boolean,
    private val strength: Float,
) {
    private fun perform(light: Int, firm: Int) {
        if (!enabled) return
        val constant = if (strength >= 0.66f) firm else light
        view.performHapticFeedback(constant)
    }

    /** Crossing a grid cell, passing a page boundary, scrubbing a slider. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.KEYBOARD_TAP)

    /** Tapping an icon or a control. */
    fun click() = perform(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.CONTEXT_CLICK)

    /** Entering edit mode, opening the long-press menu, picking an item up. */
    fun longPress() = perform(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.LONG_PRESS)

    /** Committing a drop, creating a folder, confirming a destructive action. */
    fun commit() = perform(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.CONFIRM)

    /** Rejecting an invalid drop. */
    fun reject() = perform(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.REJECT)
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val motion = LocalSettings.current.motion
    return remember(view, motion.haptics, motion.hapticStrength) {
        Haptics(view, motion.haptics, motion.hapticStrength)
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }
