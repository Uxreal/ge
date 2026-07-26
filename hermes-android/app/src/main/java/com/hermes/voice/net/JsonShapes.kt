package com.hermes.voice.net

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shape-tolerant readers for agent payloads.
 *
 * Self-hosted agents all invent their own JSON. Rather than force one schema,
 * these helpers probe the field names that show up in practice (OpenAI,
 * Anthropic, Ollama, LangServe, and hand-rolled servers) and pull out whatever
 * text, tool call, or terminator is there.
 */
object JsonShapes {

    /** Checked in priority order — earlier keys win when several are present. */
    private val TEXT_KEYS = listOf(
        "delta", "text", "content", "token", "chunk", "output_text",
        "response", "answer", "reply", "completion", "output", "result", "message", "data",
    )

    private val STATUS_KEYS = listOf("status", "stage", "state", "progress", "thought", "thinking")

    private val TERMINAL_TYPES = setOf(
        "done", "end", "final", "complete", "completed", "finish", "finished",
        "stop", "message_stop", "response.completed", "close", "eos",
    )

    private val TOOL_TYPES = setOf(
        "tool", "tool_call", "tool_use", "function_call", "action", "tool_result",
    )

    private val ERROR_KEYS = listOf("error", "err", "detail", "message")

    fun typeOf(obj: JsonObject): String? =
        (obj["type"] ?: obj["event"] ?: obj["kind"] ?: obj["object"])
            ?.stringOrNull()
            ?.lowercase()

    fun isTerminal(obj: JsonObject): Boolean {
        val type = typeOf(obj)
        if (type != null && TERMINAL_TYPES.any { type == it || type.endsWith(".$it") }) return true
        if (obj["done"]?.booleanish() == true) return true
        if (obj["is_final"]?.booleanish() == true) return true
        val finish = obj["finish_reason"] ?: obj["stop_reason"]
            ?: (obj["choices"] as? JsonArray)?.firstOrNull()?.asObject()?.get("finish_reason")
        return finish != null && finish !is JsonNull
    }

    fun isToolEvent(obj: JsonObject): Boolean {
        val type = typeOf(obj) ?: return obj.containsKey("tool_calls") || obj.containsKey("tool_call")
        return TOOL_TYPES.any { type.contains(it) }
    }

    fun extractTool(obj: JsonObject): AgentEvent.Tool? {
        val direct = obj["tool_call"]?.asObject() ?: obj["tool"]?.asObject() ?: obj["function"]?.asObject()
        val fromArray = (obj["tool_calls"] as? JsonArray)?.firstOrNull()?.asObject()
        val holder = direct ?: fromArray ?: obj.takeIf { isToolEvent(it) } ?: return null
        val nested = holder["function"]?.asObject()
        val name = (nested?.get("name") ?: holder["name"] ?: holder["tool_name"] ?: holder["action"])
            ?.stringOrNull()
            ?: return null
        val detail = (nested?.get("arguments") ?: holder["arguments"] ?: holder["args"] ?: holder["input"])
            ?.let { if (it is JsonPrimitive) it.content else it.toString() }
            .orEmpty()
        return AgentEvent.Tool(name, detail.take(500))
    }

    fun extractStatus(obj: JsonObject): String? {
        val type = typeOf(obj)
        if (type != null && (type.contains("status") || type.contains("progress") || type == "thinking")) {
            extractText(obj, allowMessageKey = true)?.let { if (it.isNotBlank()) return it.trim() }
        }
        for (key in STATUS_KEYS) {
            val v = obj[key]?.stringOrNull()
            if (!v.isNullOrBlank() && v.length < 120) return v
        }
        return null
    }

    fun extractError(obj: JsonObject): String? {
        for (key in ERROR_KEYS) {
            val node = obj[key] ?: continue
            when (node) {
                is JsonPrimitive -> if (key != "message" || typeOf(obj) == "error") {
                    node.contentOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
                }
                is JsonObject -> (node["message"] ?: node["detail"] ?: node["error"])
                    ?.stringOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    /**
     * Pulls assistant text out of an arbitrary payload.
     *
     * [allowMessageKey] is off by default in the streaming path because many
     * servers use `message` for status/error strings; it is enabled for
     * non-streaming replies where `message` usually is the answer.
     */
    fun extractText(element: JsonElement?, allowMessageKey: Boolean = false, depth: Int = 0): String? {
        if (element == null || element is JsonNull || depth > 6) return null

        when (element) {
            // Only strings count as prose once we are inside an object; a bare
            // top-level primitive is accepted because some servers reply with
            // nothing but a quoted string.
            is JsonPrimitive -> return if (element.isString || depth == 0) element.contentOrNull() else null

            is JsonArray -> {
                val parts = element.mapNotNull { extractText(it, allowMessageKey, depth + 1) }
                    .filter { it.isNotEmpty() }
                return if (parts.isEmpty()) null else parts.joinToString("")
            }

            is JsonObject -> {
                // OpenAI chat completions, streaming and not.
                (element["choices"] as? JsonArray)?.firstOrNull()?.asObject()?.let { choice ->
                    val fromChoice = extractText(choice["delta"], allowMessageKey, depth + 1)
                        ?: extractText(choice["message"], true, depth + 1)
                        ?: extractText(choice["text"], allowMessageKey, depth + 1)
                    if (!fromChoice.isNullOrEmpty()) return fromChoice
                }

                // Anthropic-style content blocks: [{type:"text", text:"..."}]
                (element["content"] as? JsonArray)?.let { blocks ->
                    val joined = blocks.mapNotNull { block ->
                        val o = block.asObject() ?: return@mapNotNull extractText(block, allowMessageKey, depth + 1)
                        if (o["type"]?.stringOrNull() == "text" || o.containsKey("text")) {
                            o["text"]?.stringOrNull()
                        } else {
                            null
                        }
                    }.joinToString("")
                    if (joined.isNotEmpty()) return joined
                }

                for (key in TEXT_KEYS) {
                    if (key == "message" && !allowMessageKey) continue
                    val child = element[key] ?: continue
                    // Never treat a tool payload as assistant prose.
                    if (key == "content" && child is JsonObject && child.containsKey("tool_calls")) continue
                    val text = extractText(child, allowMessageKey, depth + 1)
                    if (!text.isNullOrEmpty()) return text
                }
                return null
            }
        }
    }

    fun extractConversationId(obj: JsonObject): String? =
        (obj["conversation_id"] ?: obj["conversationId"] ?: obj["session_id"] ?: obj["sessionId"] ?: obj["thread_id"])
            ?.stringOrNull()

    // --- small helpers -----------------------------------------------------

    private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

    private fun JsonElement.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull()

    private fun JsonPrimitive.contentOrNull(): String? = if (this is JsonNull) null else content

    private fun JsonElement.booleanish(): Boolean? =
        (this as? JsonPrimitive)?.let { it.booleanOrNull ?: (it.content == "true") }

    /** Public accessor used by transports that already hold a [JsonElement]. */
    fun stringField(obj: JsonObject, vararg keys: String): String? {
        for (k in keys) obj[k]?.let { el -> (el as? JsonPrimitive)?.let { return it.content } }
        return null
    }

    fun modelNames(root: JsonElement): List<String> {
        val obj = root as? JsonObject
        val array = (obj?.get("data") as? JsonArray)
            ?: (obj?.get("models") as? JsonArray)
            ?: (root as? JsonArray)
            ?: return emptyList()
        return array.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.content
                is JsonObject -> (item["id"] ?: item["name"] ?: item["model"])?.jsonPrimitive?.content
                else -> null
            }
        }
    }
}
