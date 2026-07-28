package dev.lumen.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.lumen.launcher.core.data.system.CrashLog
import dev.lumen.launcher.core.data.system.SafeMode
import dev.lumen.launcher.core.data.wallpaper.WallpaperCoordinator
import dev.lumen.launcher.core.data.workspace.WorkspaceRepository
import dev.lumen.launcher.feature.capsule.CapsuleController
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

    @Inject
    lateinit var capsule: CapsuleController

    override fun onCreate() {
        // Before anything else can fail: a crash with no trace is undebuggable in the field.
        CrashLog.install(this)
        // D44: count this boot; repeated deaths flip the next boot into safe mode.
        val safe = SafeMode.onProcessStart(this)
        super.onCreate()
        // Guarded individually (D44): startup work that fails must never take the launcher with
        // it — a note in the crash file beats a phone whose Home button does nothing.
        runCatching { workspace.preload() }.onFailure { CrashLog.note(this, "workspace.preload", it) }
        runCatching { wallpaper.refresh() }.onFailure { CrashLog.note(this, "wallpaper.refresh", it) }
        // The Capsule's sources outlive any Activity: a card pushed while the launcher is in the
        // background must already be in the deck when the Home button brings it back. In safe
        // mode the Capsule stays down entirely.
        if (!safe) {
            runCatching { capsule.start() }.onFailure { CrashLog.note(this, "capsule.start", it) }
        }
    }
}
