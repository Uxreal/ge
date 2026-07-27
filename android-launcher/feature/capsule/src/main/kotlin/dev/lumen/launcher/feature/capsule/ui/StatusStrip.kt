package dev.lumen.launcher.feature.capsule.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Lumen's replacement for the system status bar's content (D30): time on the left, battery on the
 * right, in the launcher's own type, vertically centred on the pill band so the three read as one
 * composed row — [ time ... (camera pill) ... battery ].
 *
 * Exists because a launcher cannot restyle the system bar, and the system bar's icons sitting
 * beside the island read as two designs fighting. The bar itself is hidden by the shell when this
 * strip is active; a swipe from the top edge still summons it (and the shade) transiently.
 */
@Composable
internal fun StatusStrip(
    topPx: Int,
    bandHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val typography = LocalTypography.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

    var time by remember { mutableStateOf(timeFormat.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            time = timeFormat.format(Date())
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    var percent by remember { mutableStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) percent = (level * 100) / scale
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        // Sticky: registering returns the last broadcast, so the first frame has a value.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }

    // White with a soft shadow: the strip sits on raw wallpaper, which can be any colour.
    val style = typography.capsuleGlance.copy(
        color = STRIP_TEXT,
        shadow = Shadow(color = STRIP_SHADOW, blurRadius = 7f, offset = Offset(0f, 1f)),
    )

    Row(
        modifier = modifier
            .offset { IntOffset(0, topPx) }
            .height(bandHeight)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // D43: the strip is Lumen's status bar, so its regions answer like one — the time opens
        // the clock, the battery opens the battery screen. Both fail silently on odd OEM builds;
        // a status readout must never throw.
        BasicText(
            text = time,
            style = style,
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    contentDescription = "Open clock"
                }
                .pointerInput(context) {
                    detectTapGestures {
                        runCatching {
                            context.startActivity(
                                Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    contentDescription = "Open battery settings"
                }
                .pointerInput(context) {
                    detectTapGestures {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                },
        ) {
            if (percent >= 0) {
                BasicText(text = "$percent%", style = style)
            }
            val symbol = if (charging) BuiltinSymbol.CHARGING else BuiltinSymbol.BATTERY
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .drawWithCache {
                        onDrawBehind { drawBuiltinSymbol(symbol, STRIP_TEXT, size.minDimension) }
                    },
            )
        }
    }
}

private val STRIP_TEXT = Color(0xF2FFFFFF)
private val STRIP_SHADOW = Color(0x99000000)
