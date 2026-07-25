package dev.lumen.launcher.data.apps

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import dev.lumen.launcher.data.UsageTracker
import dev.lumen.launcher.data.model.AppKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/** Persisted launch history. Kept deliberately small: two maps keyed by [AppKey.flat]. */
@Serializable
internal data class UsageRecords(
    val counts: Map<String, Int> = emptyMap(),
    val lastLaunched: Map<String, Long> = emptyMap(),
    /** Seeding from `UsageStatsManager` happens once, so the user's own history always wins. */
    val seeded: Boolean = false,
)

internal object UsageRecordsSerializer : Serializer<UsageRecords> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue = UsageRecords()

    override suspend fun readFrom(input: InputStream): UsageRecords = try {
        json.decodeFromString(UsageRecords.serializer(), input.readBytes().decodeToString())
    } catch (error: SerializationException) {
        throw CorruptionException("Unreadable usage history", error)
    }

    override suspend fun writeTo(t: UsageRecords, output: OutputStream) {
        output.write(json.encodeToString(UsageRecords.serializer(), t).encodeToByteArray())
    }
}

/**
 * Launch counts and recency.
 *
 * Predictions are what make the drawer feel like it knows you, so raw frequency is not enough: a
 * game played daily last month should not outrank the messenger opened twice this morning. The score
 * is therefore frequency multiplied by an exponential decay on days since last use.
 *
 * On a fresh install there is no history at all, so if the user has already granted usage access the
 * first load seeds itself from the system's own 30-day statistics.
 */
class DefaultUsageTracker(
    context: Context,
    private val scope: CoroutineScope,
) : UsageTracker {

    private val appContext: Context = context.applicationContext

    private val store: DataStore<UsageRecords> = DataStoreFactory.create(
        serializer = UsageRecordsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { UsageRecords() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = {
            File(appContext.filesDir, "lumen").apply { mkdirs() }
                .resolve(FILE_NAME)
        },
    )

    private val _launchCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val launchCounts: StateFlow<Map<String, Int>> = _launchCounts.asStateFlow()

    private val _lastLaunched = MutableStateFlow<Map<String, Long>>(emptyMap())
    override val lastLaunched: StateFlow<Map<String, Long>> = _lastLaunched.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val records = runCatching { store.data.first() }.getOrNull() ?: UsageRecords()
            publish(records)
            if (!records.seeded) seedFromSystem()
        }
    }

    // ------------------------------------------------------------------ reads

    override fun predictions(limit: Int): List<AppKey> {
        if (limit <= 0) return emptyList()
        val counts = _launchCounts.value
        if (counts.isEmpty()) return emptyList()
        val recency = _lastLaunched.value
        val now = System.currentTimeMillis()
        return counts.entries
            .asSequence()
            .map { (flat, count) -> flat to score(count, recency[flat], now) }
            .filter { it.second > 0f }
            .sortedWith(compareByDescending<Pair<String, Float>> { it.second }.thenBy { it.first })
            .mapNotNull { AppKey.parse(it.first) }
            .take(limit)
            .toList()
    }

    /**
     * Frequency times a half-life decay. [HALF_LIFE_DAYS] is short on purpose — predictions should
     * follow this week's habits, not this year's.
     */
    private fun score(count: Int, lastUsed: Long?, now: Long): Float {
        if (count <= 0) return 0f
        val days = lastUsed
            ?.takeIf { it > 0L }
            ?.let { (now - it).coerceAtLeast(0L).toFloat() / DAY_MILLIS }
            ?: STALE_DAYS
        return count * 0.5f.pow(days / HALF_LIFE_DAYS)
    }

    // ------------------------------------------------------------------ writes

    override fun record(key: AppKey) {
        val flat = key.flat
        val now = System.currentTimeMillis()
        // Update in memory first: the drawer may re-sort before the write lands.
        _launchCounts.value = _launchCounts.value.toMutableMap().apply {
            this[flat] = (this[flat] ?: 0) + 1
        }
        _lastLaunched.value = _lastLaunched.value + (flat to now)
        scope.launch(Dispatchers.IO) {
            runCatching {
                store.updateData { current ->
                    current.copy(
                        counts = current.counts + (flat to ((current.counts[flat] ?: 0) + 1)),
                        lastLaunched = current.lastLaunched + (flat to now),
                        seeded = true,
                    )
                }
            }
        }
    }

    override fun clear() {
        _launchCounts.value = emptyMap()
        _lastLaunched.value = emptyMap()
        scope.launch(Dispatchers.IO) {
            // `seeded` stays true: re-importing system stats would undo the user's reset.
            runCatching { store.updateData { UsageRecords(seeded = true) } }
        }
    }

    private fun publish(records: UsageRecords) {
        _launchCounts.value = records.counts
        _lastLaunched.value = records.lastLaunched
    }

    // ------------------------------------------------------------------ seeding

    /** True when the user has granted "usage access" to Lumen in system settings. */
    fun hasUsageAccess(): Boolean = runCatching {
        val ops = appContext.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                appContext.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    private suspend fun seedFromSystem() {
        if (!hasUsageAccess()) return
        val samples = runCatching { collectSystemUsage() }.getOrNull().orEmpty()
        // Even with nothing to import, mark the seed done so this does not retry on every launch.
        val merged = runCatching {
            store.updateData { current ->
                val counts = current.counts.toMutableMap()
                val recency = current.lastLaunched.toMutableMap()
                for ((flat, sample) in samples) {
                    if (flat !in counts) counts[flat] = sample.count
                    if (flat !in recency) recency[flat] = sample.lastUsed
                }
                current.copy(counts = counts, lastLaunched = recency, seeded = true)
            }
        }.getOrNull() ?: return
        publish(merged)
    }

    private class SystemSample(val count: Int, val lastUsed: Long)

    /**
     * `UsageStats` is per package, so each one is attributed to that package's main activity in the
     * personal profile — the same key [record] would write.
     */
    private fun collectSystemUsage(): Map<String, SystemSample> {
        val usage = appContext.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val launcherApps = appContext.getSystemService(LauncherApps::class.java) ?: return emptyMap()
        val user = Process.myUserHandle()
        val serial = LauncherProfiles.serialFor(appContext, user)
        val now = System.currentTimeMillis()
        val stats = runCatching {
            usage.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - TimeUnit.DAYS.toMillis(SEED_WINDOW_DAYS),
                now,
            )
        }.getOrNull().orEmpty()
        if (stats.isEmpty()) return emptyMap()

        val byPackage = HashMap<String, SystemSample>()
        for (stat in stats) {
            if (stat.packageName == appContext.packageName) continue
            val launches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                runCatching { stat.appLaunchCount }.getOrDefault(0)
            } else {
                0
            }
            // Older platforms report no launch count, so foreground minutes stand in for it.
            val derived = if (launches > 0) {
                launches
            } else {
                (stat.totalTimeInForeground / FOREGROUND_MILLIS_PER_LAUNCH).toInt()
            }
            if (derived <= 0) continue
            val previous = byPackage[stat.packageName]
            byPackage[stat.packageName] = SystemSample(
                count = (previous?.count ?: 0) + derived,
                lastUsed = maxOf(previous?.lastUsed ?: 0L, stat.lastTimeUsed),
            )
        }

        val result = HashMap<String, SystemSample>(byPackage.size)
        for ((packageName, sample) in byPackage) {
            val activity = runCatching { launcherApps.getActivityList(packageName, user) }
                .getOrNull()
                ?.firstOrNull()
                ?: continue
            val key = AppKey(packageName, activity.componentName.className, serial)
            result[key.flat] = sample
        }
        return result
    }

    private companion object {
        const val FILE_NAME = "usage.json"
        const val DAY_MILLIS = 24f * 60f * 60f * 1000f
        const val HALF_LIFE_DAYS = 6f
        const val STALE_DAYS = 45f
        const val SEED_WINDOW_DAYS = 30L
        const val FOREGROUND_MILLIS_PER_LAUNCH = 5L * 60L * 1000L
    }
}
