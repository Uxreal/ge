package com.hermes.voice.net

import com.hermes.voice.data.HermesSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume

/**
 * Persistent ws:// or wss:// connection to the agent.
 *
 * One socket is kept open across turns so a stateful agent can also push
 * unsolicited frames (status, tool activity) between prompts. The socket is
 * lazily (re)connected whenever a turn starts and the previous one died.
 */
class WebSocketTransport : HermesTransport {

    private sealed interface Frame {
        data class Text(val body: String) : Frame
        data class Closed(val reason: String) : Frame
    }

    private val listeners = CopyOnWriteArrayList<(Frame) -> Unit>()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var connectedUrl: String? = null

    private fun dispatch(frame: Frame) = listeners.forEach { runCatching { it(frame) } }

    private val socketListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) = dispatch(Frame.Text(text))

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (socket === webSocket) {
                socket = null
                connectedUrl = null
            }
            dispatch(Frame.Closed(t.friendlyMessage()))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (socket === webSocket) {
                socket = null
                connectedUrl = null
            }
            dispatch(Frame.Closed(if (reason.isBlank()) "Socket closed ($code)" else reason))
        }
    }

    private fun ensureConnected(settings: HermesSettings): WebSocket {
        val url = settings.endpoint()
        socket?.let { existing -> if (connectedUrl == url) return existing }

        val request = Request.Builder().url(url).applyAuth(settings).build()
        val ws = Http.client(settings.requestTimeoutSec).newWebSocket(request, socketListener)
        socket = ws
        connectedUrl = url
        return ws
    }

    override fun stream(request: AgentRequest): Flow<AgentEvent> = callbackFlow {
        val settings = request.settings
        var finished = false
        var sawText = false
        var lastActivity = System.currentTimeMillis()

        val listener: (Frame) -> Unit = { frame ->
            lastActivity = System.currentTimeMillis()
            if (!finished) {
                when (frame) {
                    is Frame.Closed -> {
                        finished = true
                        trySend(AgentEvent.Failed(frame.reason))
                        close()
                    }

                    is Frame.Text -> {
                        val element = runCatching { lenientJson.parseToJsonElement(frame.body) }.getOrNull()
                        val obj = element as? JsonObject
                        if (obj == null) {
                            // Server is streaming raw text over the socket.
                            if (frame.body.isNotEmpty()) {
                            sawText = true
                            trySend(AgentEvent.Delta(frame.body))
                        }
                        } else {
                            val error = JsonShapes.extractError(obj)
                            if (error != null) {
                                finished = true
                                trySend(AgentEvent.Failed(error))
                                close()
                            } else {
                                JsonShapes.extractTool(obj)?.let { trySend(it) }
                                JsonShapes.extractStatus(obj)?.let { trySend(AgentEvent.Status(it)) }
                                JsonShapes.extractText(obj)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        sawText = true
                                        trySend(AgentEvent.Delta(it))
                                    }
                                if (JsonShapes.isTerminal(obj)) {
                                    finished = true
                                    trySend(AgentEvent.Completed)
                                    close()
                                }
                            }
                        }
                    }
                }
            }
        }

        listeners.add(listener)

        val sent = runCatching {
            val ws = ensureConnected(settings)
            ws.send(turnFrame(request).toString())
        }
        if (sent.isFailure || sent.getOrDefault(false) == false) {
            finished = true
            val reason = sent.exceptionOrNull()?.friendlyMessage()
                ?: "Could not send over the WebSocket — the connection is not open."
            trySend(AgentEvent.Failed(reason))
            close()
        }

        // Not every server marks the end of a turn. Without a watchdog the UI
        // would sit in "thinking" forever, so idle time ends the turn: cleanly
        // if text arrived, as a failure if nothing ever did. A server that does
        // send a terminal frame never reaches this path.
        val watchdog = launch(Dispatchers.Default) {
            val idleLimitMs = settings.requestTimeoutSec.coerceAtLeast(15) * 1000L
            val quietAfterTextMs = QUIET_AFTER_TEXT_MS
            while (isActive && !finished) {
                delay(500)
                val idleFor = System.currentTimeMillis() - lastActivity
                val done = if (sawText) idleFor >= quietAfterTextMs else idleFor >= idleLimitMs
                if (done) {
                    finished = true
                    if (sawText) {
                        trySend(AgentEvent.Completed)
                    } else {
                        trySend(AgentEvent.Failed("No response from Hermes over the socket."))
                    }
                    close()
                }
            }
        }

        awaitClose {
            watchdog.cancel()
            listeners.remove(listener)
        }
    }

    private fun turnFrame(request: AgentRequest) = buildJsonObject {
        val settings = request.settings
        put("type", "user_message")
        put("message", request.prompt)
        put("text", request.prompt)
        put("prompt", request.prompt)
        put("conversation_id", request.conversationId)
        put("session_id", request.conversationId)
        put("stream", true)
        if (settings.model.isNotBlank()) put("model", settings.model)
        put("temperature", settings.temperature)
        if (settings.maxTokens > 0) put("max_tokens", settings.maxTokens)
        if (settings.systemPrompt.isNotBlank()) put("system", settings.systemPrompt)
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
                    put("role", "user")
                    put("content", request.prompt)
                },
            )
        }
    }

    /** Best-effort interrupt for agents that honour a cancel frame. */
    fun sendCancel(conversationId: String) {
        val frame = buildJsonObject {
            put("type", "cancel")
            put("conversation_id", conversationId)
        }
        runCatching { socket?.send(frame.toString()) }
    }

    fun disconnect() {
        runCatching { socket?.close(1000, "client closed") }
        socket = null
        connectedUrl = null
    }

    override suspend fun ping(settings: HermesSettings): PingResult = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        val url = settings.endpoint()
        val request = Request.Builder().url(url).applyAuth(settings).build()

        val outcome = withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val probe = Http.client(20).newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            if (cont.isActive) cont.resume(Result.success(Unit))
                            webSocket.close(1000, "probe")
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            if (cont.isActive) cont.resume(Result.failure(t))
                        }
                    },
                )
                cont.invokeOnCancellation { runCatching { probe.cancel() } }
            }
        }

        val elapsed = (System.nanoTime() - started) / 1_000_000
        when {
            outcome == null -> PingResult(false, "Handshake timed out after 10s", elapsed)
            outcome.isSuccess -> PingResult(true, "Socket opened", elapsed)
            else -> PingResult(false, outcome.exceptionOrNull()?.friendlyMessage() ?: "Handshake failed", elapsed)
        }
    }

    private companion object {
        /**
         * Fallback only: how long the socket may stay silent after the agent has
         * already streamed text before the turn is treated as finished. Servers
         * that send a `{"type":"done"}` frame end their turns instantly instead.
         */
        const val QUIET_AFTER_TEXT_MS = 8_000L
    }
}
