package com.hermes.voice.net

import com.hermes.voice.data.ChatMessage
import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.Role
import com.hermes.voice.data.TransportKind
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-end transport tests over a real socket (MockWebServer), so the HTTP
 * request that gets built, the SSE/NDJSON framing, and the event mapping are
 * all exercised together rather than mocked apart.
 */
class TransportTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        // A pooled keep-alive connection can outlive the test; never fail a
        // passing assertion over teardown.
        runCatching { server.shutdown() }
    }

    private fun settings(kind: TransportKind, path: String = "/chat") = HermesSettings(
        baseUrl = server.url("/").toString().trimEnd('/'),
        transport = kind,
        chatPath = path,
        apiKey = "secret-token",
        model = "hermes-1",
        systemPrompt = "Be brief.",
        requestTimeoutSec = 20,
    )

    private fun request(settings: HermesSettings) = AgentRequest(
        conversationId = "conv-1",
        prompt = "what is the build status",
        history = listOf(ChatMessage(role = Role.USER, text = "hello", timestamp = 1L)),
        settings = settings,
    )

    private fun sse(vararg frames: String) = frames.joinToString("") { "data: $it\n\n" }

    private fun deltas(events: List<AgentEvent>) =
        events.filterIsInstance<AgentEvent.Delta>().joinToString("") { it.text }

    // --- OpenAI-compatible -------------------------------------------------

    @Test
    fun `openai transport streams sse deltas and completes`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    sse(
                        """{"choices":[{"delta":{"role":"assistant"}}]}""",
                        """{"choices":[{"delta":{"content":"Build "}}]}""",
                        """{"choices":[{"delta":{"content":"is green."}}]}""",
                        "[DONE]",
                    ),
                ),
        )

        val settings = settings(TransportKind.OPENAI_COMPAT)
        val events = withTimeout(10_000) {
            OpenAiCompatTransport().stream(request(settings)).toList()
        }

        assertEquals("Build is green.", deltas(events))
        assertTrue(events.last() is AgentEvent.Completed)

        val recorded: RecordedRequest = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer secret-token", recorded.getHeader("Authorization"))
        val body = recorded.body.readUtf8()
        assertTrue("stream flag missing", body.contains("\"stream\":true"))
        assertTrue("model missing", body.contains("\"hermes-1\""))
        assertTrue("system prompt missing", body.contains("Be brief."))
        assertTrue("history missing", body.contains("hello"))
        assertTrue("prompt missing", body.contains("what is the build status"))
    }

    @Test
    fun `openai transport reports an http error with a hint`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"bad key"}"""))

        val events = withTimeout(10_000) {
            OpenAiCompatTransport().stream(request(settings(TransportKind.OPENAI_COMPAT))).toList()
        }

        val failure = events.filterIsInstance<AgentEvent.Failed>().single()
        assertTrue(failure.message.contains("401"))
        assertTrue(failure.message.contains("API key"))
    }

    @Test
    fun `openai transport surfaces an in-stream error frame`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(sse("""{"error":{"message":"context length exceeded"}}""")),
        )

        val events = withTimeout(10_000) {
            OpenAiCompatTransport().stream(request(settings(TransportKind.OPENAI_COMPAT))).toList()
        }

        assertEquals("context length exceeded", events.filterIsInstance<AgentEvent.Failed>().single().message)
    }

    // --- Generic JSON REST -------------------------------------------------

    @Test
    fun `rest transport reads ndjson deltas`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(
                    """{"response":"Everything "}""" + "\n" +
                        """{"response":"looks fine."}""" + "\n" +
                        """{"done":true}""" + "\n",
                ),
        )

        val events = withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON))).toList()
        }

        assertEquals("Everything looks fine.", deltas(events))
        assertTrue(events.last() is AgentEvent.Completed)
        assertEquals("/chat", server.takeRequest().path)
    }

    @Test
    fun `rest transport collapses a cumulative stream`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(
                    """{"text":"Deploy"}""" + "\n" +
                        """{"text":"Deploy finished"}""" + "\n" +
                        """{"text":"Deploy finished cleanly."}""" + "\n",
                ),
        )

        val events = withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON))).toList()
        }

        assertEquals("Deploy finished cleanly.", deltas(events))
    }

    @Test
    fun `rest transport reads a single json reply`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"Three jobs are running."}"""),
        )

        val events = withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON))).toList()
        }

        assertEquals("Three jobs are running.", deltas(events))
        assertTrue(events.last() is AgentEvent.Completed)
    }

    @Test
    fun `rest transport reports tool and status events`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/x-ndjson")
                .setBody(
                    """{"type":"status","status":"searching the repo"}""" + "\n" +
                        """{"type":"tool_call","name":"grep","args":{"pattern":"TODO"}}""" + "\n" +
                        """{"text":"Found 3."}""" + "\n" +
                        """{"type":"done"}""" + "\n",
                ),
        )

        val events = withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON))).toList()
        }

        assertEquals("searching the repo", events.filterIsInstance<AgentEvent.Status>().first().text)
        assertEquals("grep", events.filterIsInstance<AgentEvent.Tool>().single().name)
        assertEquals("Found 3.", deltas(events))
    }

    @Test
    fun `rest transport explains an unrecognised body`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{"unexpected":{"nested":123}}"""),
        )

        val events = withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON))).toList()
        }

        val failure = events.filterIsInstance<AgentEvent.Failed>().single()
        assertTrue(failure.message.contains("no text field was recognised"))
    }

    @Test
    fun `rest transport sends the prompt under every common field name`() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"text":"ok"}"""))

        withTimeout(10_000) {
            RestJsonTransport().stream(request(settings(TransportKind.REST_JSON, path = "/agent"))).toList()
        }

        val body = server.takeRequest().body.readUtf8()
        listOf("\"message\"", "\"prompt\"", "\"input\"", "\"query\"", "\"messages\"", "\"history\"")
            .forEach { field -> assertTrue("$field missing from request", body.contains(field)) }
        assertTrue(body.contains("\"conversation_id\":\"conv-1\""))
    }

    // --- WebSocket ---------------------------------------------------------

    @Test
    fun `websocket transport maps frames to events`() = runBlocking {
        val listener = object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                // Reply as an agent would: a status, two deltas, then a terminator.
                webSocket.send("""{"type":"status","status":"thinking"}""")
                webSocket.send("""{"type":"delta","text":"All "}""")
                webSocket.send("""{"type":"delta","text":"clear."}""")
                webSocket.send("""{"type":"done"}""")
                webSocket.close(1000, "turn complete")
            }
        }
        server.enqueue(MockResponse().withWebSocketUpgrade(listener))

        val settings = settings(TransportKind.WEBSOCKET, path = "/ws")
        val transport = WebSocketTransport()
        val events = withTimeout(15_000) { transport.stream(request(settings)).toList() }
        transport.disconnect()

        assertEquals("All clear.", deltas(events))
        assertEquals("thinking", events.filterIsInstance<AgentEvent.Status>().first().text)
        assertTrue(events.last() is AgentEvent.Completed)
    }

    @Test
    fun `websocket transport reports a socket failure`() = runBlocking {
        // No upgrade offered: the handshake fails.
        server.enqueue(MockResponse().setResponseCode(500))

        val transport = WebSocketTransport()
        val events = withTimeout(15_000) {
            transport.stream(request(settings(TransportKind.WEBSOCKET, path = "/ws"))).toList()
        }
        transport.disconnect()

        assertTrue(events.any { it is AgentEvent.Failed })
    }
}
