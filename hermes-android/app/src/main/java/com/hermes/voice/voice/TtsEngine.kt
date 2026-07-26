package com.hermes.voice.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Text-to-speech with streaming support.
 *
 * Deltas are pushed in with [feed] as they arrive; complete sentences are
 * handed to the engine immediately, so Hermes starts talking while it is still
 * writing. [stop] is the barge-in path — it silences playback instantly.
 */
class TtsEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private val utteranceCounter = AtomicInteger(0)
    private val pending = AtomicInteger(0)
    private val buffer = StringBuilder()

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** Text of the last full utterance, for the "say that again" command. */
    @Volatile var lastSpoken: String = ""
        private set

    /**
     * Must be called from the main thread: [TextToSpeech]'s init callback is
     * posted to the main looper, so binding the field before the callback can
     * run is only guaranteed there.
     */
    suspend fun init(): Boolean = withContext(Dispatchers.Main) {
        if (_ready.value) return@withContext true
        suspendCancellableCoroutine { cont ->
            val engine = TextToSpeech(context) { status ->
                val ok = status == TextToSpeech.SUCCESS
                if (ok) {
                    tts?.setOnUtteranceProgressListener(progressListener)
                    _ready.value = true
                }
                if (cont.isActive) cont.resume(ok)
            }
            tts = engine
            cont.invokeOnCancellation { runCatching { engine.shutdown() } }
        }
    }

    private fun utteranceFinished() {
        if (pending.decrementAndGet() <= 0) {
            pending.set(0)
            _speaking.value = false
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _speaking.value = true
        }

        override fun onDone(utteranceId: String?) = utteranceFinished()

        @Deprecated("Abstract in the framework base class; superseded by onError(String, Int).")
        override fun onError(utteranceId: String?) = utteranceFinished()

        override fun onError(utteranceId: String?, errorCode: Int) = utteranceFinished()

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            pending.set(0)
            _speaking.value = false
        }
    }

    fun applyVoiceSettings(rate: Float, pitch: Float, voiceName: String) {
        val engine = tts ?: return
        engine.setSpeechRate(rate.coerceIn(0.5f, 2.5f))
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        if (voiceName.isNotBlank()) {
            engine.voices
                ?.firstOrNull { it.name == voiceName }
                ?.let { engine.voice = it }
        }
    }

    fun availableVoices(): List<Voice> = tts?.voices?.sortedBy { it.name }.orEmpty()

    /** Speaks [text] immediately, replacing anything currently queued. */
    fun speakNow(text: String) {
        val clean = SpeechText.forSpeech(text)
        if (clean.isBlank()) return
        stop()
        enqueue(clean, flush = true)
        lastSpoken = clean
    }

    /** Buffers a streaming delta, speaking each sentence as it completes. */
    fun feed(delta: String) {
        buffer.append(delta)
        while (true) {
            val chunk = SpeechText.takeSpeakableChunk(buffer.toString(), force = false) ?: break
            val (speakable, remainder) = chunk
            buffer.setLength(0)
            buffer.append(remainder)
            enqueue(speakable, flush = false)
            lastSpoken = (lastSpoken + " " + speakable).trim().takeLast(4000)
        }
    }

    /** Flushes whatever is left in the buffer at the end of a turn. */
    fun endFeed() {
        val chunk = SpeechText.takeSpeakableChunk(buffer.toString(), force = true)
        buffer.setLength(0)
        if (chunk != null) {
            enqueue(chunk.first, flush = false)
            lastSpoken = (lastSpoken + " " + chunk.first).trim().takeLast(4000)
        }
    }

    fun resetFeed() {
        buffer.setLength(0)
        lastSpoken = ""
    }

    private fun enqueue(text: String, flush: Boolean) {
        val engine = tts ?: return
        val id = "hermes-${utteranceCounter.incrementAndGet()}"
        pending.incrementAndGet()
        _speaking.value = true
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = engine.speak(text, mode, Bundle(), id)
        if (result != TextToSpeech.SUCCESS) {
            if (pending.decrementAndGet() <= 0) {
                pending.set(0)
                _speaking.value = false
            }
        }
    }

    fun stop() {
        buffer.setLength(0)
        pending.set(0)
        _speaking.value = false
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        _ready.value = false
    }
}
