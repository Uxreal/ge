package com.hermes.voice.engine

import com.hermes.voice.data.Macro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommandsTest {

    private val macros = listOf(
        Macro(phrase = "status report", prompt = "Give me a full status report."),
        Macro(phrase = "deploy check", prompt = "Check the deploy pipeline.", enabled = false),
    )

    @Test
    fun `bare stop cancels locally`() {
        assertEquals(LocalCommand.Cancel, LocalCommands.resolve("stop", macros))
        assertEquals(LocalCommand.Cancel, LocalCommands.resolve("Never mind.", macros))
    }

    @Test
    fun `stop inside a real instruction still reaches the agent`() {
        val command = LocalCommands.resolve("stop the nightly deploy job", macros)
        assertTrue(command is LocalCommand.Send)
        assertEquals("stop the nightly deploy job", (command as LocalCommand.Send).text)
    }

    @Test
    fun `new conversation is handled locally`() {
        assertEquals(LocalCommand.NewConversation, LocalCommands.resolve("start over", macros))
    }

    @Test
    fun `repeat is handled locally`() {
        assertEquals(LocalCommand.RepeatLast, LocalCommands.resolve("say that again", macros))
    }

    @Test
    fun `stop listening ends the voice session`() {
        assertEquals(LocalCommand.StopVoice, LocalCommands.resolve("go to sleep", macros))
    }

    @Test
    fun `an enabled macro expands to its prompt`() {
        val command = LocalCommands.resolve("status report", macros)
        assertTrue(command is LocalCommand.RunMacro)
        assertEquals("Give me a full status report.", (command as LocalCommand.RunMacro).macro.prompt)
    }

    @Test
    fun `a near miss on a macro phrase still fires`() {
        assertTrue(LocalCommands.resolve("status reports", macros) is LocalCommand.RunMacro)
    }

    @Test
    fun `a disabled macro is ignored`() {
        assertTrue(LocalCommands.resolve("deploy check", macros) is LocalCommand.Send)
    }

    @Test
    fun `a long utterance mentioning a macro phrase is sent verbatim`() {
        val spoken = "write me a status report about the migration and include the timeline"
        val command = LocalCommands.resolve(spoken, macros)
        assertTrue(command is LocalCommand.Send)
        assertEquals(spoken, (command as LocalCommand.Send).text)
    }

    @Test
    fun `ordinary speech is sent to the agent`() {
        val command = LocalCommands.resolve("what is the current build status", macros)
        assertTrue(command is LocalCommand.Send)
    }
}
