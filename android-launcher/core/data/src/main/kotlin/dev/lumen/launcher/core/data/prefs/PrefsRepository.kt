package dev.lumen.launcher.core.data.prefs

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.proto.LumenPrefs
import dev.lumen.launcher.core.data.proto.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Corrupt prefs fall back to defaults; a launcher must never crash-loop on a bad file. */
object PrefsSerializer : Serializer<LumenPrefs> {
    override val defaultValue: LumenPrefs = LumenPrefs.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LumenPrefs = try {
        LumenPrefs.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
        throw CorruptionException("prefs corrupted", e)
    }

    override suspend fun writeTo(t: LumenPrefs, output: OutputStream) = t.writeTo(output)
}

object UsageSerializer : Serializer<UsageStats> {
    override val defaultValue: UsageStats = UsageStats.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UsageStats = try {
        UsageStats.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
        throw CorruptionException("usage corrupted", e)
    }

    override suspend fun writeTo(t: UsageStats, output: OutputStream) = t.writeTo(output)
}

/**
 * The proto's zero-values mean "unset", resolved here into the real defaults from the §3/§14
 * decisions. UI code only ever sees [Snapshot].
 */
data class PrefsSnapshot(
    val homeModel: HomeModel?,        // null until onboarding chooses
    val columns: Int,
    val themeMode: String,
    val motionSpeed: Float,
    val reduceMotion: Boolean,
    val hapticIntensity: String,
    val labelLines: Int,
    val hideLabels: Boolean,
    val parallax: Float,
    val onboardingDone: Boolean,
    val smoothness: Float,
    val workspaceSeeded: Boolean,
    val capsuleEnabled: Boolean,
    /** D30: false (default) hides the system bar on the home screen; Lumen draws time/battery. */
    val showStatusBar: Boolean,
    /** D38: the dock, as AppKey.flat strings in order. */
    val dockKeys: List<String>,
    /** D41: apps hidden from every drawer view, revealed only behind the biometric shelf. */
    val hiddenKeys: List<String>,
    val dockSeeded: Boolean,
    /** §4.1: packages that have ever pushed a Capsule card, so settings can list them. */
    val capsuleSeenPackages: List<String>,
    val capsuleBlockedPackages: List<String>,
)

@Singleton
class PrefsRepository @Inject constructor(
    private val store: DataStore<LumenPrefs>,
    scope: CoroutineScope,
) {
    val prefs: StateFlow<PrefsSnapshot> = store.data
        .catch { emit(LumenPrefs.getDefaultInstance()) }
        .map { it.toSnapshot() }
        .stateIn(scope, SharingStarted.Eagerly, LumenPrefs.getDefaultInstance().toSnapshot())

    private val writeScope = scope

    fun update(transform: (LumenPrefs.Builder) -> Unit) {
        writeScope.launch {
            runCatching {
                store.updateData { current -> current.toBuilder().apply(transform).build() }
            }
        }
    }

    fun setHomeModel(model: HomeModel) = update { it.homeModel = model.name }
    fun setColumns(columns: Int) = update { it.columns = columns.coerceIn(4, 6) }
    fun setThemeMode(mode: String) = update { it.themeMode = mode }
    fun setMotionSpeed(speed: Float) = update { it.motionSpeed = speed.coerceIn(0.5f, 1.5f) }
    fun setReduceMotion(reduce: Boolean) = update { it.reduceMotion = reduce }
    fun setHapticIntensity(intensity: String) = update { it.hapticIntensity = intensity }
    fun setLabelLines(lines: Int) = update { it.labelLines = lines.coerceIn(1, 2) }
    fun setHideLabels(hide: Boolean) = update { it.hideLabels = hide }
    fun setParallax(value: Float) = update {
        it.parallax = value.coerceIn(0f, 1.5f)
        it.parallaxSet = true
    }
    fun setOnboardingDone() = update { it.onboardingDone = true }
    fun setSmoothness(n: Float) = update { it.smoothness = n.coerceIn(2f, 6f) }
    fun setWorkspaceSeeded() = update { it.workspaceSeeded = true }

    fun setCapsuleEnabled(enabled: Boolean) = update { it.capsuleOff = !enabled }

    fun setShowStatusBar(show: Boolean) = update { it.showStatusBar = show }

    fun addDockKey(flat: String) = update { builder ->
        if (flat in builder.dockKeysList) return@update
        val kept = (builder.dockKeysList + flat).takeLast(MAX_DOCK)
        builder.clearDockKeys().addAllDockKeys(kept)
    }

    fun removeDockKey(flat: String) = update { builder ->
        val kept = builder.dockKeysList.filterNot { it == flat }
        builder.clearDockKeys().addAllDockKeys(kept)
    }

    fun hideApp(flat: String) = update { builder ->
        if (flat !in builder.hiddenKeysList) builder.addHiddenKeys(flat)
        // A hidden app has no business staying on the dock.
        val kept = builder.dockKeysList.filterNot { it == flat }
        builder.clearDockKeys().addAllDockKeys(kept)
    }

    fun unhideApp(flat: String) = update { builder ->
        val kept = builder.hiddenKeysList.filterNot { it == flat }
        builder.clearHiddenKeys().addAllHiddenKeys(kept)
    }

    fun setDockSeeded() = update { it.dockSeeded = true }

    /** Capped so a package that renames itself in a loop cannot grow the prefs file without end. */
    fun rememberCapsulePackage(pkg: String) = update { builder ->
        if (pkg in builder.capsuleSeenPackagesList) return@update
        val kept = (builder.capsuleSeenPackagesList + pkg).takeLast(MAX_SEEN_PACKAGES)
        builder.clearCapsuleSeenPackages().addAllCapsuleSeenPackages(kept)
    }

    fun blockCapsulePackage(pkg: String) = update { builder ->
        if (pkg !in builder.capsuleBlockedPackagesList) builder.addCapsuleBlockedPackages(pkg)
    }

    fun unblockCapsulePackage(pkg: String) = update { builder ->
        val kept = builder.capsuleBlockedPackagesList.filterNot { it == pkg }
        builder.clearCapsuleBlockedPackages().addAllCapsuleBlockedPackages(kept)
    }

    private companion object {
        const val MAX_SEEN_PACKAGES = 64
        const val MAX_DOCK = 5
    }
}

private fun LumenPrefs.toSnapshot() = PrefsSnapshot(
    homeModel = homeModel.takeIf { it.isNotEmpty() }?.let { runCatching { HomeModel.valueOf(it) }.getOrNull() },
    columns = if (columns in 4..6) columns else 4,
    themeMode = themeMode.ifEmpty { "AUTO" },
    motionSpeed = if (motionSpeed in 0.5f..1.5f) motionSpeed else 1f,
    reduceMotion = reduceMotion,
    hapticIntensity = hapticIntensity.ifEmpty { "STANDARD" },
    labelLines = if (labelLines in 1..2) labelLines else 1,
    hideLabels = hideLabels,
    parallax = if (parallaxSet) parallax.coerceIn(0f, 1.5f) else 1f,
    onboardingDone = onboardingDone,
    smoothness = if (smoothness in 2f..6f) smoothness else 3.0f,
    workspaceSeeded = workspaceSeeded,
    capsuleEnabled = !capsuleOff,
    showStatusBar = showStatusBar,
    dockKeys = dockKeysList,
    hiddenKeys = hiddenKeysList,
    dockSeeded = dockSeeded,
    capsuleSeenPackages = capsuleSeenPackagesList,
    capsuleBlockedPackages = capsuleBlockedPackagesList,
)
