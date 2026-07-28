package dev.lumen.launcher.core.data.system

import android.content.Context

/**
 * D44: the crash-loop breaker. A launcher that dies during composition can never show its own
 * crash card — the process is gone before the first frame — so the user is left with a phone
 * whose Home button does nothing. This object counts consecutive boots that never reached a
 * stable frame; at [THRESHOLD] the next boot comes up in safe mode: the Capsule stays off, the
 * system status bar stays visible, and the notification listener publishes nothing. A launcher
 * must always open.
 *
 * SharedPreferences, not DataStore: the count must be written synchronously before the next
 * crash can land, and read before Hilt exists.
 */
object SafeMode {

    private const val PREFS = "lumen-boot"
    private const val KEY_ATTEMPTS = "attempts"

    /** Two full-mode boots may die; the third boot is safe. */
    const val THRESHOLD = 3

    /** The policy, pure for the test: is boot number [attempts] a safe-mode boot? */
    fun safeFor(attempts: Int): Boolean = attempts >= THRESHOLD

    /** True for the whole process lifetime once a boot starts in safe mode. */
    @Volatile
    var active: Boolean = false
        private set

    /** Call first thing in Application.onCreate. Returns whether this boot is safe-mode. */
    fun onProcessStart(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val attempts = prefs.getInt(KEY_ATTEMPTS, 0) + 1
        // commit(), not apply(): the very next instruction may be the crash.
        prefs.edit().putInt(KEY_ATTEMPTS, attempts).commit()
        active = safeFor(attempts)
        return active
    }

    /**
     * Call once the UI has been alive long enough to count as a working boot. A safe-mode boot
     * does NOT clear the counter — safe mode persists until the user asks for full mode, so a
     * crash-safe-crash-safe cycle cannot happen on its own.
     */
    fun confirmAlive(context: Context) {
        if (active) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ATTEMPTS, 0).apply()
    }

    /** The banner's "Try full mode": reset the counter and die; the system restarts its Home. */
    fun exitSafeModeAndRestart(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ATTEMPTS, 0).commit()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
