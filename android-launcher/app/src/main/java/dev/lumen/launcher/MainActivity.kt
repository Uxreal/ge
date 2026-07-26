package dev.lumen.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.lumen.launcher.feature.capsule.CapsuleViewModel
import dev.lumen.launcher.feature.drawer.DrawerViewModel
import dev.lumen.launcher.feature.home.HomeViewModel
import dev.lumen.launcher.feature.settings.SettingsViewModel
import dev.lumen.launcher.feature.widgets.WidgetHostManager
import dev.lumen.launcher.ui.LauncherRoot
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

/**
 * The HOME activity. `singleTask` + `stateNotNeeded`, so pressing Home re-delivers the intent
 * instead of recreating anything — [onNewIntent] drives §5's home-press sequence.
 *
 * Widget binding and configuration are the two flows that still require the old
 * `startActivityForResult` machinery (`AppWidgetHost.startAppWidgetConfigureActivityForResult`
 * has no Activity Result contract), so this Activity owns them and reports outcomes on a channel.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var widgetHost: WidgetHostManager

    /** One entry per completed bind/configure round: the widget id on success, null on cancel. */
    val widgetFlowResults = Channel<Int?>(Channel.BUFFERED)

    val homePresses = Channel<Unit>(Channel.CONFLATED)

    private val homeVm: HomeViewModel by viewModels()
    private val capsuleVm: CapsuleViewModel by viewModels()
    private val drawerVm: DrawerViewModel by viewModels()
    private val settingsVm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LauncherRoot(
                activity = this,
                homeVm = homeVm,
                drawerVm = drawerVm,
                settingsVm = settingsVm,
                capsuleVm = capsuleVm,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.trySend(Unit)
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    // ------------------------------------------------------------------ widget bind/configure

    fun startWidgetFlow(provider: AppWidgetProviderInfo) {
        val id = widgetHost.allocateId()
        if (id == -1) {
            widgetFlowResults.trySend(null)
            return
        }
        if (widgetHost.bind(id, provider)) {
            continueWidgetFlow(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
            }
            @Suppress("DEPRECATION")
            runCatching { startActivityForResult(intent, REQUEST_BIND) }
                .onFailure {
                    widgetHost.releaseId(id)
                    widgetFlowResults.trySend(null)
                }
        }
    }

    private fun continueWidgetFlow(appWidgetId: Int) {
        val info = widgetHost.infoFor(appWidgetId)
        if (info?.configure != null) {
            runCatching {
                widgetHost.appWidgetHost.startAppWidgetConfigureActivityForResult(
                    this, appWidgetId, 0, REQUEST_CONFIGURE, null,
                )
            }.onFailure {
                // A broken configure activity should not cost the user the widget.
                widgetFlowResults.trySend(appWidgetId)
            }
        } else {
            widgetFlowResults.trySend(appWidgetId)
        }
    }

    @Deprecated("AppWidgetHost configure flow predates Activity Result contracts")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        when (requestCode) {
            REQUEST_BIND -> if (resultCode == RESULT_OK && id != -1) {
                continueWidgetFlow(id)
            } else {
                if (id != -1) widgetHost.releaseId(id)
                widgetFlowResults.trySend(null)
            }

            REQUEST_CONFIGURE -> if (resultCode == RESULT_OK && id != -1) {
                widgetFlowResults.trySend(id)
            } else {
                if (id != -1) widgetHost.releaseId(id)
                widgetFlowResults.trySend(null)
            }
        }
    }

    private companion object {
        const val REQUEST_BIND = 0x4C01
        const val REQUEST_CONFIGURE = 0x4C02
    }
}
