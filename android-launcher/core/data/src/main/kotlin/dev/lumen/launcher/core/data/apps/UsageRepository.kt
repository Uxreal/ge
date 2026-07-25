package dev.lumen.launcher.core.data.apps

import androidx.datastore.core.DataStore
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.data.proto.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * Launch statistics feeding the drawer's suggested-apps row (§6). Ranking is recency-weighted
 * frequency — an app used daily this week beats one used heavily last month — with a half-life of
 * about four days.
 */
@Singleton
class UsageRepository @Inject constructor(
    private val store: DataStore<UsageStats>,
    private val scope: CoroutineScope,
) {
    val stats: StateFlow<UsageStats> = store.data
        .catch { emit(UsageStats.getDefaultInstance()) }
        .stateIn(scope, SharingStarted.Eagerly, UsageStats.getDefaultInstance())

    fun recordLaunch(key: AppKey) {
        val now = System.currentTimeMillis()
        scope.launch {
            runCatching {
                store.updateData { current ->
                    current.toBuilder()
                        .putLaunchCounts(key.flat, (current.launchCountsMap[key.flat] ?: 0L) + 1L)
                        .putLastLaunch(key.flat, now)
                        .build()
                }
            }
        }
    }

    /** Highest-scoring flat keys, most relevant first. */
    fun suggestions(limit: Int, now: Long = System.currentTimeMillis()): List<String> {
        val snapshot = stats.value
        return snapshot.launchCountsMap.entries
            .map { (flat, count) ->
                val last = snapshot.lastLaunchMap[flat] ?: 0L
                val ageDays = ((now - last).coerceAtLeast(0L)) / DAY_MS.toFloat()
                flat to count * exp(-ageDays / HALF_LIFE_DAYS)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val HALF_LIFE_DAYS = 4f
    }
}
