package com.hermes.voice.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun `strips markdown emphasis`() {
        assertEquals("Really important", SpeechText.forSpeech("**Really** *important*"))
    }

    @Test
    fun `strips headings and bullets`() {
        val spoken = SpeechText.forSpeech("## Status\n- one\n- two")
        assertFalse(spoken.contains("#"))
        assertFalse(spoken.contains("- "))
        assertTrue(spoken.contains("Status"))
    }

    @Test
    fun `replaces fenced code with a spoken marker`() {
        val spoken = SpeechText.forSpeech("Run this:\n```\nrm -rf /tmp/x\n```\nDone.")
        assertFalse(spoken.contains("rm -rf"))
        assertTrue(spoken.contains("code block"))
        assertTrue(spoken.contains("Done."))
    }

    @Test
    fun `keeps link text and drops the url`() {
        val spoken = SpeechText.forSpeech("See [the dashboard](https://example.com/very/long)")
        assertTrue(spoken.contains("the dashboard"))
        assertFalse(spoken.contains("example.com"))
    }

    @Test
    fun `chunks on a sentence boundary`() {
        val chunk = SpeechText.takeSpeakableChunk("All done. Next up", force = false)
        assertEquals("All done.", chunk?.first)
        assertEquals(" Next up", chunk?.second)
    }

    @Test
    fun `waits for a boundary before speaking`() {
        assertNull(SpeechText.takeSpeakableChunk("still thinking about", force = false))
    }

    @Test
    fun `does not split a decimal number`() {
        assertNull(SpeechText.takeSpeakableChunk("the value is 3.5 and", force = false))
    }

    @Test
    fun `force flushes whatever is buffered`() {
        val chunk = SpeechText.takeSpeakableChunk("no punctuation here", force = true)
        assertEquals("no punctuation here", chunk?.first)
        assertEquals("", chunk?.second)
    }

    @Test
    fun `flushes at a word break when a sentence runs very long`() {
        val long = "word ".repeat(80)
        val chunk = SpeechText.takeSpeakableChunk(long, force = false)
        assertTrue(chunk != null)
        assertTrue(chunk!!.first.isNotEmpty())
        assertTrue(chunk.second.isNotEmpty())
    }

    @Test
    fun `newline ends a chunk`() {
        val chunk = SpeechText.takeSpeakableChunk("First line\nsecond", force = false)
        assertEquals("First line", chunk?.first)
    }
}
