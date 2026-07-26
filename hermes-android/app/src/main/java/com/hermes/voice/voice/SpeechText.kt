package com.hermes.voice.voice

/**
 * Prepares agent output for text-to-speech.
 *
 * Agents answer in markdown; read literally that becomes "asterisk asterisk
 * important asterisk asterisk". This strips the syntax, drops fenced code (a
 * phone reading out a shell script is never what you wanted) and splits the
 * stream on sentence boundaries so speaking can start before generation ends.
 */
object SpeechText {

    private val fencedCode = Regex("```[\\s\\S]*?```")
    private val danglingFence = Regex("```[\\s\\S]*$")
    private val inlineCode = Regex("`([^`]*)`")
    private val imageLink = Regex("!\\[[^]]*]\\([^)]*\\)")
    private val markdownLink = Regex("\\[([^]]+)]\\([^)]*\\)")
    private val emphasis = Regex("(\\*\\*|\\*|__|_|~~)")
    private val heading = Regex("(?m)^\\s{0,3}#{1,6}\\s*")
    private val blockquote = Regex("(?m)^\\s{0,3}>\\s?")
    private val bullet = Regex("(?m)^\\s*[-*+]\\s+")
    private val rule = Regex("(?m)^\\s*([-*_])\\1{2,}\\s*$")
    private val bareUrl = Regex("https?://\\S+")
    private val whitespace = Regex("[ \\t]{2,}")

    fun forSpeech(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw
        text = fencedCode.replace(text, " (code block) ")
        text = danglingFence.replace(text, " (code block) ")
        text = imageLink.replace(text, " ")
        text = markdownLink.replace(text, "$1")
        text = inlineCode.replace(text, "$1")
        text = heading.replace(text, "")
        text = blockquote.replace(text, "")
        text = rule.replace(text, "")
        text = bullet.replace(text, "")
        text = emphasis.replace(text, "")
        text = bareUrl.replace(text, " link ")
        text = whitespace.replace(text, " ")
        return text.trim()
    }

    /**
     * Splits [buffer] into a leading chunk that is safe to speak now and the
     * remainder to keep buffering. Returns null when nothing is speakable yet.
     */
    fun takeSpeakableChunk(buffer: String, force: Boolean): Pair<String, String>? {
        if (buffer.isBlank()) return null

        if (force) {
            val cleaned = forSpeech(buffer)
            return if (cleaned.isBlank()) null else cleaned to ""
        }

        val boundary = lastSentenceBoundary(buffer)
        if (boundary > 0) {
            val head = buffer.substring(0, boundary)
            val tail = buffer.substring(boundary)
            val cleaned = forSpeech(head)
            return if (cleaned.isBlank()) null else cleaned to tail
        }

        // No punctuation for a long while — flush at a word break so a rambling
        // reply still starts playing instead of buffering to the very end.
        if (buffer.length >= MAX_BUFFER) {
            val cut = buffer.lastIndexOf(' ', MAX_BUFFER).takeIf { it > MAX_BUFFER / 2 } ?: MAX_BUFFER
            val cleaned = forSpeech(buffer.substring(0, cut))
            return if (cleaned.isBlank()) null else cleaned to buffer.substring(cut)
        }

        return null
    }

    /** Index just past the last sentence terminator, or 0 if there is none. */
    private fun lastSentenceBoundary(text: String): Int {
        for (i in text.length - 1 downTo 0) {
            val c = text[i]
            if (c == '\n') return i + 1
            if (c == '.' || c == '!' || c == '?' || c == '…' || c == ':' || c == ';') {
                // Ignore decimals ("3.5") and common abbreviations mid-number.
                val next = text.getOrNull(i + 1)
                val prev = text.getOrNull(i - 1)
                val isDecimal = c == '.' && prev?.isDigit() == true && next?.isDigit() == true
                if (!isDecimal && (next == null || next == ' ' || next == '\n')) return i + 1
            }
        }
        return 0
    }

    private const val MAX_BUFFER = 220
}
