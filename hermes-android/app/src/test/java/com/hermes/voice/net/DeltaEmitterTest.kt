package com.hermes.voice.net

import org.junit.Assert.assertEquals
import org.junit.Test

class DeltaEmitterTest {

    @Test
    fun `passes through true deltas untouched`() {
        val out = mutableListOf<String>()
        val emitter = DeltaEmitter { out += it }
        emitter.emit("Hel")
        emitter.emit("lo ")
        emitter.emit("there")
        assertEquals(listOf("Hel", "lo ", "there"), out)
        assertEquals("Hello there", out.joinToString(""))
    }

    @Test
    fun `collapses cumulative frames to their new tail`() {
        val out = mutableListOf<String>()
        val emitter = DeltaEmitter { out += it }
        emitter.emit("Hello")
        emitter.emit("Hello there")
        emitter.emit("Hello there, friend")
        assertEquals(listOf("Hello", " there", ", friend"), out)
        assertEquals("Hello there, friend", out.joinToString(""))
    }

    @Test
    fun `a repeated chunk that is not an extension is kept`() {
        val out = mutableListOf<String>()
        val emitter = DeltaEmitter { out += it }
        emitter.emit("ha")
        emitter.emit("ha")
        assertEquals("haha", out.joinToString(""))
    }

    @Test
    fun `ignores empty chunks`() {
        val out = mutableListOf<String>()
        val emitter = DeltaEmitter { out += it }
        emitter.emit("")
        emitter.emit("x")
        assertEquals(listOf("x"), out)
    }
}
