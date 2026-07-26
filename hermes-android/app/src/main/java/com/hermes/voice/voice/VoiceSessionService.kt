package com.hermes.voice.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hermes.voice.MainActivity
import com.hermes.voice.R
import com.hermes.voice.ServiceLocator
import com.hermes.voice.data.VoiceMode
import com.hermes.voice.engine.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Keeps a voice session alive with the app in the background or the screen off.
 *
 * The service owns no logic of its own — [ServiceLocator]'s engine keeps
 * running either way. What it provides is the foreground-service microphone
 * grant Android requires, plus a notification to stop the session from
 * anywhere.
 */
class VoiceSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat(buildNotification("Starting…"))

        val engine = ServiceLocator.engine(this)
        engine.state
            .map { state -> statusLine(state.voiceMode, state.voiceState) }
            .distinctUntilChanged()
            .onEach { line ->
                ContextCompat.getSystemService(this, android.app.NotificationManager::class.java)
                    ?.notify(NOTIFICATION_ID, buildNotification(line))
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceLocator.engine(this).setVoiceMode(VoiceMode.OFF)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun statusLine(mode: VoiceMode, state: VoiceState): String = when (state) {
        VoiceState.LISTENING -> if (mode == VoiceMode.WAKE_WORD) "Waiting for the wake word" else "Listening"
        VoiceState.THINKING -> "Hermes is thinking"
        VoiceState.SPEAKING -> "Speaking"
        VoiceState.IDLE -> when (mode) {
            VoiceMode.WAKE_WORD -> "Wake word armed"
            VoiceMode.HANDS_FREE -> "Hands-free ready"
            VoiceMode.PUSH_TO_TALK -> "Push to talk"
            VoiceMode.OFF -> "Idle"
        }
    }

    private fun buildNotification(status: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceSessionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hermes_glyph)
            .setContentTitle("Hermes voice")
            .setContentText(status)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "hermes_voice_session"
        private const val NOTIFICATION_ID = 4711
        private const val ACTION_STOP = "com.hermes.voice.action.STOP_VOICE"

        fun start(context: Context) {
            val intent = Intent(context, VoiceSessionService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, VoiceSessionService::class.java)) }
        }
    }
}
