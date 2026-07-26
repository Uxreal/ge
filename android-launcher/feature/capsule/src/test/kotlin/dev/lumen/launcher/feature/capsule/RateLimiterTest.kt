package dev.lumen.launcher.feature.capsule

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** §4.1: four pushes per second, per package. */
class RateLimiterTest {

    @Test
    fun `a burst of four is allowed and the fifth inside the same second is not`() {
        val limiter = RateLimiter()
        repeat(4) { assertTrue("push $it should pass", limiter.allow("com.app", now = 0)) }
        assertFalse(limiter.allow("com.app", now = 100))
        assertFalse(limiter.allow("com.app", now = 999))
    }

    @Test
    fun `the window slides rather than resetting on a fixed boundary`() {
        val limiter = RateLimiter()
        repeat(4) { limiter.allow("com.app", now = it * 100L) }
        // The oldest of the four was at t=0, so t=1000 frees exactly one slot.
        assertTrue(limiter.allow("com.app", now = 1_000))
        assertFalse(limiter.allow("com.app", now = 1_001))
        assertTrue(limiter.allow("com.app", now = 1_100))
    }

    @Test
    fun `one noisy package cannot starve another`() {
        val limiter = RateLimiter()
        repeat(8) { limiter.allow("com.spam", now = 0) }
        assertTrue(limiter.allow("com.quiet", now = 0))
    }
}
