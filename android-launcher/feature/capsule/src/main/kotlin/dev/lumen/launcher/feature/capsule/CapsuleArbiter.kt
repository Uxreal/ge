package dev.lumen.launcher.feature.capsule

/**
 * §4's arbitration algorithm, implemented exactly and with no clock of its own — every entry point
 * takes `now`, which is what makes the whole thing unit-testable at millisecond precision instead
 * of by sleeping.
 *
 * The rules, in the order they apply:
 *
 *  1. **Dedupe by package + activity kind.** One card per [CapsuleCard.dedupeKey], always.
 *  2. **Expiry.** `expiresAt`, explicit clear, or 30s of silence on a progress card.
 *  3. **800ms dwell.** A source is not eligible for the front slot until it has been continuously
 *     present for [FRONT_DWELL_MS]. A source that re-pushes itself immediately does not flicker
 *     the pill, because [push] preserves `firstSeenAt` across updates.
 *  4. **Priority order**, ties broken by arrival so the deck never reshuffles arbitrarily.
 *  5. **Manual override.** A card the user swiped to holds the front for [MANUAL_HOLD_MS].
 *  6. **400ms coalesce.** At most one committed state change per [COALESCE_MS]; intermediate
 *     states are dropped and the UI animates straight to the final one.
 *
 * Not thread-safe: [CapsuleController] confines it to a single coroutine.
 */
class CapsuleArbiter(
    private val dwellMs: Long = FRONT_DWELL_MS,
    private val coalesceMs: Long = COALESCE_MS,
) {
    private val entries = LinkedHashMap<String, CapsuleCard>()
    private val autoExpanded = mutableSetOf<String>()

    private var manualKey: String? = null
    private var manualUntil = 0L
    private var autoExpandKey: String? = null
    private var autoExpandUntil = 0L

    private var committed = CapsuleDeck.Dormant

    // Half of MIN_VALUE, not MIN_VALUE: the coalesce check is `now - committedAt`, and subtracting
    // the true minimum overflows to a negative result that would suppress every commit forever.
    private var committedAt = Long.MIN_VALUE / 2

    /**
     * Adds or updates a source. Returns the card as stored, with arbiter-owned timestamps applied.
     *
     * An update keeps the original `firstSeenAt` — that is rule 3. It is the difference between a
     * download that re-pushes progress twice a second sitting calmly at the front and one that
     * re-arms its own dwell forever and never appears.
     */
    fun push(card: CapsuleCard, now: Long): CapsuleCard {
        val key = card.dedupeKey
        val existing = entries[key]
        val stored = card.copy(
            firstSeenAt = existing?.firstSeenAt ?: now,
            updatedAt = now,
        )
        entries[key] = stored

        // Rule: a *newly arriving* high-priority source auto-expands once. "Once" is tracked by
        // key for the process lifetime, so a navigation step that updates every corner expands
        // when the trip starts and never again.
        if (existing == null && stored.priority >= AUTO_EXPAND_PRIORITY && autoExpanded.add(key)) {
            autoExpandKey = key
            // The expansion is visible only once the card clears its dwell, so the window starts
            // there rather than at arrival.
            autoExpandUntil = now + dwellMs + AUTO_EXPAND_MS
        }
        return stored
    }

    /** §4.1's CLEAR. Matches on the push-supplied id, which is unique per (source, key). */
    fun clear(id: String, now: Long) {
        val key = entries.entries.firstOrNull { it.value.id == id }?.key ?: return
        remove(key, now)
    }

    /** Withdraws a built-in source (the charger was unplugged, the alarm was cancelled). */
    fun clearKey(key: String, now: Long) = remove(key, now)

    /** Drops every card a package pushed — the block list's teeth. */
    fun clearPackage(pkg: String, now: Long) {
        entries.values.filter { it.sourcePackage == pkg }.forEach { remove(it.dedupeKey, now) }
    }

    private fun remove(key: String, now: Long) {
        if (entries.remove(key) == null) return
        if (manualKey == key) {
            manualKey = null
            manualUntil = 0L
        }
        if (autoExpandKey == key) {
            autoExpandKey = null
            autoExpandUntil = 0L
        }
        // Withdrawal is not a state change the coalescer may sit on: a call that ended must leave
        // the front immediately, so the next resolve() is allowed to commit.
        committedAt = now - coalesceMs
    }

    /** The user swiped the deck. That card owns the front for 8s regardless of priority. */
    fun pin(key: String, now: Long) {
        if (!entries.containsKey(key)) return
        manualKey = key
        manualUntil = now + MANUAL_HOLD_MS
        committedAt = now - coalesceMs
    }

    /** True while a user swipe is holding a card at the front. */
    fun isPinned(now: Long): Boolean = manualKey != null && now < manualUntil

    /** The current committed deck, advancing the state machine to [now]. */
    fun resolve(now: Long): CapsuleDeck {
        prune(now)
        val target = compose(now)
        if (target == committed) return committed
        if (now - committedAt < coalesceMs) return committed
        committed = target
        committedAt = now
        return committed
    }

    /**
     * The earliest instant at which [resolve] could return something different, so the controller
     * can sleep until then instead of polling. Null means "nothing pending" — a still Capsule
     * costs no wakeups at all.
     */
    fun nextDeadline(now: Long): Long? {
        val deadlines = ArrayList<Long>(entries.size * 2 + 3)
        for (card in entries.values) {
            deadlines += card.firstSeenAt + dwellMs
            if (card.expiresAt > 0L) deadlines += card.expiresAt
            if (card.hasProgress) deadlines += card.updatedAt + PROGRESS_TIMEOUT_MS
        }
        deadlines += manualUntil
        deadlines += autoExpandUntil
        if (compose(now) != committed) deadlines += committedAt + coalesceMs

        return deadlines.filter { it > now }.minOrNull()
    }

    private fun prune(now: Long) {
        val iterator = entries.values.iterator()
        var removedManual = false
        var removedAutoExpand = false
        var removedAny = false
        while (iterator.hasNext()) {
            val card = iterator.next()
            val expired = card.expiresAt in 1..now
            val abandoned = card.hasProgress && now - card.updatedAt > PROGRESS_TIMEOUT_MS
            if (expired || abandoned) {
                iterator.remove()
                removedAny = true
                if (card.dedupeKey == manualKey) removedManual = true
                if (card.dedupeKey == autoExpandKey) removedAutoExpand = true
            }
        }
        if (removedManual) {
            manualKey = null
            manualUntil = 0L
        }
        if (removedAutoExpand) {
            autoExpandKey = null
            autoExpandUntil = 0L
        }
        // An expiry is a withdrawal, and withdrawals are not coalesced: leaving an alarm that has
        // already rung on screen for another 400ms is worse than the flicker the rule prevents.
        if (removedAny) committedAt = now - coalesceMs
    }

    private fun compose(now: Long): CapsuleDeck {
        val eligible = entries.values
            .filter { now - it.firstSeenAt >= dwellMs }
            .sortedWith(compareByDescending<CapsuleCard> { it.priority }.thenBy { it.firstSeenAt })
        if (eligible.isEmpty()) return CapsuleDeck.Dormant

        val pinned = manualKey?.takeIf { now < manualUntil }
        val ordered = if (pinned == null) {
            eligible
        } else {
            val index = eligible.indexOfFirst { it.dedupeKey == pinned }
            if (index <= 0) {
                eligible
            } else {
                ArrayList<CapsuleCard>(eligible.size).apply {
                    add(eligible[index])
                    eligible.forEachIndexed { i, card -> if (i != index) add(card) }
                }
            }
        }

        return CapsuleDeck(
            cards = ordered,
            autoExpand = ordered.first().dedupeKey == autoExpandKey && now < autoExpandUntil,
        )
    }

    companion object {
        /** §4: a source must be continuously present this long before it may claim the front. */
        const val FRONT_DWELL_MS = 800L

        /** §4: no two committed state changes closer together than this. */
        const val COALESCE_MS = 400L

        /** §4: priority at or above which a new arrival expands itself once. */
        const val AUTO_EXPAND_PRIORITY = 800

        const val AUTO_EXPAND_MS = 2_500L

        /** §4: a card the user swiped to stays front this long, then normal ordering resumes. */
        const val MANUAL_HOLD_MS = 8_000L

        /** §4: progress-type sources expire after this long with no update. */
        const val PROGRESS_TIMEOUT_MS = 30_000L
    }
}
