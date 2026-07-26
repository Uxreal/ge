package com.hermes.voice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hermes.voice.data.VoiceMode
import com.hermes.voice.engine.HermesEngine
import com.hermes.voice.ui.chat.ChatScreen
import com.hermes.voice.ui.conversations.ConversationsScreen
import com.hermes.voice.ui.macros.MacrosScreen
import com.hermes.voice.ui.settings.SettingsScreen
import com.hermes.voice.ui.theme.HermesTheme

class MainActivity : ComponentActivity() {

    private lateinit var engine: HermesEngine

    /** Set when the launch intent asked for the mic (tile tap or assistant gesture). */
    private var pendingVoiceStart = false

    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingVoiceStart) {
            pendingVoiceStart = false
            engine.toggleListening()
        }
    }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        engine = ServiceLocator.engine(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleLaunchIntent(intent)

        setContent {
            HermesTheme {
                val state by engine.state.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                var appliedDefaultMode by remember { mutableStateOf(false) }

                // Apply the configured launch mode once settings have loaded.
                LaunchedEffect(state.settings.defaultVoiceMode, state.settings.isConfigured) {
                    if (!appliedDefaultMode && state.settings.isConfigured) {
                        appliedDefaultMode = true
                        val mode = state.settings.defaultVoiceMode
                        if (mode != VoiceMode.OFF && engine.hasMicPermission()) {
                            engine.setVoiceMode(mode)
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "chat") {
                    composable("chat") {
                        ChatScreen(
                            state = state,
                            onSend = { engine.send(it) },
                            onMicTap = ::onMicTap,
                            onVoiceModeChange = ::onVoiceModeChange,
                            onStop = { engine.cancelTurn() },
                            onStopSpeaking = { engine.stopSpeaking() },
                            onNewConversation = { engine.newConversation() },
                            onOpenConversations = { navController.navigate("conversations") },
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenMacros = { navController.navigate("macros") },
                            onDismissNotice = { engine.dismissNotice() },
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            state = state,
                            onUpdate = { transform -> engine.updateSettings(transform) },
                            onTestConnection = { engine.checkConnection() },
                            onPreviewVoice = { engine.previewVoice() },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable("conversations") {
                        ConversationsScreen(
                            conversations = state.conversations,
                            activeId = state.conversation?.id,
                            onSelect = {
                                engine.selectConversation(it)
                                navController.popBackStack()
                            },
                            onDelete = { engine.deleteConversation(it) },
                            onNew = {
                                engine.newConversation()
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable("macros") {
                        MacrosScreen(
                            macros = state.macros,
                            onSave = { engine.saveMacros(it) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    /**
     * Opening via the Quick Settings tile, the assistant gesture, or
     * VOICE_COMMAND should go straight to listening — that is the whole point
     * of those entry points.
     */
    private fun handleLaunchIntent(intent: Intent?) {
        val fromTile = intent?.getBooleanExtra(EXTRA_START_VOICE, false) == true
        val fromAssistant = intent?.action == Intent.ACTION_ASSIST ||
            intent?.action == Intent.ACTION_VOICE_COMMAND
        if (!fromTile && !fromAssistant) return
        if (fromAssistant && !engine.state.value.settings.listenOnAssistLaunch) return

        pendingVoiceStart = true
        if (engine.hasMicPermission()) {
            pendingVoiceStart = false
            engine.toggleListening()
        } else {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun onMicTap() {
        if (engine.hasMicPermission()) {
            engine.toggleListening()
        } else {
            pendingVoiceStart = true
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun onVoiceModeChange(mode: VoiceMode) {
        if (mode == VoiceMode.OFF || engine.hasMicPermission()) {
            engine.setVoiceMode(mode)
        } else {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    companion object {
        const val EXTRA_START_VOICE = "com.hermes.voice.extra.START_VOICE"
    }
}
