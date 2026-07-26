package com.hermes.voice.net

import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * `POST {base}/v1/chat/completions` with `stream: true`, i.e. the shape served
 * by Ollama, llama.cpp, vLLM, LM Studio, LiteLLM, and most agent frameworks
 * that expose an OpenAI-compatible front door.
 */
class OpenAiCompatTransport : HermesTransport {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override fun stream(request: AgentRequest): Flow<AgentEvent> = callbackFlow {
        val settings = request.settings
        val body = buildJsonObject {
            if (settings.model.isNotBlank()) put("model", settings.model)
            put("stream", true)
            put("temperature", settings.temperature)
            if (settings.maxTokens > 0) put("max_tokens", settings.maxTokens)
            // Harmless for servers that ignore it; stateful agents use it to
            // thread the conversation on their side.
            put("user", request.conversationId)
            putJsonArray("messages") {
                buildHistory(request).forEach { message ->
                    add(
                        buildJsonObject {
                            put("role", message.role.wireName())
                            put("content", message.text)
                        },
                    )
                }
                add(
                    buildJsonObject {
                        put("role", Role.USER.wireName())
                        put("content", request.prompt)
                    },
                )
            }
        }

        val httpRequest = Request.Builder()
            .url(settings.endpoint())
            .header("Accept", "text/event-stream")
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

                    var sawContent = false
                    StreamReader.read(response) { payload ->
                        if (payload.trim() == "[DONE]") return@read false

                        val element = runCatching { lenientJson.parseToJsonElement(payload) }.getOrNull()
                            ?: return@read true
                        val obj = element as? JsonObject ?: return@read true

                        JsonShapes.extractError(obj)?.let { error ->
                            trySend(AgentEvent.Failed(error))
                            return@read false
                        }

                        JsonShapes.extractTool(obj)?.let { trySend(it) }

                        val text = JsonShapes.extractText(obj)
                        if (!text.isNullOrEmpty()) {
                            sawContent = true
                            trySend(AgentEvent.Delta(text))
                        }

                        !JsonShapes.isTerminal(obj)
                    }

                    if (!sawContent) {
                        trySend(AgentEvent.Status("No content in response stream"))
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

    override suspend fun ping(settings: HermesSettings): PingResult = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        val url = settings.normalizedBaseUrl() + "/v1/models"
        val request = Request.Builder().url(url).applyAuth(settings).get().build()
        try {
            Http.client(20).newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - started) / 1_000_000
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext PingResult(
                        ok = false,
                        message = "GET /v1/models → HTTP ${response.code}",
                        latencyMs = elapsed,
                    )
                }
                val models = runCatching {
                    JsonShapes.modelNames(lenientJson.parseToJsonElement(raw))
                }.getOrDefault(emptyList())
                PingResult(
                    ok = true,
                    message = if (models.isEmpty()) "Reachable" else "Reachable · ${models.size} model(s)",
                    latencyMs = elapsed,
                    models = models,
                )
            }
        } catch (t: Throwable) {
            PingResult(false, t.friendlyMessage(), (System.nanoTime() - started) / 1_000_000)
        }
    }
}

internal fun httpErrorMessage(code: Int, detail: String): String {
    val hint = when (code) {
        401, 403 -> "Check the API key."
        404 -> "Check the endpoint path and transport mode."
        429 -> "Rate limited."
        in 500..599 -> "Hermes returned a server error."
        else -> ""
    }
    val trimmed = detail.trim().replace(Regex("\\s+"), " ")
    return buildString {
        append("HTTP $code")
        if (hint.isNotEmpty()) append(" — ").append(hint)
        if (trimmed.isNotEmpty()) append(" · ").append(trimmed.take(240))
    }
}
