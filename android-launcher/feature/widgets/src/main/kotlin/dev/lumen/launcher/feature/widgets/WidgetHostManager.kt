package dev.lumen.launcher.feature.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The `AppWidgetHost` wrapper (§2). One host for the whole launcher; the Activity drives
 * [startListening]/[stopListening] with its lifecycle, and §10's process-death rule is honoured by
 * never storing views — a host view is recreated from its id on demand.
 */
@Singleton
class WidgetHostManager @Inject constructor(
    @ApplicationContext private val context: Context,
    scope: CoroutineScope,
) {
    private val host = AppWidgetHost(context, HOST_ID)
    private val manager: AppWidgetManager? =
        runCatching { AppWidgetManager.getInstance(context) }.getOrNull()

    private var listening = false

    private val _providers = MutableStateFlow<List<AppWidgetProviderInfo>>(emptyList())
    val providers: StateFlow<List<AppWidgetProviderInfo>> = _providers.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) { refreshProviders() }
    }

    fun startListening() {
        if (listening) return
        // Some OEMs throw from startListening when the host has stale ids; never let that kill home.
        runCatching { host.startListening() }.onSuccess { listening = true }
    }

    fun stopListening() {
        if (!listening) return
        runCatching { host.stopListening() }
        listening = false
    }

    fun refreshProviders() {
        _providers.value = runCatching {
            val users = context.getSystemService(UserManager::class.java)?.userProfiles
                ?: listOf(Process.myUserHandle())
            users.flatMap { user ->
                manager?.getInstalledProvidersForProfile(user).orEmpty()
            }.sortedBy { it.loadLabel(context.packageManager) ?: "" }
        }.getOrDefault(emptyList())
    }

    fun allocateId(): Int = runCatching { host.allocateAppWidgetId() }.getOrDefault(-1)

    fun releaseId(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    fun providerFor(flat: String): AppWidgetProviderInfo? {
        val component = ComponentName.unflattenFromString(flat) ?: return null
        return providers.value.firstOrNull { it.provider == component }
            ?: runCatching { manager?.getAppWidgetInfo(-1) }.getOrNull() // never resolves; keep null path
    }

    fun infoFor(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager?.getAppWidgetInfo(appWidgetId) }.getOrNull()

    /** True when the id bound without user interaction; false means launch the bind intent. */
    fun bind(appWidgetId: Int, provider: AppWidgetProviderInfo): Boolean = runCatching {
        manager?.bindAppWidgetIdIfAllowed(appWidgetId, provider.profile, provider.provider, null)
    }.getOrNull() == true

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = infoFor(appWidgetId) ?: return null
        return runCatching {
            host.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }

    fun previewOf(provider: AppWidgetProviderInfo): Drawable? = runCatching {
        provider.loadPreviewImage(context, 0) ?: provider.loadIcon(context, 0)
    }.getOrNull()

    fun labelOf(provider: AppWidgetProviderInfo): String = runCatching {
        provider.loadLabel(context.packageManager) ?: provider.provider.shortClassName
    }.getOrDefault(provider.provider.shortClassName)

    fun appLabelOf(provider: AppWidgetProviderInfo): String = runCatching {
        val applicationInfo = context.packageManager.getApplicationInfo(provider.provider.packageName, 0)
        context.packageManager.getApplicationLabel(applicationInfo).toString()
    }.getOrDefault(provider.provider.packageName)

    /** Default span from the provider's minimum size and the current cell size. */
    fun spanFor(provider: AppWidgetProviderInfo, cellWidthPx: Float, cellHeightPx: Float): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        val minW = provider.minWidth * density
        val minH = provider.minHeight * density
        val spanX = kotlin.math.ceil(minW / cellWidthPx.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
        val spanY = kotlin.math.ceil(minH / cellHeightPx.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
        return spanX to spanY
    }

    fun needsConfigure(provider: AppWidgetProviderInfo): Boolean = provider.configure != null

    val appWidgetHost: AppWidgetHost get() = host

    companion object {
        /** Stable host id — changing it orphans every bound widget. */
        const val HOST_ID = 0x4C4D_0001.toInt()
    }
}
