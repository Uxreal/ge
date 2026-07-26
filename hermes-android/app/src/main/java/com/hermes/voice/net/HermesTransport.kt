package com.hermes.voice.net

import com.hermes.voice.data.ChatMessage
import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface HermesTransport {
    /** Streams one turn. The flow completes after [AgentEvent.Completed] or [AgentEvent.Failed]. */
    fun stream(request: AgentRequest): Flow<AgentEvent>

    /** Reachability check for the Settings screen and the connection dot. */
    suspend fun ping(settings: HermesSettings): PingResult
}

internal val lenientJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

/** One shared connection pool; per-request timeouts come from settings. */
object Http {
    private var cached: Pair<Int, OkHttpClient>? = null

    fun client(timeoutSec: Int): OkHttpClient {
        cached?.let { (sec, client) -> if (sec == timeoutSec) return client }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Streaming turns can idle between tokens; read timeout must cover it.
            .readTimeout(timeoutSec.toLong().coerceAtLeast(30), TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        cached = timeoutSec to client
        return client
    }
}

internal fun Request.Builder.applyAuth(settings: HermesSettings): Request.Builder {
    if (settings.apiKey.isNotBlank()) {
        val key = settings.apiKey.trim()
        // Accept a raw token or a fully-formed scheme ("Bearer x", "Token x").
        val value = if (key.contains(' ')) key else "Bearer $key"
        header("Authorization", value)
        header("x-api-key", key)
    }
    settings.parsedHeaders().forEach { (name, value) -> header(name, value) }
    return this
}

/** Trims history to the configured depth and prepends the system prompt. */
internal fun buildHistory(request: AgentRequest): List<ChatMessage> {
    val s = request.settings
    val trimmed = if (!s.sendHistory) {
        emptyList()
    } else {
        request.history
            .filterNot { it.isError }
            .takeLast(s.historyDepth.coerceAtLeast(0))
    }
    val system = s.systemPrompt.trim()
    val prefix = if (system.isEmpty()) {
        emptyList()
    } else {
        listOf(ChatMessage(role = Role.SYSTEM, text = system, timestamp = 0L))
    }
    return prefix + trimmed
}

internal fun Role.wireName(): String = when (this) {
    Role.USER -> "user"
    Role.ASSISTANT -> "assistant"
    Role.SYSTEM -> "system"
}

/** Turns an exception into something worth showing on a phone screen. */
internal fun Throwable.friendlyMessage(): String = when (this) {
    is java.net.UnknownHostException -> "Can't resolve the host. Check the base URL and that the phone is on the right network."
    is java.net.ConnectException -> "Connection refused. Is Hermes running and reachable from this phone?"
    is java.net.SocketTimeoutException -> "Timed out waiting for Hermes."
    is javax.net.ssl.SSLException -> "TLS error: ${message ?: "handshake failed"}"
    else -> message ?: this::class.java.simpleName
}
