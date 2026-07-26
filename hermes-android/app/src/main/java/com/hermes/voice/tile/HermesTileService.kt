package com.hermes.voice.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hermes.voice.MainActivity
import com.hermes.voice.ServiceLocator
import com.hermes.voice.data.VoiceMode

/**
 * Quick Settings tile: pull down, tap, talk.
 *
 * The tile opens the app rather than starting the microphone service directly —
 * Android 14+ refuses a background start of a microphone foreground service, so
 * routing through the Activity is what actually works on a modern device.
 */
class HermesTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_START_VOICE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val active = ServiceLocator.engine(this).state.value.voiceMode != VoiceMode.OFF
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) "Listening" else "Tap to talk"
        }
        tile.updateTile()
    }
}
