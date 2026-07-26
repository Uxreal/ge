package dev.lumen.launcher.feature.capsule.push

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.lumen.launcher.feature.capsule.CapsuleAction
import dev.lumen.launcher.feature.capsule.CapsuleCard
import dev.lumen.launcher.feature.capsule.CapsuleController
import dev.lumen.launcher.feature.capsule.CapsuleGlyph
import dev.lumen.launcher.feature.capsule.SourceKind

/**
 * §4.1's public API. Any app, Tasker task or `adb shell am broadcast` can push a card; `README.md`
 * carries a copy-pasteable example.
 *
 * Everything is validated and malformed pushes are dropped silently (logged at debug), because a
 * launcher that can be crashed by a broadcast is a launcher that can be crashed by any installed
 * app. Three specific defences:
 *
 *  * **Priority is clamped to 0..500** in [CapsuleController], so a third party can never outrank
 *    an incoming call.
 *  * **Identity is proven where possible.** A broadcast carries no trustworthy caller package, but
 *    `PendingIntent.getCreatorPackage()` is filled in by the system and cannot be forged. When a
 *    push supplies a tap intent or an action, that is the package we attribute it to; only when it
 *    supplies neither do we fall back to the self-declared `pkg` extra, and the card is then marked
 *    unverified so the block-list screen can say so.
 *  * **Rate limited** to four pushes per second per package.
 */
class CapsulePushReceiver : BroadcastReceiver() {

    /**
     * Resolved through an entry point rather than `@AndroidEntryPoint` field injection: Hilt's
     * receiver support needs a `super.onReceive()` call that Kotlin will not compile, because at
     * compile time the superclass member is still abstract. Fetching the singleton explicitly is
     * the same dependency with none of that.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ControllerEntryPoint {
        fun capsuleController(): CapsuleController
    }

    override fun onReceive(context: Context, intent: Intent) {
        val controller = runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, ControllerEntryPoint::class.java)
                .capsuleController()
        }.getOrElse {
            Log.d(TAG, "capsule unavailable", it)
            return
        }

        when (intent.action) {
            ACTION_PUSH -> parse(context, intent)?.let(controller::pushExternal)
                ?: Log.d(TAG, "dropped malformed push")

            ACTION_CLEAR -> intent.getStringExtra(EXTRA_ID)
                ?.takeIf { it.isNotBlank() }
                ?.let(controller::clear)
        }
    }

    private fun parse(context: Context, intent: Intent): CapsuleCard? {
        val id = intent.getStringExtra(EXTRA_ID)?.trim()?.take(MAX_ID) ?: return null
        if (id.isEmpty()) return null

        val tapIntent = intent.pendingIntentExtra(EXTRA_TAP_INTENT)
        val actions = parseActions(intent)

        val creator = tapIntent?.creatorPackage
            ?: actions.firstNotNullOfOrNull { it.intent?.creatorPackage }
        val declared = intent.getStringExtra(EXTRA_PACKAGE)?.trim()?.take(MAX_PACKAGE)
        val sourcePackage = creator ?: declared ?: UNATTRIBUTED

        // Nothing may impersonate the launcher's own built-in sources.
        if (sourcePackage == context.packageName) return null

        val title = intent.getStringExtra(EXTRA_TITLE)?.trim()?.take(MAX_TITLE).orEmpty()
        val collapsed = intent.getStringExtra(EXTRA_COLLAPSED)?.trim()?.take(MAX_COLLAPSED)
            ?: title.take(MAX_COLLAPSED)
        if (collapsed.isEmpty() && title.isEmpty()) return null

        val progress = intent.getFloatExtra(EXTRA_PROGRESS, CapsuleCard.NO_PROGRESS).let {
            when {
                it == CapsuleCard.INDETERMINATE -> CapsuleCard.INDETERMINATE
                it in 0f..1f -> it
                else -> CapsuleCard.NO_PROGRESS
            }
        }

        val expiresAt = intent.getLongExtra(EXTRA_EXPIRES_AT, 0L).let { raw ->
            // An expiry in the past would make the card flicker in and straight back out; an
            // absurdly distant one would pin it forever. Both become "no expiry".
            if (raw <= 0L || raw > System.currentTimeMillis() + MAX_LIFETIME_MS) 0L else raw
        }

        return CapsuleCard(
            id = id,
            sourcePackage = sourcePackage,
            kind = SourceKind.THIRD_PARTY,
            priority = intent.getIntExtra(EXTRA_PRIORITY, DEFAULT_PRIORITY),
            collapsedText = collapsed,
            title = title.ifEmpty { collapsed },
            subtitle = intent.getStringExtra(EXTRA_SUBTITLE)?.trim()?.take(MAX_SUBTITLE).orEmpty(),
            progress = progress,
            glyph = intent.getStringExtra(EXTRA_ICON_URI)
                ?.takeIf { it.startsWith("content://") || it.startsWith("android.resource://") }
                ?.let(CapsuleGlyph::Image)
                ?: CapsuleGlyph.Builtin(dev.lumen.launcher.feature.capsule.BuiltinSymbol.SPARK),
            accentArgb = intent.getIntExtra(EXTRA_ACCENT, 0),
            actions = actions,
            tapIntent = tapIntent,
            dismissible = intent.getBooleanExtra(EXTRA_DISMISSIBLE, true),
            expiresAt = expiresAt,
            verified = creator != null,
        )
    }

    private fun parseActions(intent: Intent): List<CapsuleAction> =
        (0 until MAX_ACTIONS).mapNotNull { index ->
            val label = intent.getStringExtra("$EXTRA_ACTION_LABEL$index")
                ?.trim()?.take(MAX_ACTION_LABEL)
                ?: return@mapNotNull null
            if (label.isEmpty()) return@mapNotNull null
            CapsuleAction(label, intent.pendingIntentExtra("$EXTRA_ACTION_INTENT$index"))
        }

    private fun Intent.pendingIntentExtra(key: String): PendingIntent? =
        runCatching {
            @Suppress("DEPRECATION")
            getParcelableExtra(key) as? PendingIntent
        }.getOrNull()

    companion object {
        // DECISIONS D3 fixes the intent namespace to the application id.
        const val ACTION_PUSH = "dev.lumen.launcher.capsule.PUSH"
        const val ACTION_CLEAR = "dev.lumen.launcher.capsule.CLEAR"

        const val EXTRA_ID = "id"
        const val EXTRA_PACKAGE = "pkg"
        const val EXTRA_PRIORITY = "priority"
        const val EXTRA_COLLAPSED = "collapsedText"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUBTITLE = "subtitle"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_ICON_URI = "iconUri"
        const val EXTRA_ACCENT = "accentColor"
        const val EXTRA_TAP_INTENT = "tapIntent"
        const val EXTRA_EXPIRES_AT = "expiresAt"
        const val EXTRA_DISMISSIBLE = "dismissible"

        /** `actionLabel0`..`actionLabel2` with matching `actionIntent0`..`actionIntent2`. */
        const val EXTRA_ACTION_LABEL = "actionLabel"
        const val EXTRA_ACTION_INTENT = "actionIntent"

        /** Shown in the block list when a push carried no provable identity. */
        const val UNATTRIBUTED = "(unattributed)"

        private const val TAG = "Capsule"
        private const val DEFAULT_PRIORITY = 250
        private const val MAX_ACTIONS = 3
        private const val MAX_ID = 128
        private const val MAX_PACKAGE = 128
        private const val MAX_COLLAPSED = 12
        private const val MAX_TITLE = 80
        private const val MAX_SUBTITLE = 120
        private const val MAX_ACTION_LABEL = 20
        private const val MAX_LIFETIME_MS = 24L * 60 * 60 * 1000
    }
}
