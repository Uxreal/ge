package dev.lumen.launcher.system

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast

/** Device-admin receiver whose only purpose is to unlock `lockNow()` for the sleep gesture. */
class LumenDeviceAdmin : DeviceAdminReceiver()

/**
 * Thin wrappers over the system surfaces a launcher is expected to reach. Everything here is
 * best-effort: these APIs vary by OEM and Android version, so each call degrades to a short
 * explanation rather than a crash.
 */
object SystemActions {

    private fun adminComponent(context: Context) =
        ComponentName(context.applicationContext, LumenDeviceAdmin::class.java)

    fun isLockAvailable(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return false
        return dpm.isAdminActive(adminComponent(context))
    }

    /**
     * Sleeping the device from a launcher requires device-admin consent; the first invocation walks
     * the user to that screen and later ones lock immediately.
     */
    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (dpm == null) {
            toast(context, "Screen lock is unavailable on this device")
            return
        }
        val component = adminComponent(context)
        if (dpm.isAdminActive(component)) {
            runCatching { dpm.lockNow() }
                .onFailure { toast(context, "Could not lock the screen") }
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Lumen needs this permission only to turn the screen off " +
                        "when you use the sleep gesture.",
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
                .onFailure { toast(context, "Could not open device admin settings") }
        }
    }

    fun expandNotifications(context: Context) = invokeStatusBar(
        context = context,
        method = "expandNotificationsPanel",
        failureMessage = "This Android version blocks opening the shade",
    )

    fun expandQuickSettings(context: Context) = invokeStatusBar(
        context = context,
        method = "expandSettingsPanel",
        failureMessage = "This Android version blocks opening quick settings",
    )

    /**
     * `StatusBarManager` has never been public API, but the two panel methods have been stable for
     * a decade and every third-party launcher uses them. Reflection failure is expected on some
     * builds and simply disables the gesture.
     */
    private fun invokeStatusBar(context: Context, method: String, failureMessage: String) {
        runCatching {
            @Suppress("WrongConstant")
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            clazz.getMethod(method).invoke(service)
        }.onFailure {
            toast(context, failureMessage)
        }
    }

    fun openAssistant(context: Context) {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, intent)) openVoiceSearch(context)
    }

    fun openVoiceSearch(context: Context) {
        val intent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, intent)) toast(context, "No voice search app installed")
    }

    fun openRecents(context: Context) {
        // Recents is only reachable through an accessibility service; point the user at the gesture.
        toast(context, "Use the system gesture to open Recents")
    }

    fun openNotificationListenerSettings(context: Context) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        } else {
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        }
        start(context, Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openHomeSettings(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!start(context, intent)) {
            start(context, Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun openUsageAccessSettings(context: Context) {
        start(
            context,
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun start(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
