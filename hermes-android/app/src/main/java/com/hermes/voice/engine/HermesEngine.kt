package com.hermes.voice.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.hermes.voice.data.ChatMessage
import com.hermes.voice.data.Conversation
import com.hermes.voice.data.ConversationStore
import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.Macro
import com.hermes.voice.data.Role
import com.hermes.voice.data.SettingsRepository
import com.hermes.voice.data.ToolCallInfo
import com.hermes.voice.data.VoiceMode
import com.hermes.voice.net.AgentEvent
import com.hermes.voice.net.AgentRequest
import com.hermes.voice.net.PingResult
import com.hermes.voice.net.TransportFactory
import com.hermes.voice.voice.SttEngine
import com.hermes.voice.voice.TtsEngine
import com.hermes.voice.voice.WakePhrase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * The single source of truth for the app.
 *
 * The UI and the foreground voice service both talk to this one instance rather
 * than to each other, so a hands-free session keeps running while the Activity
 * is gone and the screen shows the right state the moment it comes back.
 */
class HermesEngine(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val store: ConversationStore,
    private val transports: TransportFactory,
    private val stt: SttEngine,
    private val tts: TtsEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    /** Raised when a voice session needs the foreground service started/stopped. */
    var onVoiceSessionChanged: ((active: Boolean) -> Unit)? = null

    private var turnJob: Job? = null
    private var voiceJob: Job? = null
    private var bootstrapped = false

    // --- lifecycle ---------------------------------------------------------

    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true

        settingsRepository.settings
            .onEach { settings ->
                _state.value = _state.value.copy(settings = settings)
                tts.applyVoiceSettings(settings.speechRate, settings.speechPitch, settings.ttsVoice)
            }
            .launchIn(scope)

        settingsRepository.macros
            .onEach { macros -> _state.value = _state.value.copy(macros = macros) }
            .launchIn(scope)

        store.conversations
            .onEach { list ->
                val active = _state.value.conversation?.id?.let { id -> list.firstOrNull { it.id == id } }
                _state.value = _state.value.copy(conversations = list, conversation = active ?: _state.value.conversation)
            }
            .launchIn(scope)

        tts.speaking
            .onEach { speaking ->
                val current = _state.value
                val nextVoiceState = when {
                    speaking -> VoiceState.SPEAKING
                    current.voiceState == VoiceState.SPEAKING -> VoiceState.IDLE
                    else -> current.voiceState
                }
                _state.value = current.copy(speaking = speaking, voiceState = nextVoiceState)
            }
            .launchIn(scope)

        scope.launch {
            store.load()
            val savedId = settingsRepository.activeConversationId.first()
            val restored = store.get(savedId) ?: store.conversations.value.firstOrNull()
            _state.value = _state.value.copy(
                conversation = restored ?: newConversationObject(),
                conversations = store.conversations.value,
            )
            if (tts.init()) {
                val settings = settingsRepository.settings.first()
                tts.applyVoiceSettings(settings.speechRate, settings.speechPitch, settings.ttsVoice)
                _state.value = _state.value.copy(ttsVoices = tts.availableVoices().map { it.name })
            }
            if (_state.value.settings.isConfigured) checkConnection()
        }
    }

    fun shutdown() {
        voiceJob?.cancel()
        turnJob?.cancel()
        tts.shutdown()
        transports.closeSocket()
    }

    // --- conversations -----------------------------------------------------

    private fun newConversationObject(): Conversation {
        val now = System.currentTimeMillis()
        return Conversation(
            id = UUID.randomUUID().toString(),
            title = ConversationStore.DEFAULT_TITLE,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun newConversation() {
        cancelTurn(silent = true)
        val conversation = newConversationObject()
        _state.value = _state.value.copy(
            conversation = conversation,
            streamingText = "",
            agentStatus = null,
            notice = null,
        )
        scope.launch { settingsRepository.setActiveConversation(conversation.id) }
    }

    fun selectConversation(id: String) {
        val target = store.get(id) ?: return
        cancelTurn(silent = true)
        _state.value = _state.value.copy(conversation = target, streamingText = "", agentStatus = null)
        scope.launch { settingsRepository.setActiveConversation(id) }
    }

    fun deleteConversation(id: String) {
        scope.launch {
            store.delete(id)
            if (_state.value.conversation?.id == id) {
                val next = store.conversations.value.firstOrNull() ?: newConversationObject()
                _state.value = _state.value.copy(conversation = next, streamingText = "")
                settingsRepository.setActiveConversation(next.id)
            }
        }
    }

    fun renameConversation(id: String, title: String) {
        scope.launch {
            store.rename(id, title)
            if (_state.value.conversation?.id == id) {
                _state.value = _state.value.copy(conversation = _state.value.conversation?.copy(title = title))
            }
        }
    }

    // --- sending -----------------------------------------------------------

    fun send(text: String, viaVoice: Boolean = false) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return
        if (!_state.value.settings.isConfigured) {
            _state.value = _state.value.copy(notice = "Set the Hermes base URL in Settings first.")
            return
        }
        turnJob?.cancel()
        turnJob = scope.launch { runTurn(prompt, viaVoice) }
    }

    /** Streams one turn to completion. Safe to await from the voice loop. */
    private suspend fun runTurn(prompt: String, viaVoice: Boolean) {
        val settings = _state.value.settings
        val conversation = _state.value.conversation ?: newConversationObject()
        val now = System.currentTimeMillis()

        val userMessage = ChatMessage(role = Role.USER, text = prompt, timestamp = now, viaVoice = viaVoice)
        val withUser = conversation.copy(
            messages = conversation.messages + userMessage,
            updatedAt = now,
        )
        _state.value = _state.value.copy(
            conversation = withUser,
            streamingText = "",
            streaming = true,
            agentStatus = null,
            notice = null,
            partialTranscript = "",
            voiceState = VoiceState.THINKING,
        )
        store.upsert(withUser)
        settingsRepository.setActiveConversation(withUser.id)

        val speakReply = settings.autoSpeakReplies && (viaVoice || _state.value.voiceMode != VoiceMode.OFF)
        if (speakReply) {
            tts.stop()
            tts.resetFeed()
        }

        val request = AgentRequest(
            conversationId = withUser.id,
            prompt = prompt,
            history = conversation.messages,
            settings = settings,
        )

        val builder = StringBuilder()
        val tools = mutableListOf<ToolCallInfo>()
        var failure: String? = null

        try {
            transports.forKind(settings.transport).stream(request).collect { event ->
                when (event) {
                    is AgentEvent.Delta -> {
                        builder.append(event.text)
                        _state.value = _state.value.copy(streamingText = builder.toString(), agentStatus = null)
                        if (speakReply) tts.feed(event.text)
                    }

                    is AgentEvent.Status ->
                        _state.value = _state.value.copy(agentStatus = event.text)

                    is AgentEvent.Tool -> {
                        tools += ToolCallInfo(event.name, event.detail)
                        _state.value = _state.value.copy(agentStatus = "Tool: ${event.name}")
                    }

                    is AgentEvent.Failed -> failure = event.message

                    AgentEvent.Completed -> Unit
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) {
                // The scope is already cancelled, so persisting the partial
                // answer has to happen outside cancellation.
                withContext(NonCancellable) {
                    finalizeTurn(builder.toString(), tools, null, cancelled = true, speakReply = speakReply)
                }
                throw t
            }
            failure = t.message ?: t::class.java.simpleName
        }

        finalizeTurn(builder.toString(), tools, failure, cancelled = false, speakReply = speakReply)
    }

    private suspend fun finalizeTurn(
        text: String,
        tools: List<ToolCallInfo>,
        failure: String?,
        cancelled: Boolean,
        speakReply: Boolean,
    ) {
        val conversation = _state.value.conversation ?: return
        val trimmed = text.trim()

        if (speakReply && !cancelled && failure == null) {
            tts.endFeed()
        } else if (cancelled) {
            tts.stop()
        }

        val messages = buildList {
            addAll(conversation.messages)
            when {
                failure != null && trimmed.isEmpty() ->
                    add(ChatMessage(role = Role.ASSISTANT, text = failure, timestamp = System.currentTimeMillis(), isError = true))

                failure != null ->
                    add(
                        ChatMessage(
                            role = Role.ASSISTANT,
                            text = trimmed + "\n\n_(interrupted: $failure)_",
                            timestamp = System.currentTimeMillis(),
                            toolCalls = tools,
                        ),
                    )

                trimmed.isNotEmpty() ->
                    add(
                        ChatMessage(
                            role = Role.ASSISTANT,
                            text = if (cancelled) "$trimmed\n\n_(stopped)_" else trimmed,
                            timestamp = System.currentTimeMillis(),
                            toolCalls = tools,
                        ),
                    )
            }
        }

        val updated = conversation.copy(messages = messages, updatedAt = System.currentTimeMillis())
        _state.value = _state.value.copy(
            conversation = updated,
            streamingText = "",
            streaming = false,
            agentStatus = null,
            voiceState = if (_state.value.speaking) VoiceState.SPEAKING else VoiceState.IDLE,
            notice = failure,
        )
        store.upsert(updated)
    }

    fun cancelTurn(silent: Boolean = false) {
        val settings = _state.value.settings
        _state.value.conversation?.let { transports.cancelRemote(settings.transport, it.id) }
        turnJob?.cancel()
        turnJob = null
        tts.stop()
        _state.value = _state.value.copy(
            streaming = false,
            agentStatus = null,
            voiceState = VoiceState.IDLE,
            notice = if (silent) _state.value.notice else "Stopped.",
        )
    }

    fun stopSpeaking() {
        tts.stop()
        _state.value = _state.value.copy(voiceState = VoiceState.IDLE)
    }

    fun repeatLast() {
        val last = _state.value.conversation?.messages?.lastOrNull { it.role == Role.ASSISTANT }?.text
        if (last.isNullOrBlank()) {
            _state.value = _state.value.copy(notice = "Nothing to repeat yet.")
            return
        }
        tts.speakNow(last)
    }

    // --- voice -------------------------------------------------------------

    fun setVoiceMode(mode: VoiceMode) {
        if (mode != VoiceMode.OFF && !hasMicPermission()) {
            _state.value = _state.value.copy(
                notice = "Microphone permission is required for voice.",
                voiceMode = VoiceMode.OFF,
            )
            return
        }

        voiceJob?.cancel()
        voiceJob = null
        _state.value = _state.value.copy(
            voiceMode = mode,
            voiceState = VoiceState.IDLE,
            partialTranscript = "",
            micLevel = 0f,
        )
        onVoiceSessionChanged?.invoke(mode != VoiceMode.OFF)

        when (mode) {
            VoiceMode.OFF -> {
                tts.stop()
            }

            VoiceMode.PUSH_TO_TALK -> Unit // waits for a tap

            VoiceMode.HANDS_FREE -> voiceJob = scope.launch { handsFreeLoop() }

            VoiceMode.WAKE_WORD -> voiceJob = scope.launch { wakeWordLoop() }
        }
    }

    /** Orb tap: start one utterance, or stop the one in progress. */
    fun toggleListening() {
        // Talking over Hermes always silences it first.
        if (_state.value.speaking) tts.stop()

        if (!hasMicPermission()) {
            _state.value = _state.value.copy(notice = "Microphone permission is required for voice.")
            return
        }

        val mode = _state.value.voiceMode

        if (_state.value.voiceState == VoiceState.LISTENING) {
            // Stopping mid-listen drops out of any continuous mode, so the mode
            // shown on screen keeps matching what the microphone is doing.
            setVoiceMode(VoiceMode.PUSH_TO_TALK)
            return
        }

        // In a continuous mode a tap means "listen right now" — restart the loop.
        if (mode == VoiceMode.HANDS_FREE || mode == VoiceMode.WAKE_WORD) {
            setVoiceMode(mode)
            return
        }

        if (mode == VoiceMode.OFF) {
            _state.value = _state.value.copy(voiceMode = VoiceMode.PUSH_TO_TALK)
            onVoiceSessionChanged?.invoke(true)
        }
        voiceJob?.cancel()
        voiceJob = scope.launch {
            val heard = listenOnce()
            if (!heard.isNullOrBlank()) handleTranscript(heard)
        }
    }

    private suspend fun handsFreeLoop() {
        var consecutiveFailures = 0
        while (currentCoroutineContext().isActive && _state.value.voiceMode == VoiceMode.HANDS_FREE) {
            awaitSilence()
            if (_state.value.voiceMode != VoiceMode.HANDS_FREE) break

            val heard = listenOnce()
            if (heard == null) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_LISTEN_FAILURES) {
                    _state.value = _state.value.copy(
                        notice = "Stopped hands-free listening after repeated microphone errors.",
                    )
                    setVoiceMode(VoiceMode.OFF)
                    return
                }
                delay(400)
                continue
            }
            consecutiveFailures = 0
            if (heard.isBlank()) {
                delay(250)
                continue
            }

            val keepGoing = handleTranscript(heard)
            if (!keepGoing || !_state.value.settings.handsFreeContinues) {
                if (_state.value.voiceMode == VoiceMode.HANDS_FREE) setVoiceMode(VoiceMode.OFF)
                return
            }
        }
    }

    private suspend fun wakeWordLoop() {
        var consecutiveFailures = 0
        while (currentCoroutineContext().isActive && _state.value.voiceMode == VoiceMode.WAKE_WORD) {
            awaitSilence()
            if (_state.value.voiceMode != VoiceMode.WAKE_WORD) break

            val phrase = _state.value.settings.wakePhrase
            val heard = listenOnce(forWake = true)
            if (heard == null) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_LISTEN_FAILURES) {
                    _state.value = _state.value.copy(
                        notice = "Stopped wake-word listening after repeated microphone errors.",
                    )
                    setVoiceMode(VoiceMode.OFF)
                    return
                }
                delay(600)
                continue
            }
            consecutiveFailures = 0
            if (!WakePhrase.matches(heard, phrase)) {
                delay(150)
                continue
            }

            val remainder = WakePhrase.remainderAfterWake(heard, phrase)
            if (remainder.isNotBlank()) {
                handleTranscript(remainder)
            } else {
                // Woken with nothing after it: acknowledge, then take the command.
                _state.value = _state.value.copy(notice = null)
                val command = listenOnce()
                if (!command.isNullOrBlank()) handleTranscript(command)
            }
        }
    }

    /**
     * Runs one recognition pass. Returns the transcript (possibly blank when
     * nothing was said), or null on a microphone/service error.
     */
    private suspend fun listenOnce(forWake: Boolean = false): String? {
        val settings = _state.value.settings
        _state.value = _state.value.copy(
            voiceState = VoiceState.LISTENING,
            partialTranscript = "",
            micLevel = 0f,
        )

        var finalText: String? = null
        var failed = false
        var recoverable = true

        try {
            stt.listen(
                language = settings.sttLanguage,
                silenceTimeoutMs = if (forWake) 900 else settings.silenceTimeoutMs,
            ).collect { event ->
                when (event) {
                    is SttEngine.Event.Partial ->
                        if (!forWake) _state.value = _state.value.copy(partialTranscript = event.text)

                    is SttEngine.Event.Final -> finalText = event.text

                    is SttEngine.Event.Level -> {
                        // rms is roughly -2..10 dB; normalize for the orb.
                        val level = ((event.rms + 2f) / 12f).coerceIn(0f, 1f)
                        _state.value = _state.value.copy(micLevel = level)
                    }

                    is SttEngine.Event.Failed -> {
                        failed = true
                        recoverable = event.recoverable
                        if (!event.recoverable) {
                            _state.value = _state.value.copy(notice = event.message)
                        }
                    }

                    SttEngine.Event.Ready, SttEngine.Event.BeginSpeech, SttEngine.Event.EndSpeech -> Unit
                }
            }
        } finally {
            _state.value = _state.value.copy(micLevel = 0f)
            if (_state.value.voiceState == VoiceState.LISTENING) {
                _state.value = _state.value.copy(voiceState = VoiceState.IDLE)
            }
        }

        return when {
            failed && !recoverable -> null
            failed -> ""
            else -> finalText.orEmpty()
        }
    }

    /**
     * Acts on a finished utterance. Returns false when the voice loop should
     * stop (the user asked it to).
     */
    private suspend fun handleTranscript(transcript: String): Boolean {
        _state.value = _state.value.copy(partialTranscript = "")
        return when (val command = LocalCommands.resolve(transcript, _state.value.macros)) {
            LocalCommand.Cancel -> {
                cancelTurn()
                true
            }

            LocalCommand.NewConversation -> {
                newConversation()
                true
            }

            LocalCommand.RepeatLast -> {
                repeatLast()
                awaitSilence()
                true
            }

            LocalCommand.StopVoice -> {
                setVoiceMode(VoiceMode.OFF)
                false
            }

            is LocalCommand.RunMacro -> {
                runTurnAndWait(command.macro.prompt)
                true
            }

            is LocalCommand.Send -> {
                runTurnAndWait(command.text)
                true
            }
        }
    }

    private suspend fun runTurnAndWait(prompt: String) {
        turnJob?.cancel()
        val job = scope.launch { runTurn(prompt, viaVoice = true) }
        turnJob = job
        job.join()
        awaitSilence()
    }

    /** Waits until TTS has finished, so the mic never records Hermes itself. */
    private suspend fun awaitSilence() {
        if (!_state.value.speaking) return
        withTimeoutOrNull(MAX_SPEECH_WAIT_MS) {
            while (currentCoroutineContext().isActive && _state.value.speaking) delay(120)
        }
        // Small tail so the speaker has fully quiesced before the mic opens.
        delay(220)
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    // --- settings & diagnostics -------------------------------------------

    fun updateSettings(transform: (HermesSettings) -> HermesSettings) {
        scope.launch { settingsRepository.update(transform) }
    }

    fun saveMacros(macros: List<Macro>) {
        scope.launch { settingsRepository.saveMacros(macros) }
    }

    fun dismissNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    fun checkConnection(onResult: ((PingResult) -> Unit)? = null) {
        val settings = _state.value.settings
        if (!settings.isConfigured) {
            _state.value = _state.value.copy(
                connection = ConnectionState(ConnectionHealth.FAILED, "No base URL set"),
            )
            return
        }
        scope.launch {
            _state.value = _state.value.copy(
                connection = _state.value.connection.copy(health = ConnectionHealth.CHECKING, message = "Checking…"),
            )
            val result = transports.forKind(settings.transport).ping(settings)
            _state.value = _state.value.copy(
                connection = ConnectionState(
                    health = if (result.ok) ConnectionHealth.OK else ConnectionHealth.FAILED,
                    message = result.message,
                    latencyMs = result.latencyMs,
                    models = result.models,
                ),
            )
            onResult?.invoke(result)
        }
    }

    fun previewVoice() {
        val settings = _state.value.settings
        tts.applyVoiceSettings(settings.speechRate, settings.speechPitch, settings.ttsVoice)
        tts.speakNow("Hermes here. Ready when you are.")
    }

    private companion object {
        const val MAX_LISTEN_FAILURES = 4
        const val MAX_SPEECH_WAIT_MS = 120_000L
    }
}
