package com.hermes.voice.engine

import com.hermes.voice.data.Macro
import com.hermes.voice.voice.WakePhrase

/**
 * Spoken instructions the app handles itself, without a round trip to Hermes.
 *
 * Matching is deliberately strict — only an utterance that is *nothing but* the
 * command counts, so "stop the nightly deploy" still reaches the agent while a
 * bare "stop" halts it.
 */
sealed interface LocalCommand {
    data object Cancel : LocalCommand
    data object NewConversation : LocalCommand
    data object RepeatLast : LocalCommand
    data object StopVoice : LocalCommand
    data class RunMacro(val macro: Macro) : LocalCommand
    data class Send(val text: String) : LocalCommand
}

object LocalCommands {

    private val cancel = setOf(
        "stop", "cancel", "stop that", "cancel that", "never mind", "nevermind",
        "shut up", "be quiet", "quiet", "stop talking",
    )
    private val newConversation = setOf(
        "new conversation", "new chat", "start over", "start a new conversation",
        "clear the chat", "clear chat", "reset conversation", "reset the conversation",
    )
    private val repeat = setOf(
        "repeat", "repeat that", "say that again", "say it again", "what did you say",
        "read that again",
    )
    private val stopVoice = setOf(
        "stop listening", "go to sleep", "voice off", "turn off voice", "sleep now",
    )

    fun resolve(transcript: String, macros: List<Macro>): LocalCommand {
        val normalized = WakePhrase.normalize(transcript)
        if (normalized.isEmpty()) return LocalCommand.Send(transcript)

        if (normalized in cancel) return LocalCommand.Cancel
        if (normalized in newConversation) return LocalCommand.NewConversation
        if (normalized in repeat) return LocalCommand.RepeatLast
        if (normalized in stopVoice) return LocalCommand.StopVoice

        macros.firstOrNull { it.enabled && matchesMacro(normalized, it.phrase) }
            ?.let { return LocalCommand.RunMacro(it) }

        return LocalCommand.Send(transcript.trim())
    }

    /**
     * A macro fires when the utterance is essentially just its trigger phrase.
     * Requiring similar length stops a long request that happens to mention the
     * phrase from being replaced wholesale by the macro's prompt.
     */
    private fun matchesMacro(normalizedTranscript: String, phrase: String): Boolean {
        val target = WakePhrase.normalize(phrase)
        if (target.isEmpty()) return false
        if (normalizedTranscript == target) return true
        if (normalizedTranscript.length > target.length * 1.6 + 6) return false
        return WakePhrase.similarity(normalizedTranscript, target) >= 0.85
    }
}
