package com.hermes.voice.voice

/**
 * Wake-phrase matching for the always-listening mode.
 *
 * On-device recognition mangles short phrases ("hey hermes" comes back as "hey
 * her mess", "a hermes", "hey hermès"), so exact matching would barely ever
 * fire. Matching is therefore fuzzy: normalize, then accept a close-enough
 * window anywhere in the transcript.
 */
object WakePhrase {

    private val nonLetters = Regex("[^a-z0-9 ]")
    private val spaces = Regex(" +")

    fun normalize(text: String): String =
        nonLetters.replace(text.lowercase(), " ").let { spaces.replace(it, " ") }.trim()

    /**
     * True when [transcript] contains [phrase] closely enough to count as the
     * wake word. Returns false for a blank phrase so the caller never wakes on
     * everything by accident.
     */
    fun matches(transcript: String, phrase: String): Boolean {
        val target = normalize(phrase)
        if (target.isEmpty()) return false
        val heard = normalize(transcript)
        if (heard.isEmpty()) return false
        if (heard.contains(target)) return true

        val targetWords = target.split(' ')
        val heardWords = heard.split(' ')
        if (heardWords.size < targetWords.size) {
            // Whole transcript is shorter than the phrase: compare directly.
            return similarity(heard, target) >= THRESHOLD
        }

        // Slide a window the size of the phrase across the transcript.
        for (start in 0..(heardWords.size - targetWords.size)) {
            val window = heardWords.subList(start, start + targetWords.size).joinToString(" ")
            if (similarity(window, target) >= THRESHOLD) return true
        }
        // Also try a window one word longer — recognizers love splitting a name
        // into two tokens ("her mess").
        val wider = targetWords.size + 1
        if (heardWords.size >= wider) {
            for (start in 0..(heardWords.size - wider)) {
                val window = heardWords.subList(start, start + wider).joinToString(" ")
                if (similarity(window.replace(" ", ""), target.replace(" ", "")) >= THRESHOLD) return true
            }
        }
        return false
    }

    /**
     * Everything after the wake phrase, when the user runs straight on
     * ("hey hermes what's the build status"). Empty when there is no tail.
     */
    fun remainderAfterWake(transcript: String, phrase: String): String {
        val target = normalize(phrase)
        val heard = normalize(transcript)
        val idx = heard.indexOf(target)
        if (idx < 0) return ""
        return heard.substring(idx + target.length).trim()
    }

    /** 0..1 similarity based on Levenshtein distance. */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val distance = levenshtein(a, b)
        val longest = maxOf(a.length, b.length)
        return 1.0 - distance.toDouble() / longest
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    private const val THRESHOLD = 0.72
}
