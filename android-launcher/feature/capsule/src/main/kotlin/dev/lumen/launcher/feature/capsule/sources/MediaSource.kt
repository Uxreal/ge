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

    /**
     * Re-reads the world. Called at start, whenever the launcher resumes (the user may have just
     * granted access in system settings), when the listener service connects, and from playback
     * callbacks. Cheap, idempotent, and a silent no-op without consent.
     */
    fun refresh() {
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

        controller.push(
            CapsuleCard(
                id = "media:${session.packageName}",
                sourcePackage = session.packageName,
                kind = SourceKind.MEDIA,
                priority = SourceKind.MEDIA.defaultPriority,
                collapsedText = title.ifEmpty { "Playing" }.take(12),
                title = title.ifEmpty { "Media" },
                subtitle = artist.orEmpty(),
                glyph = CapsuleGlyph.Builtin(BuiltinSymbol.NOTE),
                actions = listOf(
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
            ),
        )
    }

    private var lastPackage: String? = null

    private fun mediaKeyFor(pkg: String) = "$pkg/${SourceKind.MEDIA.name}"

    private val MediaController.isPlaying: Boolean
        get() = playbackState?.state == PlaybackState.STATE_PLAYING
}

/**
 * The consent token. `MediaSessionManager` will only answer on behalf of an enabled notification
 * listener, so this service exists to *be* that listener — it reads nothing and stores nothing,
 * which is exactly what the system's "allow notification access" screen is consenting to here.
 */
class CapsuleNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        // The user just flipped the toggle in system settings; media can start flowing now.
        runCatching {
            EntryPointAccessors
                .fromApplication(applicationContext, CapsulePushReceiver.ControllerEntryPoint::class.java)
                .capsuleController()
                .refreshMedia()
        }
    }
}
