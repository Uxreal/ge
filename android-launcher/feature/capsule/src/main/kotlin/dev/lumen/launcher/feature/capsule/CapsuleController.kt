package dev.lumen.launcher.feature.capsule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.lumen.launcher.core.data.prefs.PrefsRepository
import dev.lumen.launcher.feature.capsule.push.CapsulePushParser
import dev.lumen.launcher.feature.capsule.sources.MediaSource
import dev.lumen.launcher.feature.capsule.sources.SystemSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the Capsule's state for the process: one [CapsuleArbiter] confined to one coroutine, fed by
 * a command channel so broadcast receivers, the UI and the built-in system sources never touch the
 * arbiter concurrently.
 *
 * The loop is event-driven, not polled. After each resolve it asks the arbiter when the state could
 * next change and sleeps exactly that long; with a still deck and no deadlines it blocks on the
 * channel and costs nothing.
 */
@Singleton
class CapsuleController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository,
    private val scope: CoroutineScope,
) {
    private val arbiter = CapsuleArbiter()
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val rateLimiter = RateLimiter()

    private val _deck = MutableStateFlow(CapsuleDeck.Dormant)
    val deck: StateFlow<CapsuleDeck> = _deck.asStateFlow()

    private val sources = SystemSources(context, this)
    private val media = MediaSource(context, this)
    private var started = false

    /**
     * §4.1's live half. A manifest receiver stopped seeing implicit broadcasts in Android 8, so
     * `am broadcast -a dev.lumen.launcher.capsule.PUSH` only works through a context-registered
     * receiver — and the launcher, being HOME, is effectively always running to hold one. Exported
     * on purpose: the API is public by design, and its defences are validation, the clamp, the
     * rate limit and the block list, not a permission wall.
     */
    private val pushReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            CapsulePushParser.handle(receiverContext, intent, this@CapsuleController)
        }
    }

    /** Called once from the Application. Idempotent. */
    fun start() {
        if (started) return
        started = true
        scope.launch { runLoop() }
        sources.start(scope)
        media.refresh()
        ContextCompat.registerReceiver(
            context,
            pushReceiver,
            IntentFilter().apply {
                addAction(CapsulePushParser.ACTION_PUSH)
                addAction(CapsulePushParser.ACTION_CLEAR)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    // ---- inbound -----------------------------------------------------------------------------

    fun push(card: CapsuleCard) {
        commands.trySend(Command.Push(card))
    }

    fun clear(id: String) {
        commands.trySend(Command.Clear(id))
    }

    fun clearKey(key: String) {
        commands.trySend(Command.ClearKey(key))
    }

    /** A third-party push, subject to §4.1's clamp, rate limit and block list. */
    fun pushExternal(card: CapsuleCard) {
        commands.trySend(Command.External(card))
    }

    /** The user swiped the deck to [key]; §4 holds it at the front for 8 seconds. */
    fun pinFront(key: String) {
        commands.trySend(Command.Pin(key))
    }

    fun dismiss(card: CapsuleCard) {
        if (!card.dismissible) return
        clear(card.id)
    }

    /** Re-checks media-session access; called on resume and when the listener connects. */
    fun refreshMedia() = media.refresh()

    fun blockPackage(pkg: String) {
        prefs.blockCapsulePackage(pkg)
        commands.trySend(Command.ClearPackage(pkg))
    }

    fun unblockPackage(pkg: String) = prefs.unblockCapsulePackage(pkg)

    // ---- loop --------------------------------------------------------------------------------

    private suspend fun runLoop() {
        while (scope.isActive) {
            val now = System.currentTimeMillis()
            _deck.value = arbiter.resolve(now)

            val deadline = arbiter.nextDeadline(now)
            val command = if (deadline == null) {
                commands.receive()
            } else {
                withTimeoutOrNull((deadline - now).coerceAtLeast(1L)) { commands.receive() }
            }
            command?.let(::apply)
            // Drain anything that queued while we were resolving, so a burst of pushes produces
            // one recomposition rather than one per push.
            while (true) {
                apply(commands.tryReceive().getOrNull() ?: break)
            }
        }
    }

    private fun apply(command: Command) {
        val now = System.currentTimeMillis()
        when (command) {
            is Command.Push -> arbiter.push(command.card, now)
            is Command.Clear -> arbiter.clear(command.id, now)
            is Command.ClearKey -> arbiter.clearKey(command.key, now)
            is Command.ClearPackage -> arbiter.clearPackage(command.pkg, now)
            is Command.Pin -> arbiter.pin(command.key, now)
            is Command.External -> applyExternal(command.card, now)
        }
    }

    private fun applyExternal(card: CapsuleCard, now: Long) {
        val snapshot = prefs.prefs.value
        if (card.sourcePackage in snapshot.capsuleBlockedPackages) return
        if (!rateLimiter.allow(card.sourcePackage, now)) {
            Log.d(TAG, "rate limited: ${card.sourcePackage}")
            return
        }
        // Every package that has ever pushed is remembered so the settings screen can list it —
        // a block list you can only populate by guessing package names is not a block list.
        if (card.sourcePackage !in snapshot.capsuleSeenPackages) {
            prefs.rememberCapsulePackage(card.sourcePackage)
        }
        arbiter.push(
            card.copy(
                kind = SourceKind.THIRD_PARTY,
                priority = card.priority.coerceIn(0, SourceKind.THIRD_PARTY_CEILING),
            ),
            now,
        )
    }

    private sealed interface Command {
        data class Push(val card: CapsuleCard) : Command
        data class External(val card: CapsuleCard) : Command
        data class Clear(val id: String) : Command
        data class ClearKey(val key: String) : Command
        data class ClearPackage(val pkg: String) : Command
        data class Pin(val key: String) : Command
    }

    private companion object {
        const val TAG = "Capsule"
    }
}

/**
 * §4.1's 4 pushes/second, per package, as a sliding window of the last four arrival times. Bounded
 * in memory: a package that stops pushing is forgotten on the next sweep.
 */
internal class RateLimiter(
    private val burst: Int = 4,
    private val windowMs: Long = 1_000L,
) {
    private val recent = HashMap<String, LongArray>()
    private val cursor = HashMap<String, Int>()

    fun allow(pkg: String, now: Long): Boolean {
        // Seeded far in the past rather than at zero, so a package's first four pushes are not
        // measured against an epoch timestamp — and halved so `now - it` cannot overflow.
        val times = recent.getOrPut(pkg) { LongArray(burst) { Long.MIN_VALUE / 2 } }
        val index = cursor.getOrDefault(pkg, 0)
        // times[index] is the oldest of the last `burst` arrivals.
        if (now - times[index] < windowMs) return false
        times[index] = now
        cursor[pkg] = (index + 1) % burst
        if (recent.size > MAX_TRACKED) sweep(now)
        return true
    }

    private fun sweep(now: Long) {
        recent.entries.removeAll { (_, times) -> times.all { it < now - windowMs * 60 } }
        cursor.keys.retainAll(recent.keys)
    }

    private companion object {
        const val MAX_TRACKED = 128
    }
}
