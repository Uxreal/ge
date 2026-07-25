package dev.lumen.launcher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import dev.lumen.launcher.data.LauncherServices
import dev.lumen.launcher.ui.LauncherRoot

/**
 * The home activity.
 *
 * Being `singleTask` with `stateNotNeeded`, pressing HOME re-delivers an intent rather than
 * recreating anything — [onNewIntent] is what makes the home button feel instant, unwinding
 * overlays and edit mode instead of rebuilding the workspace.
 */
class MainActivity : ComponentActivity() {

    private val services: LauncherServices
        get() = (application as LauncherApplication).services

    private var homePressListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Let the wallpaper (and the launcher's own glass) sit behind the system bars.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
        )

        setContent {
            LauncherRoot(
                activity = this,
                services = services,
                registerHomePress = { listener -> homePressListener = listener },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressListener?.invoke()
        }
    }

    override fun onResume() {
        super.onResume()
        services.widgets.startListening()
        services.apps.refresh()
    }

    override fun onPause() {
        super.onPause()
        services.widgets.stopListening()
    }

    @Deprecated("Widget bind/configure predates the Activity Result APIs and still uses request codes")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (services.widgets.onActivityResult(requestCode, resultCode, data)) return
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
    }
}
