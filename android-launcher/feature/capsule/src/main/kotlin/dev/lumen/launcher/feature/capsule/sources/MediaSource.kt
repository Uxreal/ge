package dev.lumen.launcher.feature.capsule.sources

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import dagger.hilt.android.EntryPointAccessors
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import dev.lumen.launcher.feature.capsule.CapsuleAction
import dev.lumen.launcher.feature.capsule.CapsuleCard
import dev.lumen.launcher.feature.capsule.CapsuleController
import dev.lumen.launcher.feature.capsule.CapsuleGlyph
import dev.lumen.launcher.feature.capsule.SourceKind
import androidx.compose.ui.graphics.asImageBitmap
import dev.lumen.launcher.feature.capsule.push.CapsulePushReceiver

/**
 * §4's media source, priority 700: whatever `MediaSession` is currently playing becomes a card
 * with play/pause/next transport controls.
 *
 * `MediaSessionManager` only talks to a `NotificationListenerService` the user has explicitly
 * enabled, which is §10's consent model working as intended: no permission prompt at install, no
 * card until the user opts in from Settings → Capsule, full function without it. Every call here
 * is wrapped accordingly — "not granted" is a normal state, not an error.
 */
internal class MediaSource(
    private val context: Context,
    private val controller: CapsuleController,
) {
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, CapsuleNotificationListener::class.java)

    private var watched: MediaController? = null
    private var listening = false

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { publish(it.orEmpty()) }

    private val playbackCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Re-reads the world. Called at start, whenever the launcher resumes (the user may have just
     * granted access in system settings), when the listener service connects, from playback
     * callbacks, and once a minute as a self-heal for missed callbacks (OEM listener rebinding is
     * flaky, and a stale "nothing playing" that fixes itself within a minute beats one that
     * needs a process restart). Cheap, idempotent, and a silent no-op without consent.
     *
     * Always hops to the main thread: `MediaSessionManager` listener registration needs a Looper
     * thread, and callers should not have to know that.
     */
    fun refresh() {
        mainHandler.post { doRefresh() }
    }

    private fun doRefresh() {
        val manager = sessionManager ?: return
        runCatching {
            if (!listening) {
                manager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent)
                listening = true
            }
            publish(manager.getActiveSessions(listenerComponent))
        }.onFailure {
            // SecurityException: notification access not granted. Settings owns the consent flow.
            listening = false
        }
    }

    private fun publish(sessions: List<MediaController>) {
        // The card follows the session most worth showing: a playing one, else the most recent.
        val session = sessions.firstOrNull { it.isPlaying } ?: sessions.firstOrNull()

        if (watched?.sessionToken != session?.sessionToken) {
            watched?.unregisterCallback(playbackCallback)
            session?.registerCallback(playbackCallback)
            watched = session
        }

        if (session == null) {
            controller.clearKey(mediaKeyFor(lastPackage ?: return))
            lastPackage = null
            return
        }

        // One card per package+MEDIA (§4's dedupe); switching apps withdraws the old card.
        lastPackage?.takeIf { it != session.packageName }?.let {
            controller.clearKey(mediaKeyFor(it))
        }
        lastPackage = session.packageName

        val metadata = session.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim()
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim()
        val playing = session.isPlaying
        val transport = session.transportControls
        val state = session.playbackState

        // Playback geometry for local extrapolation: the UI moves the rim itself instead of the
        // source re-pushing a card every second.
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = state?.position ?: 0L
        val positionAt = state?.lastPositionUpdateTime
            ?.takeIf { it > 0L }
            // PlaybackState stamps in elapsed-realtime; the card carries epoch.
            ?.let { System.currentTimeMillis() - (android.os.SystemClock.elapsedRealtime() - it) }
            ?: System.currentTimeMillis()

        // Artwork arrives as an already-decoded bitmap over binder — no disk decode (§5). The
        // instance is cached per track so an unchanged card stays reference-equal and the deck
        // does not re-commit on every refresh.
        val artKey = "${'$'}{session.packageName}|${'$'}title|${'$'}artist"
        if (artKey != lastArtKey) {
            lastArtKey = artKey
            lastArt = runCatching {
                (metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON))
                    ?.asImageBitmap()
            }.getOrNull()
        }

        controller.push(
            CapsuleCard(
                id = "media:${'$'}{session.packageName}",
                sourcePackage = session.packageName,
                kind = SourceKind.MEDIA,
                priority = SourceKind.MEDIA.defaultPriority,
                collapsedText = title.ifEmpty { "Playing" }.take(12),
                title = title.ifEmpty { "Media" },
                subtitle = artist.orEmpty(),
                glyph = CapsuleGlyph.Builtin(BuiltinSymbol.NOTE),
                actions = listOf(
                    CapsuleAction("Previous", intent = null, run = { transport.skipToPrevious() }, symbol = BuiltinSymbol.PREV),
                    if (playing) {
                        CapsuleAction("Pause", intent = null, run = { transport.pause() }, symbol = BuiltinSymbol.PAUSE)
                    } else {
                        CapsuleAction("Play", intent = null, run = { transport.play() }, symbol = BuiltinSymbol.PLAY)
                    },
                    CapsuleAction("Next", intent = null, run = { transport.skipToNext() }, symbol = BuiltinSymbol.NEXT),
                ),
                tapIntent = session.sessionActivity,
                // Dismissal would be undone by the next playback callback re-pushing the card,
                // and a control that comes straight back reads as broken. The card leaves when
                // the session does.
                dismissible = false,
                verified = true,
                artwork = lastArt,
                mediaPlaying = playing,
                mediaDurationMs = duration.coerceAtLeast(0L),
                mediaPositionMs = position.coerceAtLeast(0L),
                mediaPositionAtMs = positionAt,
            ),
        )
    }

    private var lastArtKey: String? = null
    private var lastArt: androidx.compose.ui.graphics.ImageBitmap? = null

    private var lastPackage: String? = null

    /** One sentence for Settings: exactly why a media card is or is not showing. */
    fun status(): String {
        val granted = context.packageName in
            androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
        if (!granted) {
            return "Off. Requires notification access, granted in system settings. Lumen reads " +
                "only which app posted a notification (for the dots) and media sessions — " +
                "never notification content."
        }
        val sessions = runCatching { sessionManager?.getActiveSessions(listenerComponent) }
            .getOrNull()
            ?: return "Access looks granted but Android refused the session list. Toggle the " +
                "access off and on, then reopen Lumen."
        return when {
            sessions.isEmpty() -> "On. Nothing is playing right now; start some music and the " +
                "card appears."
            else -> "On. Following " + sessions.joinToString { it.packageName } + "."
        }
    }

    private fun mediaKeyFor(pkg: String) = "$pkg/${SourceKind.MEDIA.name}"

    private val MediaController.isPlaying: Boolean
        get() = playbackState?.state == PlaybackState.STATE_PLAYING
}

/**
 * The consent token, and since D43 the dot counter. `MediaSessionManager` will only answer on
 * behalf of an enabled notification listener, so this service exists to *be* that listener. What
 * it reads from notifications is exactly one field — which package posted — to light the dots on
 * icons; titles, text and extras are never touched and nothing reaches disk. Disabled, media and
 * dots both fall away and the launcher works fully without them (§10).
 */
class CapsuleNotificationListener : NotificationListenerService() {

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface DotsEntryPoint {
        fun notificationDots(): dev.lumen.launcher.core.data.notifications.NotificationDotsRepository
    }

    override fun onListenerConnected() {
        // D44: the listener shares the launcher's process, so a throw anywhere in here kills the
        // home screen — every callback is wrapped whole, and safe mode silences the lot.
        if (dev.lumen.launcher.core.data.system.SafeMode.active) return
        // The user just flipped the toggle in system settings; media can start flowing now.
        runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, CapsulePushReceiver.ControllerEntryPoint::class.java)
                .capsuleController()
                .refreshMedia()
        }
        runCatching { publishDots() }
    }

    override fun onListenerDisconnected() {
        runCatching { publish(emptySet()) }
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        if (dev.lumen.launcher.core.data.system.SafeMode.active) return
        runCatching { publishDots() }
    }

    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        if (dev.lumen.launcher.core.data.system.SafeMode.active) return
        runCatching { publishDots() }
    }

    /**
     * Recomputed from [getActiveNotifications] every time rather than kept as deltas: the system's
     * list is authoritative, delta bookkeeping drifts on missed callbacks, and the call is cheap
     * at notification cadence.
     */
    private fun publishDots() {
        val active = runCatching {
            activeNotifications.orEmpty().map { it.packageName to it.isOngoing }
        }.getOrDefault(emptyList())
        publish(
            dev.lumen.launcher.core.data.notifications.NotificationDotsRepository
                .dotWorthy(active),
        )
    }

    private fun publish(packages: Set<String>) {
        runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, DotsEntryPoint::class.java)
                .notificationDots()
                .update(packages)
        }
    }
}
