package com.hermes.voice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_settings")

/**
 * Settings live in app-private DataStore. The API key never leaves this process
 * except as an Authorization header on requests to the configured base URL.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val apiKey = stringPreferencesKey("api_key")
        val transport = stringPreferencesKey("transport")
        val chatPath = stringPreferencesKey("chat_path")
        val extraHeaders = stringPreferencesKey("extra_headers")
        val requestTimeoutSec = intPreferencesKey("request_timeout_sec")

        val model = stringPreferencesKey("model")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val temperature = floatPreferencesKey("temperature")
        val maxTokens = intPreferencesKey("max_tokens")
        val sendHistory = booleanPreferencesKey("send_history")
        val historyDepth = intPreferencesKey("history_depth")

        val autoSpeakReplies = booleanPreferencesKey("auto_speak")
        val handsFreeContinues = booleanPreferencesKey("hands_free_continues")
        val wakePhrase = stringPreferencesKey("wake_phrase")
        val speechRate = floatPreferencesKey("speech_rate")
        val speechPitch = floatPreferencesKey("speech_pitch")
        val ttsVoice = stringPreferencesKey("tts_voice")
        val sttLanguage = stringPreferencesKey("stt_language")
        val silenceTimeoutMs = intPreferencesKey("silence_timeout_ms")
        val listenOnAssist = booleanPreferencesKey("listen_on_assist")
        val defaultVoiceMode = stringPreferencesKey("default_voice_mode")

        val macros = stringPreferencesKey("macros_json")
        val activeConversation = stringPreferencesKey("active_conversation")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val defaults = HermesSettings()

    val settings: Flow<HermesSettings> = context.dataStore.data.map { p ->
        HermesSettings(
            baseUrl = p[Keys.baseUrl] ?: defaults.baseUrl,
            apiKey = p[Keys.apiKey] ?: defaults.apiKey,
            transport = p[Keys.transport]?.let { name ->
                runCatching { TransportKind.valueOf(name) }.getOrNull()
            } ?: defaults.transport,
            chatPath = p[Keys.chatPath] ?: defaults.chatPath,
            extraHeaders = p[Keys.extraHeaders] ?: defaults.extraHeaders,
            requestTimeoutSec = p[Keys.requestTimeoutSec] ?: defaults.requestTimeoutSec,
            model = p[Keys.model] ?: defaults.model,
            systemPrompt = p[Keys.systemPrompt] ?: defaults.systemPrompt,
            temperature = p[Keys.temperature] ?: defaults.temperature,
            maxTokens = p[Keys.maxTokens] ?: defaults.maxTokens,
            sendHistory = p[Keys.sendHistory] ?: defaults.sendHistory,
            historyDepth = p[Keys.historyDepth] ?: defaults.historyDepth,
            autoSpeakReplies = p[Keys.autoSpeakReplies] ?: defaults.autoSpeakReplies,
            handsFreeContinues = p[Keys.handsFreeContinues] ?: defaults.handsFreeContinues,
            wakePhrase = p[Keys.wakePhrase] ?: defaults.wakePhrase,
            speechRate = p[Keys.speechRate] ?: defaults.speechRate,
            speechPitch = p[Keys.speechPitch] ?: defaults.speechPitch,
            ttsVoice = p[Keys.ttsVoice] ?: defaults.ttsVoice,
            sttLanguage = p[Keys.sttLanguage] ?: defaults.sttLanguage,
            silenceTimeoutMs = p[Keys.silenceTimeoutMs] ?: defaults.silenceTimeoutMs,
            listenOnAssistLaunch = p[Keys.listenOnAssist] ?: defaults.listenOnAssistLaunch,
            defaultVoiceMode = p[Keys.defaultVoiceMode]?.let { name ->
                runCatching { VoiceMode.valueOf(name) }.getOrNull()
            } ?: defaults.defaultVoiceMode,
        )
    }

    val macros: Flow<List<Macro>> = context.dataStore.data.map { p ->
        val raw = p[Keys.macros] ?: return@map defaultMacros
        runCatching { json.decodeFromString<List<Macro>>(raw) }.getOrDefault(defaultMacros)
    }

    val activeConversationId: Flow<String?> = context.dataStore.data.map { it[Keys.activeConversation] }

    suspend fun update(transform: (HermesSettings) -> HermesSettings) {
        context.dataStore.edit { p ->
            val current = HermesSettings(
                baseUrl = p[Keys.baseUrl] ?: defaults.baseUrl,
                apiKey = p[Keys.apiKey] ?: defaults.apiKey,
                transport = p[Keys.transport]?.let { n -> runCatching { TransportKind.valueOf(n) }.getOrNull() }
                    ?: defaults.transport,
                chatPath = p[Keys.chatPath] ?: defaults.chatPath,
                extraHeaders = p[Keys.extraHeaders] ?: defaults.extraHeaders,
                requestTimeoutSec = p[Keys.requestTimeoutSec] ?: defaults.requestTimeoutSec,
                model = p[Keys.model] ?: defaults.model,
                systemPrompt = p[Keys.systemPrompt] ?: defaults.systemPrompt,
                temperature = p[Keys.temperature] ?: defaults.temperature,
                maxTokens = p[Keys.maxTokens] ?: defaults.maxTokens,
                sendHistory = p[Keys.sendHistory] ?: defaults.sendHistory,
                historyDepth = p[Keys.historyDepth] ?: defaults.historyDepth,
                autoSpeakReplies = p[Keys.autoSpeakReplies] ?: defaults.autoSpeakReplies,
                handsFreeContinues = p[Keys.handsFreeContinues] ?: defaults.handsFreeContinues,
                wakePhrase = p[Keys.wakePhrase] ?: defaults.wakePhrase,
                speechRate = p[Keys.speechRate] ?: defaults.speechRate,
                speechPitch = p[Keys.speechPitch] ?: defaults.speechPitch,
                ttsVoice = p[Keys.ttsVoice] ?: defaults.ttsVoice,
                sttLanguage = p[Keys.sttLanguage] ?: defaults.sttLanguage,
                silenceTimeoutMs = p[Keys.silenceTimeoutMs] ?: defaults.silenceTimeoutMs,
                listenOnAssistLaunch = p[Keys.listenOnAssist] ?: defaults.listenOnAssistLaunch,
                defaultVoiceMode = p[Keys.defaultVoiceMode]?.let { n -> runCatching { VoiceMode.valueOf(n) }.getOrNull() }
                    ?: defaults.defaultVoiceMode,
            )
            val next = transform(current)
            p[Keys.baseUrl] = next.baseUrl
            p[Keys.apiKey] = next.apiKey
            p[Keys.transport] = next.transport.name
            p[Keys.chatPath] = next.chatPath
            p[Keys.extraHeaders] = next.extraHeaders
            p[Keys.requestTimeoutSec] = next.requestTimeoutSec
            p[Keys.model] = next.model
            p[Keys.systemPrompt] = next.systemPrompt
            p[Keys.temperature] = next.temperature
            p[Keys.maxTokens] = next.maxTokens
            p[Keys.sendHistory] = next.sendHistory
            p[Keys.historyDepth] = next.historyDepth
            p[Keys.autoSpeakReplies] = next.autoSpeakReplies
            p[Keys.handsFreeContinues] = next.handsFreeContinues
            p[Keys.wakePhrase] = next.wakePhrase
            p[Keys.speechRate] = next.speechRate
            p[Keys.speechPitch] = next.speechPitch
            p[Keys.ttsVoice] = next.ttsVoice
            p[Keys.sttLanguage] = next.sttLanguage
            p[Keys.silenceTimeoutMs] = next.silenceTimeoutMs
            p[Keys.listenOnAssist] = next.listenOnAssistLaunch
            p[Keys.defaultVoiceMode] = next.defaultVoiceMode.name
        }
    }

    suspend fun saveMacros(macros: List<Macro>) {
        context.dataStore.edit { it[Keys.macros] = json.encodeToString(macros) }
    }

    suspend fun setActiveConversation(id: String) {
        context.dataStore.edit { it[Keys.activeConversation] = id }
    }

    companion object {
        val defaultMacros = listOf(
            Macro(phrase = "status report", prompt = "Give me a concise status report of everything you are currently working on."),
            Macro(phrase = "what's running", prompt = "List every task or job you currently have running, with its state."),
            Macro(phrase = "summarize that", prompt = "Summarize your previous answer in three sentences or fewer."),
        )
    }
}
