package com.hermes.voice.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These cover the payload shapes real agent servers emit. If a new server shape
 * shows up, add it here first — this is the file that says what the app can
 * actually talk to.
 */
class JsonShapesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun obj(raw: String): JsonObject = json.parseToJsonElement(raw) as JsonObject

    @Test
    fun `reads openai streaming delta`() {
        val payload = obj(
            """{"choices":[{"delta":{"content":"Hello"},"index":0,"finish_reason":null}]}""",
        )
        assertEquals("Hello", JsonShapes.extractText(payload))
        assertFalse(JsonShapes.isTerminal(payload))
    }

    @Test
    fun `reads openai non-streaming message`() {
        val payload = obj("""{"choices":[{"message":{"role":"assistant","content":"Done."}}]}""")
        assertEquals("Done.", JsonShapes.extractText(payload, allowMessageKey = true))
    }

    @Test
    fun `finish reason terminates the stream`() {
        val payload = obj("""{"choices":[{"delta":{},"finish_reason":"stop"}]}""")
        assertTrue(JsonShapes.isTerminal(payload))
    }

    @Test
    fun `reads ollama style response field`() {
        val payload = obj("""{"model":"llama3","response":"partial text","done":false}""")
        assertEquals("partial text", JsonShapes.extractText(payload))
        assertFalse(JsonShapes.isTerminal(payload))
    }

    @Test
    fun `ollama done flag terminates`() {
        assertTrue(JsonShapes.isTerminal(obj("""{"response":"","done":true}""")))
    }

    @Test
    fun `reads anthropic style content blocks`() {
        val payload = obj("""{"content":[{"type":"text","text":"block one"},{"type":"text","text":" and two"}]}""")
        assertEquals("block one and two", JsonShapes.extractText(payload))
    }

    @Test
    fun `reads a bare delta text event`() {
        assertEquals("tok", JsonShapes.extractText(obj("""{"type":"delta","text":"tok"}""")))
    }

    @Test
    fun `message_stop event terminates`() {
        assertTrue(JsonShapes.isTerminal(obj("""{"type":"message_stop"}""")))
    }

    @Test
    fun `numbers are not mistaken for prose`() {
        // "data" is a text key, but a numeric value is not something to speak.
        assertNull(JsonShapes.extractText(obj("""{"data":42}""")))
    }

    @Test
    fun `extracts an error object`() {
        val payload = obj("""{"error":{"message":"model not found","type":"invalid_request"}}""")
        assertEquals("model not found", JsonShapes.extractError(payload))
    }

    @Test
    fun `extracts a bare error string`() {
        assertEquals("boom", JsonShapes.extractError(obj("""{"error":"boom"}""")))
    }

    @Test
    fun `a plain message field is not treated as an error`() {
        assertNull(JsonShapes.extractError(obj("""{"message":"here is your answer"}""")))
    }

    @Test
    fun `extracts an openai tool call`() {
        val payload = obj(
            """{"choices":[{"delta":{"tool_calls":[{"function":{"name":"search","arguments":"{\"q\":\"x\"}"}}]}}]}""",
        )
        // The tool array lives under the delta, so read that level directly.
        val delta = (payload["choices"] as kotlinx.serialization.json.JsonArray)
            .first().let { it as JsonObject }["delta"] as JsonObject
        val tool = JsonShapes.extractTool(delta)
        assertEquals("search", tool?.name)
    }

    @Test
    fun `extracts a hand rolled tool event`() {
        val tool = JsonShapes.extractTool(obj("""{"type":"tool_call","name":"deploy","args":{"env":"prod"}}"""))
        assertEquals("deploy", tool?.name)
        assertTrue(tool!!.detail.contains("prod"))
    }

    @Test
    fun `reads a status event`() {
        assertEquals("searching", JsonShapes.extractStatus(obj("""{"status":"searching"}""")))
    }

    @Test
    fun `lists models from an openai style payload`() {
        val root = json.parseToJsonElement("""{"data":[{"id":"m1"},{"id":"m2"}]}""")
        assertEquals(listOf("m1", "m2"), JsonShapes.modelNames(root))
    }

    @Test
    fun `lists models from an ollama style payload`() {
        val root = json.parseToJsonElement("""{"models":[{"name":"llama3"}]}""")
        assertEquals(listOf("llama3"), JsonShapes.modelNames(root))
    }

    @Test
    fun `finds the conversation id under several spellings`() {
        assertEquals("abc", JsonShapes.extractConversationId(obj("""{"session_id":"abc"}""")))
        assertEquals("xyz", JsonShapes.extractConversationId(obj("""{"conversationId":"xyz"}""")))
    }
}
