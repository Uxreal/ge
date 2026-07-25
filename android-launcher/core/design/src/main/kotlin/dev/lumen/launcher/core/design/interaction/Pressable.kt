package dev.lumen.launcher.core.design.interaction

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Tap handling for launcher icons.
 *
 * Deliberately not `clickable`: no ripple (glass and ripples fight each other), the press state is
 * surfaced so the caller can drive its own spring, and a long press must be able to hand the
 * gesture to a drag without the tap firing.
 */
fun Modifier.launcherPressable(
    onPressChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
): Modifier = pointerInput(onClick, onLongPress) {
    detectTapGestures(
        onPress = {
            onPressChange(true)
            tryAwaitRelease()
            onPressChange(false)
        },
        onTap = { onClick() },
        onLongPress = onLongPress?.let { action -> { action() } },
    )
}
