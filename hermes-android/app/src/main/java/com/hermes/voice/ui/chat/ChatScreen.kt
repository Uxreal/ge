package com.hermes.voice.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermes.voice.data.ChatMessage
import com.hermes.voice.data.Role
import com.hermes.voice.data.VoiceMode
import com.hermes.voice.engine.ConnectionHealth
import com.hermes.voice.engine.EngineState
import com.hermes.voice.engine.VoiceState
import com.hermes.voice.ui.components.MarkdownText
import com.hermes.voice.ui.components.VoiceOrb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: EngineState,
    onSend: (String) -> Unit,
    onMicTap: () -> Unit,
    onVoiceModeChange: (VoiceMode) -> Unit,
    onStop: () -> Unit,
    onStopSpeaking: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenConversations: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMacros: () -> Unit,
    onDismissNotice: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = state.conversation?.messages.orEmpty()

    // Follow the tail of the transcript as tokens arrive.
    LaunchedEffect(messages.size, state.streamingText, state.partialTranscript) {
        val count = messages.size + if (state.streamingText.isNotEmpty() || state.partialTranscript.isNotEmpty()) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.conversation?.title?.takeIf { it.isNotBlank() } ?: "Hermes",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ConnectionLine(state)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMacros) {
                        Icon(Icons.Default.Bolt, contentDescription = "Voice shortcuts")
                    }
                    IconButton(onClick = onOpenConversations) {
                        Icon(Icons.Default.Forum, contentDescription = "Conversations")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            AnimatedVisibility(visible = state.notice != null) {
                NoticeBar(state.notice.orEmpty(), onDismissNotice)
            }

            if (messages.isEmpty() && state.streamingText.isEmpty()) {
                EmptyState(
                    configured = state.settings.isConfigured,
                    wakePhrase = state.settings.wakePhrase,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { message -> MessageBubble(message) }

                    if (state.streamingText.isNotEmpty() || state.streaming) {
                        item(key = "streaming") {
                            StreamingBubble(state.streamingText, state.agentStatus)
                        }
                    }
                    if (state.partialTranscript.isNotEmpty()) {
                        item(key = "partial") { PartialTranscript(state.partialTranscript) }
                    }
                }
            }

            VoiceBar(
                state = state,
                onMicTap = onMicTap,
                onVoiceModeChange = onVoiceModeChange,
                onStop = onStop,
                onStopSpeaking = onStopSpeaking,
            )

            Composer(
                draft = draft,
                onDraftChange = { draft = it },
                enabled = state.settings.isConfigured,
                streaming = state.streaming,
                onSend = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        onSend(text)
                        draft = ""
                    }
                },
                onStop = onStop,
                onNewConversation = onNewConversation,
            )
        }
    }
}

@Composable
private fun ConnectionLine(state: EngineState) {
    val scheme = MaterialTheme.colorScheme
    val (dot, label) = when (state.connection.health) {
        ConnectionHealth.OK -> scheme.tertiary to
            "Connected · ${state.connection.latencyMs}ms · ${state.settings.transport.label}"
        ConnectionHealth.CHECKING -> scheme.secondary to "Checking…"
        ConnectionHealth.FAILED -> scheme.error to state.connection.message
        ConnectionHealth.UNKNOWN -> scheme.onSurfaceVariant to
            if (state.settings.isConfigured) state.settings.normalizedBaseUrl() else "Not configured"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NoticeBar(text: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(
    configured: Boolean,
    wakePhrase: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VoiceOrb(state = VoiceState.IDLE, level = 0f, size = 96.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (configured) "Ready" else "Point this at Hermes",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (configured) {
                "Hold the mic to talk, or switch on hands-free and just say “$wakePhrase”."
            } else {
                "Add your agent's base URL and pick a transport, then come back and talk to it."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (!configured) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenSettings) { Text("Open settings") }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val fromUser = message.role == Role.USER
    val scheme = MaterialTheme.colorScheme
    val background = when {
        message.isError -> scheme.errorContainer
        fromUser -> scheme.primaryContainer
        else -> scheme.surface
    }
    val textColor = when {
        message.isError -> scheme.onErrorContainer
        fromUser -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (fromUser) 16.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 16.dp,
            ),
            colors = CardDefaults.cardColors(containerColor = background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.toolCalls.isNotEmpty()) {
                    message.toolCalls.forEach { tool ->
                        Text(
                            text = "⚙ ${tool.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                MarkdownText(text = message.text, color = textColor)
                if (message.viaVoice) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = textColor.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "spoken",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String, status: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!status.isNullOrBlank()) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (text.isEmpty()) {
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    MarkdownText(text = text)
                }
            }
        }
    }
}

@Composable
private fun PartialTranscript(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun VoiceBar(
    state: EngineState,
    onMicTap: () -> Unit,
    onVoiceModeChange: (VoiceMode) -> Unit,
    onStop: () -> Unit,
    onStopSpeaking: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .clickable { onMicTap() },
                contentAlignment = Alignment.Center,
            ) {
                VoiceOrb(state = state.voiceState, level = state.micLevel, size = 62.dp)
                Icon(
                    imageVector = if (state.voiceMode == VoiceMode.OFF) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Talk to Hermes",
                    tint = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (state.voiceState) {
                        VoiceState.LISTENING -> "Listening…"
                        VoiceState.THINKING -> state.agentStatus ?: "Thinking…"
                        VoiceState.SPEAKING -> "Speaking"
                        VoiceState.IDLE -> voiceHint(state.voiceMode, state.settings.wakePhrase)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VoiceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.voiceMode == mode,
                            onClick = { onVoiceModeChange(mode) },
                            label = {
                                Text(
                                    text = shortLabel(mode),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(),
                            modifier = Modifier.height(30.dp),
                        )
                    }
                }
            }

            if (state.speaking) {
                IconButton(onClick = onStopSpeaking) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Stop speaking")
                }
            }
            if (state.streaming) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Hermes")
                }
            }
        }
    }
}

private fun shortLabel(mode: VoiceMode): String = when (mode) {
    VoiceMode.OFF -> "Off"
    VoiceMode.PUSH_TO_TALK -> "Tap"
    VoiceMode.HANDS_FREE -> "Hands-free"
    VoiceMode.WAKE_WORD -> "Wake"
}

private fun voiceHint(mode: VoiceMode, wakePhrase: String): String = when (mode) {
    VoiceMode.OFF -> "Tap the orb to talk"
    VoiceMode.PUSH_TO_TALK -> "Tap the orb to talk"
    VoiceMode.HANDS_FREE -> "Hands-free — just speak"
    VoiceMode.WAKE_WORD -> "Say “$wakePhrase”"
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    enabled: Boolean,
    streaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onNewConversation: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            enabled = enabled,
            placeholder = { Text("Message Hermes") },
            maxLines = 5,
            shape = RoundedCornerShape(22.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = { if (streaming) onStop() else onSend() },
            enabled = enabled && (streaming || draft.isNotBlank()),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (streaming) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Icon(
                imageVector = if (streaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                contentDescription = if (streaming) "Stop" else "Send",
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onNewConversation, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Forum, contentDescription = "New conversation")
        }
    }
}
