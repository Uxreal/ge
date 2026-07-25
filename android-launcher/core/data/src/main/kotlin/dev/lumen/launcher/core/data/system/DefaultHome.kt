package dev.lumen.launcher.core.data.system

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Default-home plumbing (§2: `RoleManager` for the prompt). Everything is best-effort — a launcher
 * that crashes asking to become the launcher is beyond parody.
 */
object DefaultHome {

    fun isDefault(context: Context): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == context.packageName
    }.getOrDefault(false)

    /** The system's role prompt, or null when the role is unavailable or already held. */
    fun requestRoleIntent(context: Context): Intent? = runCatching {
        val roles = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roles.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        if (roles.isRoleHeld(RoleManager.ROLE_HOME)) return null
        roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }.getOrNull()

    fun openHomeSettings(context: Context) {
        val direct = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = runCatching { context.startActivity(direct) }.isSuccess
        if (!opened) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
