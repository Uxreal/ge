package dev.lumen.launcher.core.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

/** D43's dot policy: ongoing notifications never earn a dot; everything else counts once. */
class NotificationDotsRepositoryTest {

    @Test
    fun `ongoing notifications earn no dot`() {
        val active = listOf(
            "com.music" to true, // foreground playback: not news
            "com.messages" to false,
        )
        assertEquals(setOf("com.messages"), NotificationDotsRepository.dotWorthy(active))
    }

    @Test
    fun `many notifications from one app are one dot`() {
        val active = listOf(
            "com.messages" to false,
            "com.messages" to false,
            "com.messages" to false,
        )
        assertEquals(setOf("com.messages"), NotificationDotsRepository.dotWorthy(active))
    }

    @Test
    fun `an app with both an ongoing and a clearable notification still gets its dot`() {
        val active = listOf(
            "com.nav" to true,
            "com.nav" to false,
        )
        assertEquals(setOf("com.nav"), NotificationDotsRepository.dotWorthy(active))
    }

    @Test
    fun `no notifications means no dots`() {
        assertEquals(emptySet<String>(), NotificationDotsRepository.dotWorthy(emptyList()))
    }
}
