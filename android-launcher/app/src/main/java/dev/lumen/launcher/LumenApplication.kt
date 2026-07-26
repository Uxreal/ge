package dev.lumen.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.lumen.launcher.core.data.system.CrashLog
import dev.lumen.launcher.core.data.wallpaper.WallpaperCoordinator
import dev.lumen.launcher.core.data.workspace.WorkspaceRepository
import javax.inject.Inject

/**
 * §10: the launcher process is killed constantly, and the layout must be drawable on the first
 * frame after window attach — so the workspace read starts here, on a background thread, before
 * any Activity exists.
 */
@HiltAndroidApp
class LumenApplication : Application() {

    @Inject
    lateinit var workspace: WorkspaceRepository

    @Inject
    lateinit var wallpaper: WallpaperCoordinator

    override fun onCreate() {
        // Before anything else can fail: a crash with no trace is undebuggable in the field.
        CrashLog.install(this)
        super.onCreate()
        workspace.preload()
        wallpaper.refresh()
    }
}
