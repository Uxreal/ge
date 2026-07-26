package com.hermes.voice.net

import com.hermes.voice.data.ChatMessage
import com.hermes.voice.data.HermesSettings

/** One outbound turn to the agent. */
data class AgentRequest(
    val conversationId: String,
    val prompt: String,
    val history: List<ChatMessage>,
    val settings: HermesSettings,
)

/** Normalized stream events — every transport maps its wire format onto these. */
sealed interface AgentEvent {
    /** A chunk of assistant text. Always a delta, never cumulative. */
    data class Delta(val text: String) : AgentEvent

    /** Transient progress from the agent ("searching the web", "thinking"). */
    data class Status(val text: String) : AgentEvent

    /** The agent invoked a tool. */
    data class Tool(val name: String, val detail: String) : AgentEvent

    /** The turn finished cleanly. */
    data object Completed : AgentEvent

    /** The turn failed. [message] is safe to show to the user. */
    data class Failed(val message: String) : AgentEvent
}

data class PingResult(
    val ok: Boolean,
    val message: String,
    val latencyMs: Long,
    val models: List<String> = emptyList(),
)
