package dev.lumen.launcher.feature.capsule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §4's arbitration rules, each one pinned by a test. The arbiter takes `now` on every call, so
 * these run at millisecond precision without sleeping.
 */
class CapsuleArbiterTest {

    private fun card(
        id: String,
        pkg: String = "com.example.$id",
        kind: SourceKind = SourceKind.THIRD_PARTY,
        priority: Int = 250,
        progress: Float = CapsuleCard.NO_PROGRESS,
        expiresAt: Long = 0L,
        dismissible: Boolean = true,
    ) = CapsuleCard(
        id = id,
        sourcePackage = pkg,
        kind = kind,
        priority = priority,
        collapsedText = id.take(12),
        title = id,
        progress = progress,
        expiresAt = expiresAt,
        dismissible = dismissible,
    )

    private val dwell = CapsuleArbiter.FRONT_DWELL_MS
    private val coalesce = CapsuleArbiter.COALESCE_MS

    // ---- rule 3: the 800ms dwell -------------------------------------------------------------

    @Test
    fun `a source is not shown until it has been present for the dwell`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("a"), now = 0)

        assertEquals(CapsuleState.DORMANT, arbiter.resolve(0).state)
        assertEquals(CapsuleState.DORMANT, arbiter.resolve(dwell - 1).state)
        assertEquals(CapsuleState.GLANCE, arbiter.resolve(dwell).state)
    }

    @Test
    fun `a source that re-pushes itself does not re-arm its own dwell`() {
        val arbiter = CapsuleArbiter()
        // The flicker case §4 calls out: a notification that immediately updates itself.
        for (t in 0..900 step 100) arbiter.push(card("a"), now = t.toLong())

        assertEquals(CapsuleState.GLANCE, arbiter.resolve(900).state)
    }

    @Test
    fun `a source that vanishes before the dwell is never shown at all`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("blip"), now = 0)
        arbiter.clear("blip", now = 300)

        assertEquals(CapsuleState.DORMANT, arbiter.resolve(2_000).state)
    }

    // ---- rule 1: dedupe by package + kind ------------------------------------------------------

    @Test
    fun `a media session and a media notification from one package are one card`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("session", pkg = "com.player", kind = SourceKind.MEDIA), now = 0)
        arbiter.push(card("notification", pkg = "com.player", kind = SourceKind.MEDIA), now = 50)

        val deck = arbiter.resolve(dwell)
        assertEquals(1, deck.cards.size)
        assertEquals("notification", deck.front?.id)
        // The update kept the original arrival, so the dwell was served once, not twice.
        assertEquals(CapsuleState.GLANCE, deck.state)
    }

    @Test
    fun `different kinds from one package are different cards`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("m", pkg = "com.app", kind = SourceKind.MEDIA, priority = 700), now = 0)
        arbiter.push(card("d", pkg = "com.app", kind = SourceKind.TRANSFER, priority = 600), now = 0)

        assertEquals(2, arbiter.resolve(dwell).cards.size)
    }

    // ---- rule 4: priority order ----------------------------------------------------------------

    @Test
    fun `highest priority owns the front and the rest stack behind it in order`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("ambient", priority = 100), now = 0)
        arbiter.push(card("call", priority = 1000), now = 0)
        arbiter.push(card("media", priority = 700), now = 0)

        val deck = arbiter.resolve(dwell)
        assertEquals(listOf("call", "media", "ambient"), deck.cards.map { it.id })
        assertEquals(CapsuleState.STACKED, deck.state)
    }

    @Test
    fun `equal priorities keep arrival order so the deck never reshuffles on its own`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("first", priority = 300), now = 0)
        arbiter.push(card("second", priority = 300), now = 10)

        // Each serves its own dwell, so the deck is complete 10ms after the first one qualifies.
        assertEquals(listOf("first"), arbiter.resolve(dwell).cards.map { it.id })
        assertEquals(listOf("first", "second"), arbiter.resolve(dwell + coalesce).cards.map { it.id })
    }

    // ---- rule 6: the 400ms coalesce ------------------------------------------------------------

    @Test
    fun `two state changes inside the coalesce window commit as one`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("a", priority = 100), now = 0)
        val committed = arbiter.resolve(dwell)
        assertEquals(1, committed.cards.size)

        // Both arrive well after the first commit but land inside one coalesce window.
        arbiter.push(card("b", priority = 200), now = dwell)
        arbiter.push(card("c", priority = 300), now = dwell)

        val midway = arbiter.resolve(dwell + coalesce - 1)
        assertEquals("nothing commits inside the window", 1, midway.cards.size)

        val settled = arbiter.resolve(dwell + dwell + coalesce)
        assertEquals("the final state is what lands", 3, settled.cards.size)
    }

    @Test
    fun `a withdrawal is never held back by the coalesce window`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("call", priority = 1000), now = 0)
        arbiter.push(card("clock", priority = 100), now = 0)
        assertEquals("call", arbiter.resolve(dwell).front?.id)

        // A call that ended must leave the front immediately, not 400ms later.
        arbiter.clear("call", now = dwell + 10)
        assertEquals("clock", arbiter.resolve(dwell + 10).front?.id)
    }

    // ---- rule 5: manual override ---------------------------------------------------------------

    @Test
    fun `a swiped card holds the front for eight seconds then normal order resumes`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("call", priority = 1000), now = 0)
        arbiter.push(card("media", priority = 700), now = 0)
        assertEquals("call", arbiter.resolve(dwell).front?.id)

        val swipedAt = dwell + 100
        arbiter.pin(card("media").dedupeKey, now = swipedAt)
        assertEquals("media", arbiter.resolve(swipedAt).front?.id)
        assertTrue(arbiter.isPinned(swipedAt + 7_000))
        assertEquals("media", arbiter.resolve(swipedAt + 7_000).front?.id)

        assertFalse(arbiter.isPinned(swipedAt + CapsuleArbiter.MANUAL_HOLD_MS))
        assertEquals("call", arbiter.resolve(swipedAt + CapsuleArbiter.MANUAL_HOLD_MS).front?.id)
    }

    @Test
    fun `pinning a card that is not in the deck does nothing`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("only", priority = 100), now = 0)
        arbiter.pin("com.ghost/MEDIA", now = 0)

        assertFalse(arbiter.isPinned(0))
        assertEquals("only", arbiter.resolve(dwell).front?.id)
    }

    @Test
    fun `a pinned card that expires releases the pin`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("call", priority = 1000), now = 0)
        arbiter.push(card("media", priority = 700, expiresAt = 5_000), now = 0)
        arbiter.pin(card("media").dedupeKey, now = dwell)
        assertEquals("media", arbiter.resolve(dwell).front?.id)

        assertEquals("call", arbiter.resolve(5_001).front?.id)
        assertFalse(arbiter.isPinned(5_001))
    }

    // ---- expiry ---------------------------------------------------------------------------------

    @Test
    fun `a card expires at its expiresAt`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("plug", expiresAt = 6_000), now = 0)

        assertEquals(CapsuleState.GLANCE, arbiter.resolve(5_999).state)
        assertEquals(CapsuleState.DORMANT, arbiter.resolve(6_000).state)
    }

    @Test
    fun `an abandoned progress card expires after thirty seconds of silence`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("download", progress = 0.4f), now = 0)
        assertEquals(CapsuleState.GLANCE, arbiter.resolve(dwell).state)

        val timeout = CapsuleArbiter.PROGRESS_TIMEOUT_MS
        assertEquals(CapsuleState.GLANCE, arbiter.resolve(timeout).state)
        assertEquals(CapsuleState.DORMANT, arbiter.resolve(timeout + 1).state)
    }

    @Test
    fun `a progress card that keeps updating never expires`() {
        val arbiter = CapsuleArbiter()
        var now = 0L
        repeat(60) {
            arbiter.push(card("download", progress = it / 60f), now = now)
            now += 1_000
        }
        assertEquals(CapsuleState.GLANCE, arbiter.resolve(now).state)
    }

    @Test
    fun `a card with no progress never times out`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("vpn"), now = 0)
        assertEquals(CapsuleState.GLANCE, arbiter.resolve(10 * 60 * 1_000L).state)
    }

    // ---- auto-expand -----------------------------------------------------------------------------

    @Test
    fun `a new source at or above eight hundred auto-expands exactly once`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("nav", priority = 900), now = 0)
        assertTrue(arbiter.resolve(dwell).autoExpand)

        // Still inside the 2.5s window.
        assertTrue(arbiter.resolve(dwell + 2_000).autoExpand)
        // And past it.
        assertFalse(arbiter.resolve(dwell + CapsuleArbiter.AUTO_EXPAND_MS + 1).autoExpand)

        // Updating the same source does not expand it again.
        arbiter.push(card("nav", priority = 900), now = 20_000)
        assertFalse(arbiter.resolve(20_000).autoExpand)
    }

    @Test
    fun `a source below eight hundred arrives silently`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("media", priority = 700), now = 0)
        assertFalse(arbiter.resolve(dwell).autoExpand)
    }

    // ---- deadlines --------------------------------------------------------------------------------

    @Test
    fun `an empty arbiter schedules no wakeups`() {
        assertNull(CapsuleArbiter().nextDeadline(now = 1_000))
    }

    @Test
    fun `the next deadline is the moment a pending source becomes eligible`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("a"), now = 1_000)
        arbiter.resolve(1_000)

        assertEquals(1_000 + dwell, arbiter.nextDeadline(now = 1_000))
    }

    @Test
    fun `the next deadline accounts for expiry`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("a", expiresAt = 4_000), now = 0)
        arbiter.resolve(dwell)

        assertEquals(4_000L, arbiter.nextDeadline(now = dwell))
    }

    @Test
    fun `a settled deck with nothing pending schedules no wakeups`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("vpn"), now = 0)
        arbiter.resolve(dwell)

        assertNull(arbiter.nextDeadline(now = dwell + 1))
    }

    // ---- package-wide clear (the block list) --------------------------------------------------------

    @Test
    fun `blocking a package drops everything it pushed`() {
        val arbiter = CapsuleArbiter()
        arbiter.push(card("m", pkg = "com.spam", kind = SourceKind.MEDIA), now = 0)
        arbiter.push(card("d", pkg = "com.spam", kind = SourceKind.TRANSFER), now = 0)
        arbiter.push(card("keep", pkg = "com.other"), now = 0)
        assertEquals(3, arbiter.resolve(dwell).cards.size)

        arbiter.clearPackage("com.spam", now = dwell)
        val deck = arbiter.resolve(dwell)
        assertEquals(1, deck.cards.size)
        assertEquals("keep", deck.front?.id)
    }
}
