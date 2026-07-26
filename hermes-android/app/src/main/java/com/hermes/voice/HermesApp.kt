package com.hermes.voice

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.hermes.voice.voice.VoiceSessionService

class HermesApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Warm the engine so a tile tap or assistant launch finds it ready.
        ServiceLocator.engine(this)
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            VoiceSessionService.CHANNEL_ID,
            getString(R.string.voice_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.voice_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
