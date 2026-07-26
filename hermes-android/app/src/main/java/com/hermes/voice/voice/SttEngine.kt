package com.hermes.voice.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Wraps the platform [SpeechRecognizer] as a cold [Flow].
 *
 * Everything runs on the main thread because SpeechRecognizer requires it; the
 * flow tears the recognizer down when collection stops, which is what makes
 * push-to-talk cancel cleanly.
 */
class SttEngine(private val context: Context) {

    sealed interface Event {
        data object Ready : Event
        data object BeginSpeech : Event
        data object EndSpeech : Event
        data class Partial(val text: String) : Event
        data class Final(val text: String) : Event
        data class Level(val rms: Float) : Event
        data class Failed(val code: Int, val message: String, val recoverable: Boolean) : Event
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(language: String, silenceTimeoutMs: Int): Flow<Event> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(
                Event.Failed(
                    code = -1,
                    message = "No speech recognition service on this device. Install Google (or another " +
                        "voice service) and enable it in Settings → System → Languages & input.",
                    recoverable = false,
                ),
            )
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        var delivered = false

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(Event.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(Event.BeginSpeech)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(Event.Level(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                trySend(Event.EndSpeech)
            }

            override fun onError(error: Int) {
                // NO_MATCH / SPEECH_TIMEOUT are normal in hands-free listening:
                // the user simply did not say anything this round.
                val recoverable = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (!delivered) {
                    trySend(Event.Failed(error, describeError(error), recoverable))
                }
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                delivered = true
                trySend(Event.Final(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) trySend(Event.Partial(text))
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (language.isNotBlank()) {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            }
            // Hints only — most recognizers clamp these, but where they are
            // honoured they decide how long a pause ends the utterance.
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                silenceTimeoutMs.toLong(),
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                silenceTimeoutMs.toLong(),
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 900L)
        }

        runCatching { recognizer.startListening(intent) }
            .onFailure {
                trySend(Event.Failed(-2, it.message ?: "Could not start listening", recoverable = true))
                close()
            }

        awaitClose {
            runCatching { recognizer.stopListening() }
            runCatching { recognizer.cancel() }
            runCatching { recognizer.destroy() }
        }
    }.flowOn(Dispatchers.Main.immediate)

    private fun describeError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognizer client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Speech service network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service timed out"
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Speech service error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
        else -> "Speech error $code"
    }
}
