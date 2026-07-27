package dev.lumen.launcher.feature.home.ui

import android.annotation.SuppressLint
import android.content.Context

/**
 * D43: swipe down anywhere on the home surface opens the notification shade — the gesture every
 * launcher ships, and doubly needed here because Lumen hides the status bar (D30), which leaves
 * only a careful swipe from the very top edge otherwise.
 *
 * There is no public API for this. `StatusBarManager#expandNotificationsPanel` has been the
 * de-facto launcher route for a decade (it is why `EXPAND_STATUS_BAR` exists as a normal-level
 * permission) and remains on the unsupported-but-allowed list. Everything is guarded: if an OS
 * update ever closes the door, the swipe quietly does nothing — exactly what it did before D43.
 */
@SuppressLint("WrongConstant")
internal fun expandNotificationShade(context: Context) {
    runCatching {
        val service = context.getSystemService("statusbar") ?: return
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(service)
    }
}
