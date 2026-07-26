package com.hermes.voice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Just enough markdown for agent replies: fenced code blocks get their own
 * scrollable surface, and inline `code`, **bold**, *italic* are styled in place.
 * A full markdown renderer would be a dependency and a lot of surface area for
 * what a chat bubble actually needs.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(text) { splitFences(text) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> CodeBlock(block.body)
                is Block.Prose -> Text(
                    text = inlineMarkdown(block.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(body: String) {
    Text(
        text = body.trimEnd(),
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private sealed interface Block {
    data class Prose(val body: String) : Block
    data class Code(val body: String) : Block
}

private fun splitFences(text: String): List<Block> {
    if (!text.contains("```")) return listOf(Block.Prose(text))
    val blocks = mutableListOf<Block>()
    val parts = text.split("```")
    parts.forEachIndexed { index, part ->
        if (part.isEmpty()) return@forEachIndexed
        if (index % 2 == 0) {
            blocks += Block.Prose(part.trim('\n'))
        } else {
            // Drop an opening language tag ("kotlin\n...").
            val body = part.substringAfter('\n', part)
            blocks += Block.Code(body)
        }
    }
    return blocks.ifEmpty { listOf(Block.Prose(text)) }
}

private fun inlineMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < source.length) {
        when {
            source.startsWith("**", i) -> {
                val end = source.indexOf("**", i + 2)
                if (end > 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(source.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append(source[i]); i++
                }
            }

            source[i] == '`' -> {
                val end = source.indexOf('`', i + 1)
                if (end > 0) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                    append(source.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(source[i]); i++
                }
            }

            (source[i] == '*' || source[i] == '_') && isEmphasisStart(source, i) -> {
                val marker = source[i]
                val end = source.indexOf(marker, i + 1)
                if (end > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(source.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(source[i]); i++
                }
            }

            else -> {
                append(source[i]); i++
            }
        }
    }
}

/** Avoids italicising snake_case identifiers and bare asterisks. */
private fun isEmphasisStart(source: String, index: Int): Boolean {
    val prev = source.getOrNull(index - 1)
    val next = source.getOrNull(index + 1)
    if (next == null || next.isWhitespace()) return false
    return prev == null || !prev.isLetterOrDigit()
}
