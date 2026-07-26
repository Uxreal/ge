package dev.lumen.launcher.feature.capsule

import android.app.PendingIntent
import androidx.compose.runtime.Immutable

/**
 * §4's four states. [DORMANT] draws nothing at all — the status bar is left alone, which is the
 * whole reason the Capsule is allowed to own the top of the screen when it *is* drawn.
 */
enum class CapsuleState { DORMANT, GLANCE, EXPANDED, STACKED }

/**
 * The activity kind a card represents. Half of §4's dedupe key: a `MediaSession` and a media
 * notification from the same package are one card, never two.
 *
 * [defaultPriority] is §4's priority table. A push may override it, but [SourceKind.THIRD_PARTY]
 * is clamped to [THIRD_PARTY_CEILING] so nothing external can outrank a phone call.
 */
enum class SourceKind(val defaultPriority: Int) {
    CALL(1000),
    NAVIGATION(900),
    ALARM(850),
    CAPTURE(800),
    MEDIA(700),
    TRANSFER(600),
    THIRD_PARTY(250),
    TIMER(400),
    BATTERY(300),
    CONNECTIVITY(200),
    AMBIENT(100),
    ;

    companion object {
        const val THIRD_PARTY_CEILING = 500

        fun parse(raw: String?): SourceKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: THIRD_PARTY
    }
}

/** A glyph the Capsule can draw without loading anything from disk on the main thread. */
sealed interface CapsuleGlyph {
    data object None : CapsuleGlyph

    /** Drawn procedurally in `CapsuleArt` — no drawable decode, no icon-pack dependency. */
    data class Builtin(val symbol: BuiltinSymbol) : CapsuleGlyph

    /** A `content://` or `android.resource://` URI from a push; decoded off the main thread. */
    data class Image(val uri: String) : CapsuleGlyph
}

enum class BuiltinSymbol { CLOCK, BATTERY, CHARGING, ALARM, SPARK, NOTE, PLAY, PAUSE, NEXT }

@Immutable
data class CapsuleAction(
    val label: String,
    val intent: PendingIntent?,
    /**
     * Built-in sources act directly (a media transport control has no `PendingIntent`); pushed
     * cards act through [intent]. When both exist, [run] wins.
     */
    val run: (() -> Unit)? = null,
    /** When set, the chip renders this glyph instead of the label; the label becomes a11y text. */
    val symbol: BuiltinSymbol? = null,
)

/**
 * One source's contribution to the deck.
 *
 * Timestamps are set by [CapsuleArbiter], not by callers: [firstSeenAt] survives updates so a
 * source that re-pushes itself every 200ms still has to serve out §4's 800ms dwell before it may
 * claim the front slot, and [updatedAt] is what expires an abandoned progress card after 30s.
 */
@Immutable
data class CapsuleCard(
    val id: String,
    val sourcePackage: String,
    val kind: SourceKind,
    val priority: Int,
    /** ≤12 chars — the GLANCE pill. Longer strings are truncated at construction, not at draw. */
    val collapsedText: String,
    val title: String,
    val subtitle: String = "",
    /** [NO_PROGRESS], [INDETERMINATE], or 0f..1f. */
    val progress: Float = NO_PROGRESS,
    val glyph: CapsuleGlyph = CapsuleGlyph.None,
    /** ARGB, or 0 for "use the theme". Never used raw — the UI blends it toward the surface. */
    val accentArgb: Int = 0,
    val actions: List<CapsuleAction> = emptyList(),
    val tapIntent: PendingIntent? = null,
    val dismissible: Boolean = true,
    /** Epoch ms, or 0 for "no expiry". */
    val expiresAt: Long = 0L,
    /**
     * True when the source package was proven by a `PendingIntent`'s creator rather than
     * self-declared in an extra. The block list shows the difference rather than pretending
     * an unauthenticated broadcast carries a trustworthy identity.
     */
    val verified: Boolean = false,
    val firstSeenAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** §4's dedupe key: package + activity kind. */
    val dedupeKey: String get() = "$sourcePackage/${kind.name}"

    val hasProgress: Boolean get() = progress != NO_PROGRESS

    companion object {
        const val NO_PROGRESS = -2f
        const val INDETERMINATE = -1f
    }
}

/**
 * The committed deck. Index 0 is the front card; everything after it peeks from behind, in
 * priority order.
 */
@Immutable
data class CapsuleDeck(
    val cards: List<CapsuleCard> = emptyList(),
    /** §4: a source arriving at ≥800 expands itself once, for 2.5s, then settles to GLANCE. */
    val autoExpand: Boolean = false,
) {
    val front: CapsuleCard? get() = cards.firstOrNull()

    val state: CapsuleState
        get() = when {
            cards.isEmpty() -> CapsuleState.DORMANT
            cards.size > 1 -> CapsuleState.STACKED
            else -> CapsuleState.GLANCE
        }

    /**
     * Identity of the deck rather than its contents. A media card whose progress ticks every
     * second must not restart the entry animation, so transitions key off this.
     */
    val signature: String get() = cards.joinToString("|") { it.dedupeKey }

    companion object {
        val Dormant = CapsuleDeck()
    }
}
