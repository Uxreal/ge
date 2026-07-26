package com.hermes.voice.net

import com.hermes.voice.data.HermesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * `POST {base}{path}` for agents that expose their own JSON endpoint.
 *
 * The request carries the prompt under several common field names at once
 * (`message`, `prompt`, `input`, `query`, `text`, plus a `messages` array), so
 * a server only has to recognise one of them; unknown extras are ignored by
 * every JSON parser. Responses are read as SSE, NDJSON, or a single JSON
 * document, with the text pulled out by [JsonShapes].
 */
class RestJsonTransport : HermesTransport {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override fun stream(request: AgentRequest): Flow<AgentEvent> = callbackFlow {
        val settings = request.settings
        val body = buildJsonObject {
            put("message", request.prompt)
            put("prompt", request.prompt)
            put("input", request.prompt)
            put("query", request.prompt)
            put("text", request.prompt)
            put("stream", true)
            put("conversation_id", request.conversationId)
            put("session_id", request.conversationId)
            if (settings.model.isNotBlank()) put("model", settings.model)
            put("temperature", settings.temperature)
            if (settings.maxTokens > 0) put("max_tokens", settings.maxTokens)
            if (settings.systemPrompt.isNotBlank()) put("system", settings.systemPrompt)
            val history = buildHistory(request)
            putJsonArray("messages") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", message.role.wireName())
                            put("content", message.text)
                        },
                    )
                }
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", request.prompt)
                    },
                )
            }
            putJsonArray("history") {
                history.forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", message.role.wireName())
                            put("content", message.text)
                        },
                    )
                }
            }
        }

        val httpRequest = Request.Builder()
            .url(settings.endpoint())
            .header("Accept", "text/event-stream, application/x-ndjson, application/json")
            .applyAuth(settings)
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        val call = Http.client(settings.requestTimeoutSec).newCall(httpRequest)

        val job = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val detail = response.body?.string()?.take(400).orEmpty()
                        trySend(AgentEvent.Failed(httpErrorMessage(response.code, detail)))
                        return@use
                    }

                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    val streaming = contentType.contains("event-stream") ||
                        contentType.contains("ndjson") ||
                        contentType.contains("text/plain") ||
                        contentType.isEmpty()

                    if (streaming) {
                        val emitter = DeltaEmitter { trySend(AgentEvent.Delta(it)) }
                        StreamReader.read(response) { payload ->
                            handlePayload(payload, emitter)
                        }
                    } else {
                        val raw = response.body?.string().orEmpty()
                        val element = runCatching { lenientJson.parseToJsonElement(raw) }.getOrNull()
                        val obj = element as? JsonObject

                        val error = obj?.let { JsonShapes.extractError(it) }
                        if (error != null) {
                            trySend(AgentEvent.Failed(error))
                            close()
                            return@use
                        }
                        obj?.let { JsonShapes.extractTool(it) }?.let { trySend(it) }

                        val text = if (element == null) {
                            raw.takeIf { it.isNotBlank() }
                        } else {
                            JsonShapes.extractText(element, allowMessageKey = true)
                        }
                        if (text.isNullOrBlank()) {
                            trySend(
                                AgentEvent.Failed(
                                    "Hermes replied but no text field was recognised. Body: ${raw.take(200)}",
                                ),
                            )
                            close()
                            return@use
                        }
                        trySend(AgentEvent.Delta(text))
                    }
                    trySend(AgentEvent.Completed)
                }
            } catch (t: Throwable) {
                if (!call.isCanceled()) trySend(AgentEvent.Failed(t.friendlyMessage()))
            }
            close()
        }

        awaitClose {
            call.cancel()
            job.cancel()
        }
    }

    /** Returns false when the stream should stop. */
    private fun ProducerScope<AgentEvent>.handlePayload(
        payload: String,
        emitter: DeltaEmitter,
    ): Boolean {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed == "[DONE]") return false

        val element = runCatching { lenientJson.parseToJsonElement(trimmed) }.getOrNull()
        if (element == null) {
            // Plain-text stream: the line is the content.
            emitter.emit(payload)
            return true
        }

        val obj = element as? JsonObject
        if (obj == null) {
            JsonShapes.extractText(element)?.let { emitter.emit(it) }
            return true
        }

        JsonShapes.extractError(obj)?.let { error ->
            trySend(AgentEvent.Failed(error))
            return false
        }

        JsonShapes.extractTool(obj)?.let { trySend(it) }
        JsonShapes.extractStatus(obj)?.let { trySend(AgentEvent.Status(it)) }

        val text = JsonShapes.extractText(obj)
        if (!text.isNullOrEmpty()) emitter.emit(text)

        return !JsonShapes.isTerminal(obj)
    }

    override suspend fun ping(settings: HermesSettings): PingResult = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        // Try a health endpoint first, then the base URL: either proves reachability.
        val candidates = listOf(
            settings.normalizedBaseUrl() + "/health",
            settings.normalizedBaseUrl() + "/healthz",
            settings.normalizedBaseUrl() + "/",
        )
        var lastError = "Unreachable"
        for (url in candidates) {
            try {
                val request = Request.Builder().url(url).applyAuth(settings).get().build()
                Http.client(20).newCall(request).execute().use { response ->
                    val elapsed = (System.nanoTime() - started) / 1_000_000
                    if (response.code < 500) {
                        return@withContext PingResult(
                            ok = true,
                            message = "Reachable · ${url.removePrefix(settings.normalizedBaseUrl())} → HTTP ${response.code}",
                            latencyMs = elapsed,
                        )
                    }
                    lastError = "HTTP ${response.code} from $url"
                }
            } catch (t: Throwable) {
                lastError = t.friendlyMessage()
            }
        }
        PingResult(false, lastError, (System.nanoTime() - started) / 1_000_000)
    }
}

/**
 * Some servers stream deltas, others re-send the whole answer each frame.
 * If a chunk extends everything seen so far, only the new tail is emitted —
 * otherwise the transcript would repeat itself as the reply grows.
 */
internal class DeltaEmitter(private val sink: (String) -> Unit) {
    private val accumulated = StringBuilder()

    fun emit(chunk: String) {
        if (chunk.isEmpty()) return
        val soFar = accumulated.toString()
        if (soFar.isNotEmpty() && chunk.length > soFar.length && chunk.startsWith(soFar)) {
            val tail = chunk.substring(soFar.length)
            accumulated.append(tail)
            sink(tail)
        } else {
            accumulated.append(chunk)
            sink(chunk)
        }
    }
}
