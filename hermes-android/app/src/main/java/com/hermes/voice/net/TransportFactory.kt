package com.hermes.voice.net

import com.hermes.voice.data.TransportKind

/**
 * Hands out the transport for the currently selected mode. Instances are cached
 * because the WebSocket one owns a live connection that should survive turns.
 */
class TransportFactory {

    private val openAi by lazy { OpenAiCompatTransport() }
    private val rest by lazy { RestJsonTransport() }
    private val socket by lazy { WebSocketTransport() }

    fun forKind(kind: TransportKind): HermesTransport = when (kind) {
        TransportKind.OPENAI_COMPAT -> openAi
        TransportKind.REST_JSON -> rest
        TransportKind.WEBSOCKET -> socket
    }

    /** Best-effort interrupt; only the socket transport can signal the server. */
    fun cancelRemote(kind: TransportKind, conversationId: String) {
        if (kind == TransportKind.WEBSOCKET) socket.sendCancel(conversationId)
    }

    fun closeSocket() = socket.disconnect()
}
