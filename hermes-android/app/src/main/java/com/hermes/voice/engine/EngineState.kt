package com.hermes.voice.engine

import com.hermes.voice.data.Conversation
import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.Macro
import com.hermes.voice.data.VoiceMode

/** What the microphone/speaker pipeline is doing right now. */
enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }

enum class ConnectionHealth { UNKNOWN, CHECKING, OK, FAILED }

data class ConnectionState(
    val health: ConnectionHealth = ConnectionHealth.UNKNOWN,
    val message: String = "Not checked",
    val latencyMs: Long = 0,
    val models: List<String> = emptyList(),
)

data class EngineState(
    val settings: HermesSettings = HermesSettings(),
    val macros: List<Macro> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val conversation: Conversation? = null,
    /** Assistant text for the in-flight turn, appended as deltas arrive. */
    val streamingText: String = "",
    val streaming: Boolean = false,
    /** Transient agent progress line, e.g. "running tool: search". */
    val agentStatus: String? = null,
    val voiceState: VoiceState = VoiceState.IDLE,
    val voiceMode: VoiceMode = VoiceMode.OFF,
    val partialTranscript: String = "",
    val micLevel: Float = 0f,
    val speaking: Boolean = false,
    val connection: ConnectionState = ConnectionState(),
    val notice: String? = null,
    val ttsVoices: List<String> = emptyList(),
) {
    val canSend: Boolean get() = settings.isConfigured && !streaming
}
