package dev.lumen.launcher.feature.capsule.sources

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import dev.lumen.launcher.feature.capsule.BuiltinSymbol
import dev.lumen.launcher.feature.capsule.CapsuleCard
import dev.lumen.launcher.feature.capsule.CapsuleController
import dev.lumen.launcher.feature.capsule.CapsuleGlyph
import dev.lumen.launcher.feature.capsule.SourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * The Capsule's own sources — everything it can know about the device without asking for a single
 * permission or touching the network (§0.4). Three of them:
 *
 *  * **Ambient clock** at priority 100, so the Capsule is a permanent handle rather than a thing
 *    that only exists when something goes wrong. It is the launcher's identity; a pill that
 *    appeared twice a day would not be.
 *  * **Battery** at priority 300 — a transient card on plug/unplug, a persistent one below 15%.
 *  * **Next alarm** at 850 inside the last minute (§4's "timer/alarm within 60s of firing") and
 *    400 otherwise, read from `AlarmManager.getNextAlarmClock()`, which needs no permission.
 *
 * Media playback (§4, priority 700) is deliberately absent: `MediaSessionManager` requires
 * notification-listener consent, which is §10 territory and a user decision, not a default.
 */
internal class SystemSources(
    private val context: Context,
    private val controller: CapsuleController,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    // Locale- and settings-aware: a 24-hour device shows 18:40, not 6:40.
    private val timeFormat = DateFormat.getTimeFormat(context)
    private val dateFormat = DateFormat.getLongDateFormat(context)

    private val clockKey = "${context.packageName}/${SourceKind.AMBIENT.name}"
    private val batteryKey = "${context.packageName}/${SourceKind.BATTERY.name}"
    private val alarmKey = "${context.packageName}/${SourceKind.ALARM.name}"
    private val timerKey = "${context.packageName}/${SourceKind.TIMER.name}"

    fun start(scope: CoroutineScope) {
        scope.launch { runClock() }
        registerBattery()
        registerAlarm()
        publishAlarm()
    }

    // ---- ambient clock -------------------------------------------------------------------------

    /**
     * Ticks on the minute boundary rather than every second: one wakeup a minute, and the displayed
     * time is never stale at the moment it changes.
     */
    private suspend fun runClock() {
        while (currentCoroutineContext().isActive) {
            publishClock()
            publishAlarm() // Re-evaluates the 60s urgency window without its own timer.
            refreshBattery()
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }

    private fun publishClock() {
        val now = Date()
        controller.push(
            CapsuleCard(
                id = clockKey,
                sourcePackage = context.packageName,
                kind = SourceKind.AMBIENT,
                priority = SourceKind.AMBIENT.defaultPriority,
                collapsedText = timeFormat.format(now),
                title = timeFormat.format(now),
                subtitle = dateFormat.format(now),
                glyph = CapsuleGlyph.Builtin(BuiltinSymbol.CLOCK),
                dismissible = false,
                verified = true,
            ),
        )
    }

    // ---- battery -------------------------------------------------------------------------------

    private var wasCharging: Boolean? = null
    private var lastPercent: Int = -1

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            publishBattery((level * 100) / scale, charging)
        }
    }

    private fun registerBattery() {
        // ACTION_BATTERY_CHANGED is sticky and permission-free; registering returns the last value
        // immediately, so the first card appears without waiting for a state change.
        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * The low-battery card carries a progress value, which puts it under §4's 30-second
     * abandoned-progress expiry. `ACTION_BATTERY_CHANGED` can be minutes apart on an idle device,
     * so without this the warning would blink out and back roughly every half minute. Re-stating
     * the same value on the clock tick keeps `updatedAt` fresh without a timer of its own.
     */
    private fun refreshBattery() {
        val charging = wasCharging ?: return
        if (lastPercent >= 0) publishBattery(lastPercent, charging)
    }

    private fun publishBattery(percent: Int, charging: Boolean) {
        val first = wasCharging == null
        val stateChanged = !first && wasCharging != charging
        wasCharging = charging
        lastPercent = percent

        val low = !charging && percent <= LOW_BATTERY_PERCENT
        val expiry = System.currentTimeMillis() + PLUG_CARD_MS

        when {
            // §4 lists "charging state change" as the event, not "is charging" as a condition, so
            // plugging in shows for six seconds and then gets out of the way.
            charging && (stateChanged || first) ->
                controller.push(batteryCard(percent, charging = true, expiresAt = expiry))

            low -> controller.push(batteryCard(percent, charging = false, expiresAt = 0L))

            else -> controller.clearKey(batteryKey)
        }
    }

    private fun batteryCard(percent: Int, charging: Boolean, expiresAt: Long) = CapsuleCard(
        id = batteryKey,
        sourcePackage = context.packageName,
        kind = SourceKind.BATTERY,
        priority = SourceKind.BATTERY.defaultPriority,
        collapsedText = "$percent%",
        title = if (charging) "Charging" else "Battery low",
        subtitle = if (charging) "$percent% and climbing" else "$percent% remaining",
        progress = percent / 100f,
        glyph = CapsuleGlyph.Builtin(if (charging) BuiltinSymbol.CHARGING else BuiltinSymbol.BATTERY),
        dismissible = true,
        expiresAt = expiresAt,
        verified = true,
    )

    // ---- next alarm ----------------------------------------------------------------------------

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = publishAlarm()
    }

    private fun registerAlarm() {
        ContextCompat.registerReceiver(
            context,
            alarmReceiver,
            IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * Publishes under two different kinds depending on urgency, because §4's priority table treats
     * "alarm within 60s" (850) and "running timer" (400) as different things. Whichever kind is not
     * in use is withdrawn, so the two never coexist as duplicate cards.
     */
    fun publishAlarm() {
        val next = runCatching { alarmManager?.nextAlarmClock }.getOrNull()
        val remaining = next?.let { it.triggerTime - System.currentTimeMillis() } ?: -1L
        if (next == null || remaining <= 0L) {
            controller.clearKey(alarmKey)
            controller.clearKey(timerKey)
            return
        }

        val imminent = remaining <= IMMINENT_MS
        val kind = if (imminent) SourceKind.ALARM else SourceKind.TIMER
        controller.clearKey(if (imminent) timerKey else alarmKey)
        controller.push(
            CapsuleCard(
                id = if (imminent) alarmKey else timerKey,
                sourcePackage = context.packageName,
                kind = kind,
                priority = kind.defaultPriority,
                collapsedText = timeFormat.format(Date(next.triggerTime)),
                title = if (imminent) "Alarm" else "Next alarm",
                subtitle = describeAlarm(next.triggerTime, remaining),
                glyph = CapsuleGlyph.Builtin(BuiltinSymbol.ALARM),
                dismissible = !imminent,
                expiresAt = next.triggerTime + ALARM_LINGER_MS,
                verified = true,
            ),
        )
    }

    private fun describeAlarm(triggerAt: Long, remaining: Long): String {
        val minutes = remaining / 60_000L
        val whenText = timeFormat.format(Date(triggerAt))
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
        return when {
            minutes < 1L -> "Ringing in under a minute"
            minutes < 60L -> "In $minutes min · $whenText"
            sameDay -> "Today at $whenText"
            else -> "Tomorrow at $whenText"
        }
    }

    private companion object {
        const val LOW_BATTERY_PERCENT = 15
        const val PLUG_CARD_MS = 6_000L
        const val IMMINENT_MS = 60_000L
        const val ALARM_LINGER_MS = 60_000L
    }
}
