package dev.lumen.launcher.core.data.model

/**
 * Stable identity for a launchable activity. [profile] is the `UserHandle` serial number, which
 * survives reboots; a work-profile clone therefore never collides with its personal counterpart.
 */
data class AppKey(
    val packageName: String,
    val activityName: String,
    val profile: Long = 0L,
) {
    /** Round-trippable string form, used as the persistence key. */
    val flat: String get() = "$packageName/$activityName@$profile"

    companion object {
        fun parse(flat: String): AppKey? {
            val at = flat.lastIndexOf('@')
            if (at <= 0) return null
            val profile = flat.substring(at + 1).toLongOrNull() ?: return null
            val slash = flat.indexOf('/')
            if (slash <= 0 || slash > at) return null
            return AppKey(flat.substring(0, slash), flat.substring(slash + 1, at), profile)
        }
    }
}

enum class AppCategory {
    SOCIAL, COMMUNICATION, MEDIA, PHOTOGRAPHY, PRODUCTIVITY, GAMES, NEWS,
    SHOPPING, TRAVEL, FINANCE, HEALTH, EDUCATION, UTILITIES, SYSTEM, OTHER;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/** A launchable app as surfaced by `LauncherApps`. Icons are loaded separately via `IconCache`. */
data class AppInfo(
    val key: AppKey,
    val label: String,
    /** Lowercase, diacritic-stripped label + compact form + initials + package, for search. */
    val searchTokens: String,
    val isSystemApp: Boolean,
    val isWorkProfile: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val category: AppCategory,
) {
    val packageName: String get() = key.packageName
}
