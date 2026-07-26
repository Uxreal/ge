package com.hermes.voice.net

import okhttp3.Response
import okio.BufferedSource

/**
 * Reads a streaming HTTP body that may be either Server-Sent Events or NDJSON,
 * and hands each decoded payload to [onPayload].
 *
 * SSE frames accumulate `data:` lines until a blank line; NDJSON is one JSON
 * object per line. Both appear in the wild, sometimes from the same server
 * depending on flags, so this detects per line rather than trusting the
 * Content-Type header.
 *
 * [onPayload] returns false to stop reading early (e.g. after a terminator).
 */
internal object StreamReader {

    fun read(response: Response, onPayload: (String) -> Boolean) {
        val source: BufferedSource = response.body?.source() ?: return
        val dataBuffer = StringBuilder()

        while (true) {
            val line = try {
                source.readUtf8Line()
            } catch (_: Throwable) {
                null
            } ?: break

            when {
                // Blank line: end of an SSE frame.
                line.isBlank() -> {
                    if (dataBuffer.isNotEmpty()) {
                        val payload = dataBuffer.toString()
                        dataBuffer.setLength(0)
                        if (!onPayload(payload)) return
                    }
                }

                line.startsWith("data:") -> {
                    val chunk = line.removePrefix("data:").removePrefix(" ")
                    if (dataBuffer.isNotEmpty()) dataBuffer.append('\n')
                    dataBuffer.append(chunk)
                }

                // SSE metadata lines we don't need.
                line.startsWith(":") || line.startsWith("event:") ||
                    line.startsWith("id:") || line.startsWith("retry:") -> Unit

                // Bare NDJSON / plain-text line.
                else -> if (!onPayload(line)) return
            }
        }

        if (dataBuffer.isNotEmpty()) onPayload(dataBuffer.toString())
    }
}
