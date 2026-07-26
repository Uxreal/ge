package com.hermes.voice

import android.content.Context
import com.hermes.voice.data.ConversationStore
import com.hermes.voice.data.SettingsRepository
import com.hermes.voice.engine.HermesEngine
import com.hermes.voice.net.TransportFactory
import com.hermes.voice.voice.SttEngine
import com.hermes.voice.voice.TtsEngine
import com.hermes.voice.voice.VoiceSessionService

/**
 * Hand-rolled DI. The app has exactly one graph and one engine, so a locator
 * beats pulling in a framework — and it keeps the engine alive across the
 * Activity and the foreground service.
 */
object ServiceLocator {

    @Volatile private var engineInstance: HermesEngine? = null

    fun engine(context: Context): HermesEngine {
        engineInstance?.let { return it }
        return synchronized(this) {
            engineInstance ?: build(context.applicationContext).also { engineInstance = it }
        }
    }

    private fun build(app: Context): HermesEngine {
        val engine = HermesEngine(
            appContext = app,
            settingsRepository = SettingsRepository(app),
            store = ConversationStore(app),
            transports = TransportFactory(),
            stt = SttEngine(app),
            tts = TtsEngine(app),
        )
        engine.onVoiceSessionChanged = { active ->
            if (active) VoiceSessionService.start(app) else VoiceSessionService.stop(app)
        }
        engine.bootstrap()
        return engine
    }
}
