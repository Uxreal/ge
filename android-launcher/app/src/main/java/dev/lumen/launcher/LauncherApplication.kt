package dev.lumen.launcher

import android.app.Application
import dev.lumen.launcher.data.LauncherServices

/**
 * A launcher is the one app the user cannot escape, so the service graph lives here for the whole
 * process lifetime: icon caches survive rotation, the workspace never reloads mid-gesture, and no
 * annotation processor sits in the build.
 */
class LauncherApplication : Application() {

    lateinit var services: LauncherServices
        private set

    override fun onCreate() {
        super.onCreate()
        services = LauncherServices(this)
        // Warm the two caches every first frame touches.
        services.apps.refresh()
        services.wallpaper.refresh()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            services.icons.invalidate(null)
        }
    }
}
