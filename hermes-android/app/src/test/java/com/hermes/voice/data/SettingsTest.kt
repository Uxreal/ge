package com.hermes.voice.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {

    @Test
    fun `openai endpoint appends the chat completions path`() {
        val settings = HermesSettings(baseUrl = "http://10.0.0.5:8080/", transport = TransportKind.OPENAI_COMPAT)
        assertEquals("http://10.0.0.5:8080/v1/chat/completions", settings.endpoint())
    }

    @Test
    fun `rest endpoint honours the configured path`() {
        val settings = HermesSettings(
            baseUrl = "https://hermes.example.com",
            transport = TransportKind.REST_JSON,
            chatPath = "agent/chat",
        )
        assertEquals("https://hermes.example.com/agent/chat", settings.endpoint())
    }

    @Test
    fun `websocket endpoint upgrades the scheme`() {
        val plain = HermesSettings(
            baseUrl = "http://10.0.0.5:8080",
            transport = TransportKind.WEBSOCKET,
            chatPath = "/ws",
        )
        assertEquals("ws://10.0.0.5:8080/ws", plain.endpoint())

        val secure = plain.copy(baseUrl = "https://hermes.example.com")
        assertEquals("wss://hermes.example.com/ws", secure.endpoint())
    }

    @Test
    fun `parses the extra header block`() {
        val settings = HermesSettings(
            extraHeaders = "X-Client: phone\n\nbroken-line\nX-Room:  office  ",
        )
        assertEquals(
            listOf("X-Client" to "phone", "X-Room" to "office"),
            settings.parsedHeaders(),
        )
    }

    @Test
    fun `a url with no scheme is left alone for the websocket transport`() {
        val settings = HermesSettings(
            baseUrl = "ws://10.0.0.5:9000",
            transport = TransportKind.WEBSOCKET,
            chatPath = "",
        )
        assertEquals("ws://10.0.0.5:9000", settings.endpoint())
    }
}
