package com.hermes.voice.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.hermes.voice.data.HermesSettings
import com.hermes.voice.data.TransportKind
import com.hermes.voice.data.VoiceMode
import com.hermes.voice.engine.ConnectionHealth
import com.hermes.voice.engine.EngineState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: EngineState,
    onUpdate: ((HermesSettings) -> HermesSettings) -> Unit,
    onTestConnection: () -> Unit,
    onPreviewVoice: () -> Unit,
    onBack: () -> Unit,
) {
    val settings = state.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard("Connection") {
                TextSetting(
                    label = "Base URL",
                    value = settings.baseUrl,
                    placeholder = "http://192.168.1.20:8080",
                    onChange = { value -> onUpdate { it.copy(baseUrl = value.trim()) } },
                )

                Text("Transport", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransportKind.entries.forEachIndexed { index, kind ->
                        SegmentedButton(
                            selected = settings.transport == kind,
                            onClick = { onUpdate { it.copy(transport = kind) } },
                            shape = SegmentedButtonDefaults.itemShape(index, TransportKind.entries.size),
                        ) {
                            Text(
                                text = when (kind) {
                                    TransportKind.OPENAI_COMPAT -> "OpenAI"
                                    TransportKind.REST_JSON -> "REST"
                                    TransportKind.WEBSOCKET -> "WebSocket"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Text(
                    text = settings.transport.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (settings.transport != TransportKind.OPENAI_COMPAT) {
                    TextSetting(
                        label = "Endpoint path",
                        value = settings.chatPath,
                        placeholder = "/chat",
                        onChange = { value -> onUpdate { it.copy(chatPath = value.trim()) } },
                    )
                }

                TextSetting(
                    label = "API key / token",
                    value = settings.apiKey,
                    placeholder = "optional",
                    secret = true,
                    onChange = { value -> onUpdate { it.copy(apiKey = value.trim()) } },
                )

                TextSetting(
                    label = "Extra headers",
                    value = settings.extraHeaders,
                    placeholder = "X-Client: phone\nX-Room: office",
                    singleLine = false,
                    onChange = { value -> onUpdate { it.copy(extraHeaders = value) } },
                )

                NumberSetting(
                    label = "Request timeout (seconds)",
                    value = settings.requestTimeoutSec,
                    onChange = { value -> onUpdate { it.copy(requestTimeoutSec = value.coerceIn(10, 900)) } },
                )

                if (settings.isConfigured) {
                    Text(
                        text = "→ ${settings.endpoint()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(onClick = onTestConnection) { Text("Test connection") }
                    Text(
                        text = when (state.connection.health) {
                            ConnectionHealth.OK -> "✓ ${state.connection.message} (${state.connection.latencyMs}ms)"
                            ConnectionHealth.FAILED -> "✕ ${state.connection.message}"
                            ConnectionHealth.CHECKING -> "Checking…"
                            ConnectionHealth.UNKNOWN -> "Not checked"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (state.connection.health) {
                            ConnectionHealth.OK -> MaterialTheme.colorScheme.tertiary
                            ConnectionHealth.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            SectionCard("Agent") {
                if (state.connection.models.isNotEmpty()) {
                    DropdownSetting(
                        label = "Model",
                        value = settings.model.ifBlank { "(server default)" },
                        options = listOf("(server default)") + state.connection.models,
                        onSelect = { picked ->
                            onUpdate { it.copy(model = if (picked.startsWith("(")) "" else picked) }
                        },
                    )
                } else {
                    TextSetting(
                        label = "Model",
                        value = settings.model,
                        placeholder = "leave blank for the server default",
                        onChange = { value -> onUpdate { it.copy(model = value.trim()) } },
                    )
                }

                TextSetting(
                    label = "System prompt",
                    value = settings.systemPrompt,
                    placeholder = "You are Hermes, answering out loud. Keep replies short.",
                    singleLine = false,
                    onChange = { value -> onUpdate { it.copy(systemPrompt = value) } },
                )

                SliderSetting(
                    label = "Temperature",
                    value = settings.temperature,
                    range = 0f..2f,
                    display = { "%.2f".format(it) },
                    onChange = { value -> onUpdate { it.copy(temperature = value) } },
                )

                NumberSetting(
                    label = "Max tokens (0 = server default)",
                    value = settings.maxTokens,
                    onChange = { value -> onUpdate { it.copy(maxTokens = value.coerceIn(0, 32_000)) } },
                )

                SwitchSetting(
                    label = "Send conversation history",
                    description = "Off if your agent keeps its own session state.",
                    checked = settings.sendHistory,
                    onChange = { value -> onUpdate { it.copy(sendHistory = value) } },
                )

                if (settings.sendHistory) {
                    NumberSetting(
                        label = "History turns to send",
                        value = settings.historyDepth,
                        onChange = { value -> onUpdate { it.copy(historyDepth = value.coerceIn(0, 100)) } },
                    )
                }
            }

            SectionCard("Voice") {
                SwitchSetting(
                    label = "Speak replies aloud",
                    description = "Starts speaking mid-stream, sentence by sentence.",
                    checked = settings.autoSpeakReplies,
                    onChange = { value -> onUpdate { it.copy(autoSpeakReplies = value) } },
                )

                SwitchSetting(
                    label = "Keep listening after a reply",
                    description = "Hands-free mode reopens the mic once Hermes stops talking.",
                    checked = settings.handsFreeContinues,
                    onChange = { value -> onUpdate { it.copy(handsFreeContinues = value) } },
                )

                TextSetting(
                    label = "Wake phrase",
                    value = settings.wakePhrase,
                    placeholder = "hey hermes",
                    onChange = { value -> onUpdate { it.copy(wakePhrase = value) } },
                )

                DropdownSetting(
                    label = "Default voice mode on launch",
                    value = settings.defaultVoiceMode.label,
                    options = VoiceMode.entries.map { it.label },
                    onSelect = { picked ->
                        val mode = VoiceMode.entries.firstOrNull { it.label == picked } ?: VoiceMode.OFF
                        onUpdate { it.copy(defaultVoiceMode = mode) }
                    },
                )

                SwitchSetting(
                    label = "Listen immediately when opened as assistant",
                    description = "Applies when Hermes is set as the phone's default digital assistant.",
                    checked = settings.listenOnAssistLaunch,
                    onChange = { value -> onUpdate { it.copy(listenOnAssistLaunch = value) } },
                )

                SliderSetting(
                    label = "Speech rate",
                    value = settings.speechRate,
                    range = 0.5f..2.0f,
                    display = { "%.2f×".format(it) },
                    onChange = { value -> onUpdate { it.copy(speechRate = value) } },
                )

                SliderSetting(
                    label = "Pitch",
                    value = settings.speechPitch,
                    range = 0.5f..2.0f,
                    display = { "%.2f".format(it) },
                    onChange = { value -> onUpdate { it.copy(speechPitch = value) } },
                )

                if (state.ttsVoices.isNotEmpty()) {
                    DropdownSetting(
                        label = "Voice",
                        value = settings.ttsVoice.ifBlank { "(system default)" },
                        options = listOf("(system default)") + state.ttsVoices,
                        onSelect = { picked ->
                            onUpdate { it.copy(ttsVoice = if (picked.startsWith("(")) "" else picked) }
                        },
                    )
                }

                TextSetting(
                    label = "Recognition language",
                    value = settings.sttLanguage,
                    placeholder = "blank = device default, e.g. en-US",
                    onChange = { value -> onUpdate { it.copy(sttLanguage = value.trim()) } },
                )

                NumberSetting(
                    label = "End-of-speech pause (ms)",
                    value = settings.silenceTimeoutMs,
                    onChange = { value -> onUpdate { it.copy(silenceTimeoutMs = value.coerceIn(400, 6000)) } },
                )

                OutlinedButton(onClick = onPreviewVoice) { Text("Preview voice") }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun TextSetting(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    secret: Boolean = false,
    singleLine: Boolean = true,
) {
    var local by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != local) local = value }

    OutlinedTextField(
        value = local,
        onValueChange = {
            local = it
            onChange(it)
        },
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else {
            { Text(placeholder, style = MaterialTheme.typography.bodyMedium) }
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberSetting(label: String, value: Int, onChange: (Int) -> Unit) {
    var local by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (value.toString() != local) local = value.toString() }

    OutlinedTextField(
        value = local,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(6)
            local = digits
            digits.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchSetting(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                display(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(range),
            valueRange = range,
            steps = ((range.endInclusive - range.start) / 0.05f).roundToInt() - 1,
            onValueChange = onChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
