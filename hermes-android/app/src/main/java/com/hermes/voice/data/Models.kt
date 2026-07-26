package com.hermes.voice.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Role { USER, ASSISTANT, SYSTEM }

@Serializable
data class ToolCallInfo(
    val name: String,
    val detail: String = "",
)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long,
    val isError: Boolean = false,
    val viaVoice: Boolean = false,
    val toolCalls: List<ToolCallInfo> = emptyList(),
)

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New conversation",
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<ChatMessage> = emptyList(),
) {
    /** First user turn, trimmed — used to auto-title a conversation. */
    fun derivedTitle(): String {
        val first = messages.firstOrNull { it.role == Role.USER }?.text?.trim().orEmpty()
        if (first.isBlank()) return title
        return if (first.length <= 42) first else first.take(41).trimEnd() + "…"
    }
}

/**
 * A spoken shortcut: saying [phrase] sends [prompt] to the agent instead of the
 * raw transcript. Lets long, repeated instructions be triggered by a few words.
 */
@Serializable
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val phrase: String,
    val prompt: String,
    val enabled: Boolean = true,
)
