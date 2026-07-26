package com.hermes.voice.data

/** How the app talks to the Hermes agent. */
enum class TransportKind(val label: String, val description: String) {
    OPENAI_COMPAT(
        "OpenAI-compatible",
        "POST {base}/v1/chat/completions with SSE streaming. Works with llama.cpp, Ollama, vLLM, LiteLLM, LM Studio and most agent wrappers.",
    ),
    REST_JSON(
        "Generic JSON REST",
        "POST {base}{path} with a JSON body. Reads streamed SSE/NDJSON or a plain JSON reply, pulling text from whichever common field your server uses.",
    ),
    WEBSOCKET(
        "WebSocket",
        "Persistent ws:// or wss:// connection. Sends JSON frames and handles delta/tool/status/done events as they arrive.",
    ),
}

/** What the microphone is currently doing. */
enum class VoiceMode(val label: String) {
    OFF("Off"),
    PUSH_TO_TALK("Push to talk"),
    HANDS_FREE("Hands-free"),
    WAKE_WORD("Wake word"),
}

data class HermesSettings(
    // --- Connection ---
    val baseUrl: String = "",
    val apiKey: String = "",
    val transport: TransportKind = TransportKind.OPENAI_COMPAT,
    val chatPath: String = "/v1/chat/completions",
    val extraHeaders: String = "",
    val requestTimeoutSec: Int = 180,

    // --- Agent ---
    val model: String = "",
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 0,
    val sendHistory: Boolean = true,
    val historyDepth: Int = 12,

    // --- Voice ---
    val autoSpeakReplies: Boolean = true,
    val handsFreeContinues: Boolean = true,
    val wakePhrase: String = "hey hermes",
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val ttsVoice: String = "",
    val sttLanguage: String = "",
    val silenceTimeoutMs: Int = 1600,
    val listenOnAssistLaunch: Boolean = true,
    val defaultVoiceMode: VoiceMode = VoiceMode.PUSH_TO_TALK,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()

    /** Base URL without a trailing slash, so paths concatenate predictably. */
    fun normalizedBaseUrl(): String = baseUrl.trim().trimEnd('/')

    /** Parses the "Name: value" header block into pairs, ignoring blank/short lines. */
    fun parsedHeaders(): List<Pair<String, String>> =
        extraHeaders.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val name = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (name.isEmpty() || value.isEmpty()) null else name to value
            }
            .toList()

    /** The endpoint the current transport posts to. */
    fun endpoint(): String = when (transport) {
        TransportKind.OPENAI_COMPAT -> normalizedBaseUrl() + "/v1/chat/completions"
        TransportKind.REST_JSON -> normalizedBaseUrl() + chatPath.ensureLeadingSlash()
        TransportKind.WEBSOCKET -> normalizedBaseUrl().toWebSocketUrl() + chatPath.ensureLeadingSlash()
    }
}

fun String.ensureLeadingSlash(): String {
    val t = trim()
    if (t.isEmpty()) return ""
    return if (t.startsWith("/")) t else "/$t"
}

fun String.toWebSocketUrl(): String = when {
    startsWith("https://") -> "wss://" + removePrefix("https://")
    startsWith("http://") -> "ws://" + removePrefix("http://")
    else -> this
}
