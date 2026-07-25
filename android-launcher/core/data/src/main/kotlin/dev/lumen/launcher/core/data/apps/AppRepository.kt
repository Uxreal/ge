package dev.lumen.launcher.data.apps

import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.view.View
import androidx.core.content.ContextCompat
import dev.lumen.launcher.data.AppRepository
import dev.lumen.launcher.data.model.AppCategory
import dev.lumen.launcher.data.model.AppInfo
import dev.lumen.launcher.data.model.AppKey
import dev.lumen.launcher.data.prefs.AppOpenAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.Collator
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shared translation between `AppKey.profile` (a `UserHandle` serial number) and the live
 * [UserHandle] objects the platform wants.
 *
 * The serial is what gets persisted, because a `UserHandle`'s identifier is only meaningful for the
 * lifetime of a boot while the serial survives reboots and work-profile pause/resume cycles.
 */
internal object LauncherProfiles {

    fun serialFor(context: Context, user: UserHandle): Long {
        val manager = context.getSystemService(UserManager::class.java) ?: return fallbackSerial(user)
        val serial = runCatching { manager.getSerialNumberForUser(user) }.getOrDefault(-1L)
        return if (serial < 0L) fallbackSerial(user) else serial
    }

    fun handleFor(context: Context, serial: Long): UserHandle? {
        val manager = context.getSystemService(UserManager::class.java) ?: return null
        runCatching { manager.getUserForSerialNumber(serial) }.getOrNull()?.let { return it }
        // Some OEM builds refuse the lookup for paused profiles; walk the list instead.
        return runCatching {
            manager.userProfiles.firstOrNull { serialFor(context, it) == serial }
        }.getOrNull()
    }

    /** The personal profile is serial 0 on every shipping device; only exotic builds need the fallback. */
    private fun fallbackSerial(user: UserHandle): Long =
        if (user == Process.myUserHandle()) 0L else user.hashCode().toLong()
}

/**
 * The installed-app universe.
 *
 * Everything the launcher shows is derived from this list, so it is kept live rather than polled: a
 * single [LauncherApps.Callback] rebuilds only the affected package, and additions/removals are
 * announced on [installed]/[uninstalled] so the workspace can auto-place and prune without
 * diffing the whole world itself.
 */
class DefaultAppRepository(
    context: Context,
    private val scope: CoroutineScope,
) : AppRepository {

    private val appContext: Context = context.applicationContext
    private val launcherApps: LauncherApps? = runCatching {
        appContext.getSystemService(LauncherApps::class.java)
    }.getOrNull()
    private val userManager: UserManager? = runCatching {
        appContext.getSystemService(UserManager::class.java)
    }.getOrNull()
    private val packageManager: PackageManager = appContext.packageManager

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    override val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Buffered so the LauncherApps callback thread is never suspended by a slow collector.
    private val _installed = MutableSharedFlow<AppKey>(extraBufferCapacity = 128)
    override val installed: SharedFlow<AppKey> = _installed.asSharedFlow()

    private val _uninstalled = MutableSharedFlow<AppKey>(extraBufferCapacity = 128)
    override val uninstalled: SharedFlow<AppKey> = _uninstalled.asSharedFlow()

    /**
     * Which system transition [launch] asks the framework for. Defaults to the icon-origin zoom;
     * the UI layer mirrors `MotionSettings.appOpenAnimation` onto it, because the repository is
     * constructed before the settings store exists and must not depend on it.
     */
    @Volatile
    var openAnimation: AppOpenAnimation = AppOpenAnimation.ZOOM_FROM_ICON

    /** Flat key -> app. Replaced wholesale on every publish so readers never see a torn map. */
    @Volatile
    private var index: Map<String, AppInfo> = emptyMap()

    /** Serial -> handle, refreshed on every full load; launching needs the live handle. */
    @Volatile
    private var profileHandles: Map<Long, UserHandle> = emptyMap()

    private val reloadLock = Mutex()
    private val fullLoadPending = AtomicBoolean(false)
    private var announceChanges = false

    private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    private val comparator: Comparator<AppInfo> =
        compareBy<AppInfo, String>(collator) { it.label }.thenBy { it.key.flat }

    /**
     * `ActivityOptions.makeClipRevealAnimation` needs a `View` purely to read its position on
     * screen and its package. A detached view reports (0, 0), which makes the coordinates we pass
     * absolute — exactly the space `sourceBounds` is already in.
     */
    private val animationAnchor: View by lazy { View(appContext) }

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = schedule(packageName, user)

        override fun onPackageRemoved(packageName: String, user: UserHandle) = schedule(packageName, user)

        override fun onPackageChanged(packageName: String, user: UserHandle) = schedule(packageName, user)

        override fun onPackagesAvailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean,
        ) = schedule(packageNames, user)

        override fun onPackagesUnavailable(
            packageNames: Array<String>,
            user: UserHandle,
            replacing: Boolean,
        ) = schedule(packageNames, user)

        override fun onPackagesSuspended(packageNames: Array<String>, user: UserHandle) =
            schedule(packageNames, user)

        override fun onPackagesUnsuspended(packageNames: Array<String>, user: UserHandle) =
            schedule(packageNames, user)

        private fun schedule(packageName: String, user: UserHandle) {
            scope.launch { reloadPackages(listOf(packageName), user) }
        }

        private fun schedule(packageNames: Array<String>, user: UserHandle) {
            val names = packageNames.toList()
            scope.launch { reloadPackages(names, user) }
        }
    }

    /** A work profile can appear or disappear long after the first load. */
    private val profileReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        runCatching { launcherApps?.registerCallback(callback, Handler(Looper.getMainLooper())) }
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                profileReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
                    addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
                    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }
        refresh()
    }

    // ------------------------------------------------------------------ reads

    override fun app(key: AppKey): AppInfo? = index[key.flat]

    override fun hasWorkProfile(): Boolean =
        runCatching { userManager?.userProfiles?.any { it != Process.myUserHandle() } }.getOrNull() == true

    /** Live handle for a persisted profile serial, or null when that profile is gone. */
    fun profileHandle(serial: Long): UserHandle? =
        profileHandles[serial] ?: LauncherProfiles.handleFor(appContext, serial)

    // ------------------------------------------------------------------ loading

    override fun refresh() {
        if (!fullLoadPending.compareAndSet(false, true)) return
        scope.launch { reloadAll() }
    }

    private suspend fun reloadAll() {
        try {
            reloadLock.withLock {
                withContext(Dispatchers.IO) {
                    val profiles = refreshProfiles()
                    val updateTimes = installedUpdateTimes()
                    val loaded = LinkedHashMap<String, AppInfo>()
                    for ((serial, user) in profiles) {
                        for (activity in activityList(null, user)) {
                            val info = toAppInfo(activity, serial, user) { updateTimes[it] ?: 0L }
                            loaded[info.key.flat] = info
                        }
                    }
                    publish(loaded)
                    announceChanges = true
                    _loading.value = false
                }
            }
        } finally {
            fullLoadPending.set(false)
        }
    }

    /**
     * Rebuilds just the given packages. The diff in [publish] turns that into the right
     * installed/uninstalled events, so removals, updates and profile availability all share one path.
     */
    private suspend fun reloadPackages(packageNames: List<String>, user: UserHandle) {
        if (packageNames.isEmpty()) return
        reloadLock.withLock {
            withContext(Dispatchers.IO) {
                val serial = LauncherProfiles.serialFor(appContext, user)
                if (profileHandles[serial] == null) refreshProfiles()
                val next = LinkedHashMap(index)
                for (packageName in packageNames) {
                    next.entries.removeAll { (_, app) ->
                        app.packageName == packageName && app.key.profile == serial
                    }
                    val updateTime = updateTimeOf(packageName)
                    for (activity in activityList(packageName, user)) {
                        val info = toAppInfo(activity, serial, user) { updateTime }
                        next[info.key.flat] = info
                    }
                }
                publish(next)
            }
        }
    }

    private fun publish(next: Map<String, AppInfo>) {
        val previous = index
        index = next
        _apps.value = next.values.sortedWith(comparator)
        if (!announceChanges) return
        for ((flat, app) in next) {
            if (flat !in previous) _installed.tryEmit(app.key)
        }
        for ((flat, app) in previous) {
            if (flat !in next) _uninstalled.tryEmit(app.key)
        }
    }

    private fun refreshProfiles(): List<Pair<Long, UserHandle>> {
        val handles = runCatching { userManager?.userProfiles }.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(Process.myUserHandle())
        val mapped = handles.map { LauncherProfiles.serialFor(appContext, it) to it }
        profileHandles = mapped.toMap()
        return mapped
    }

    private fun activityList(packageName: String?, user: UserHandle): List<LauncherActivityInfo> =
        runCatching { launcherApps?.getActivityList(packageName, user) }.getOrNull().orEmpty()

    /** One binder call for every personal package beats one call per app during the first load. */
    private fun installedUpdateTimes(): Map<String, Long> = runCatching {
        packageManager.getInstalledPackages(0).associate { it.packageName to it.lastUpdateTime }
    }.getOrDefault(emptyMap())

    private fun updateTimeOf(packageName: String): Long = runCatching {
        packageManager.getPackageInfo(packageName, 0).lastUpdateTime
    }.getOrDefault(0L)

    private fun toAppInfo(
        activity: LauncherActivityInfo,
        serial: Long,
        user: UserHandle,
        updateTime: (String) -> Long,
    ): AppInfo {
        val component = activity.componentName
        val applicationInfo: ApplicationInfo? = runCatching { activity.applicationInfo }.getOrNull()
        val label = runCatching { activity.label?.toString() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: component.packageName
        val flags = applicationInfo?.flags ?: 0
        val system = flags and ApplicationInfo.FLAG_SYSTEM != 0
        val firstInstall = runCatching { activity.firstInstallTime }.getOrDefault(0L)
        return AppInfo(
            key = AppKey(component.packageName, component.className, serial),
            label = label,
            searchTokens = searchTokens(label, component.packageName),
            isSystemApp = system,
            isWorkProfile = user != Process.myUserHandle(),
            firstInstallTime = firstInstall,
            lastUpdateTime = updateTime(component.packageName).takeIf { it > 0L } ?: firstInstall,
            category = categoryOf(applicationInfo, component.packageName, system),
        )
    }

    // ------------------------------------------------------------------ search tokens

    /**
     * Everything a query can match, precomputed once: the folded label ("pokemon go" matches
     * "Pokémon GO"), the label with separators removed ("googlemaps"), the word initials ("gm") and
     * the package name.
     */
    private fun searchTokens(label: String, packageName: String): String {
        val folded = fold(label)
        val compact = folded.filter { it.isLetterOrDigit() }
        val initials = folded.split(' ', '-', '_', '.', '&', '/', '+')
            .filter { it.isNotBlank() }
            .map { it.first() }
            .joinToString(separator = "")
        return buildString {
            append(folded)
            if (compact.isNotEmpty() && compact != folded) append(' ').append(compact)
            if (initials.length > 1) append(' ').append(initials)
            append(' ').append(packageName.lowercase())
        }
    }

    private fun fold(text: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD), "")

    // ------------------------------------------------------------------ categories

    /**
     * The manifest category when the developer declared one, otherwise a package-name heuristic.
     * Never null: an uncategorised third-party app is [AppCategory.OTHER] and an uncategorised
     * platform app is [AppCategory.SYSTEM], which is what the drawer's category tabs expect.
     */
    private fun categoryOf(info: ApplicationInfo?, packageName: String, system: Boolean): AppCategory {
        declaredCategory(info)?.let { return it }
        val lower = packageName.lowercase()
        for ((needles, category) in PACKAGE_HEURISTICS) {
            if (needles.any { lower.contains(it) }) return category
        }
        return if (system) AppCategory.SYSTEM else AppCategory.OTHER
    }

    private fun declaredCategory(info: ApplicationInfo?): AppCategory? = when (info?.category) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> AppCategory.MEDIA
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.PHOTOGRAPHY
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.TRAVEL
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.UTILITIES
        else -> null
    }

    // ------------------------------------------------------------------ actions

    override fun launch(key: AppKey, sourceBounds: Rect?): Boolean {
        val apps = launcherApps ?: return launchViaIntent(key)
        val user = profileHandle(key.profile) ?: Process.myUserHandle()
        val component = ComponentName(key.packageName, key.activityName)
        val bounds = sourceBounds?.takeIf { !it.isEmpty }
        val started = runCatching {
            apps.startMainActivity(component, user, bounds, launchOptions(bounds))
        }.isSuccess
        if (started) return true
        // Options and source bounds are decoration; never let them cost the user the launch.
        return runCatching { apps.startMainActivity(component, user, null, null) }.isSuccess ||
            launchViaIntent(key)
    }

    private fun launchViaIntent(key: AppKey): Boolean = runCatching {
        val intent = packageManager.getLaunchIntentForPackage(key.packageName)
            ?: return@runCatching false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Maps the user's [AppOpenAnimation] onto what the platform can actually do from a launcher:
     * a clip reveal grows the window out of the icon's rectangle, a scale-up zooms it, a custom
     * animation cross-fades, and `SYSTEM` leaves the window animation alone.
     */
    private fun launchOptions(bounds: Rect?): Bundle? = when (openAnimation) {
        AppOpenAnimation.SYSTEM -> null
        AppOpenAnimation.FADE -> customFade()
        AppOpenAnimation.ZOOM_FROM_ICON, AppOpenAnimation.LIQUID_EXPAND ->
            bounds?.let { clipReveal(it) ?: scaleUp(it) }
        AppOpenAnimation.SLIDE_UP -> bounds?.let { scaleUp(it) ?: clipReveal(it) }
    }

    private fun clipReveal(bounds: Rect): Bundle? = runCatching {
        ActivityOptions.makeClipRevealAnimation(
            animationAnchor,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height(),
        ).toBundle()
    }.getOrNull()

    private fun scaleUp(bounds: Rect): Bundle? = runCatching {
        ActivityOptions.makeScaleUpAnimation(
            animationAnchor,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height(),
        ).toBundle()
    }.getOrNull()

    private fun customFade(): Bundle? = runCatching {
        ActivityOptions.makeCustomAnimation(
            appContext,
            android.R.anim.fade_in,
            android.R.anim.fade_out,
        ).toBundle()
    }.getOrNull()

    override fun openAppInfo(key: AppKey, sourceBounds: Rect?) {
        val apps = launcherApps
        val user = profileHandle(key.profile) ?: Process.myUserHandle()
        val component = ComponentName(key.packageName, key.activityName)
        val bounds = sourceBounds?.takeIf { !it.isEmpty }
        val shown = apps != null && runCatching {
            apps.startAppDetailsActivity(component, user, bounds, launchOptions(bounds))
        }.isSuccess
        if (shown) return
        runCatching {
            appContext.startActivity(
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", key.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    override fun requestUninstall(key: AppKey) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", key.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        profileHandle(key.profile)?.let { intent.putExtra(Intent.EXTRA_USER, it) }
        runCatching { appContext.startActivity(intent) }
    }

    /** A preloaded system app can only be reverted to its factory version, never removed. */
    override fun canUninstall(key: AppKey): Boolean {
        val flags = applicationInfoOf(key)?.flags ?: return false
        val system = flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updated = flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !system || updated
    }

    private fun applicationInfoOf(key: AppKey): ApplicationInfo? {
        val user = profileHandle(key.profile) ?: Process.myUserHandle()
        runCatching { launcherApps?.getApplicationInfo(key.packageName, 0, user) }
            .getOrNull()
            ?.let { return it }
        return runCatching { packageManager.getApplicationInfo(key.packageName, 0) }.getOrNull()
    }

    private companion object {
        val COMBINING_MARKS = Regex("\\p{Mn}+")

        /**
         * Ordered fallbacks for apps that never declared a category — which is most of them. The
         * first match wins, so the more specific families come first.
         */
        val PACKAGE_HEURISTICS: List<Pair<List<String>, AppCategory>> = listOf(
            listOf(
                "dialer", "telecom", "contacts", "messaging", ".mms", ".sms", "whatsapp",
                "telegram", "signal", "messenger", "gmail", "outlook", "bluemail", "protonmail",
                "email", "zoom", "webex", "teams", "skype", "duo", "meet",
            ) to AppCategory.COMMUNICATION,
            listOf(
                "camera", "gallery", "photos", "lightroom", "snapseed", "gcam", "vsco",
                "picsart", "photoeditor",
            ) to AppCategory.PHOTOGRAPHY,
            listOf(
                "instagram", "facebook", "twitter", "com.twitter", "tiktok", "snapchat",
                "mastodon", "discord", "threads", "pinterest", "tumblr", "bereal", "bluesky",
            ) to AppCategory.SOCIAL,
            listOf(
                "maps", "navigation", "waze", "uber", "lyft", "bolt", "citymapper", "transit",
                "booking", "airbnb", "trivago", "skyscanner", "flight", "railway", "trainline",
            ) to AppCategory.TRAVEL,
            listOf(
                "bank", "banking", "paypal", "revolut", "venmo", "wise", "monzo", "chase",
                "wallet", "coinbase", "binance", "finance", "invest", "trading", "wealth",
            ) to AppCategory.FINANCE,
            listOf("game", "games", "playgames", "unity", "gameloft", "supercell") to AppCategory.GAMES,
            listOf(
                "music", "spotify", "youtube", "netflix", "podcast", "audible", "deezer", "tidal",
                "soundcloud", "plex", "kodi", "vlc", "disney", "primevideo", "hbo", "twitch",
            ) to AppCategory.MEDIA,
            listOf(
                "shop", "shopping", "amazon", "ebay", "etsy", "aliexpress", "wish", "zalando",
                "ikea", "store", "market",
            ) to AppCategory.SHOPPING,
            listOf(
                "news", "reddit", "feedly", "flipboard", "nytimes", "guardian", "bbc",
                "cnn", "reuters",
            ) to AppCategory.NEWS,
            listOf(
                "fit", "fitness", "health", "strava", "calm", "headspace", "myfitnesspal",
                "sleep", "period", "medical", "pharmacy",
            ) to AppCategory.HEALTH,
            listOf(
                "duolingo", "coursera", "udemy", "khan", "anki", "edu", "school", "wikipedia",
                "dictionary", "translate",
            ) to AppCategory.EDUCATION,
            listOf(
                "docs", "sheets", "slides", "office", "word", "excel", "powerpoint", "notion",
                "todoist", "calendar", "keep", "drive", "dropbox", "onedrive", "evernote",
                "obsidian", "trello", "slack", "asana", "scanner", "pdf",
            ) to AppCategory.PRODUCTIVITY,
            listOf(
                "calculator", "clock", "deskclock", "files", "filemanager", "flashlight",
                "recorder", "vpn", "authenticator", "keyboard", "terminal", "weather",
            ) to AppCategory.UTILITIES,
            listOf(
                "com.android.", "com.google.android.gms", "com.google.android.apps.wellbeing",
                "com.samsung.android.", "com.miui.", "com.oneplus.", "settings", "systemui",
            ) to AppCategory.SYSTEM,
        )
    }
}
