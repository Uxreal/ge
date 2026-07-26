package com.hermes.voice.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakePhraseTest {

    @Test
    fun `matches the phrase exactly`() {
        assertTrue(WakePhrase.matches("hey hermes", "hey hermes"))
    }

    @Test
    fun `matches despite casing and punctuation`() {
        assertTrue(WakePhrase.matches("Hey, Hermes!", "hey hermes"))
    }

    @Test
    fun `matches a mangled transcription`() {
        // What on-device recognition actually returns for this phrase.
        assertTrue(WakePhrase.matches("hey her mess", "hey hermes"))
        assertTrue(WakePhrase.matches("hey hermez", "hey hermes"))
    }

    @Test
    fun `matches when the phrase is buried in a longer utterance`() {
        assertTrue(WakePhrase.matches("okay so hey hermes what is the build status", "hey hermes"))
    }

    @Test
    fun `does not match unrelated speech`() {
        assertFalse(WakePhrase.matches("what is the weather tomorrow", "hey hermes"))
        assertFalse(WakePhrase.matches("turn on the kitchen lights", "hey hermes"))
    }

    @Test
    fun `never matches on a blank phrase`() {
        assertFalse(WakePhrase.matches("anything at all", ""))
    }

    @Test
    fun `returns the command after the wake phrase`() {
        assertEquals(
            "what is the build status",
            WakePhrase.remainderAfterWake("hey hermes what is the build status", "hey hermes"),
        )
    }

    @Test
    fun `returns empty when nothing follows the wake phrase`() {
        assertEquals("", WakePhrase.remainderAfterWake("hey hermes", "hey hermes"))
    }

    @Test
    fun `normalizes to comparable text`() {
        assertEquals("hey hermes", WakePhrase.normalize("  Hey,   HERMES!! "))
    }
}
