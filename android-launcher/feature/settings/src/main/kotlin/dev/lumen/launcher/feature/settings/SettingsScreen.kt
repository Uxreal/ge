package dev.lumen.launcher.feature.settings

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.prefs.PrefsRepository
import dev.lumen.launcher.core.data.prefs.PrefsSnapshot
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.theme.LocalTypography
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * The settings skeleton (§13 Phase 1): Compose, not a `PreferenceScreen` (§1.1), with §7's large
 * collapsing header. §3 forbids animating text size, so the large and collapsed titles cross-fade.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PrefsRepository,
) : ViewModel() {

    val prefs: StateFlow<PrefsSnapshot> = prefsRepo.prefs

    fun setColumns(columns: Int) = prefsRepo.setColumns(columns)
    fun setThemeMode(mode: String) = prefsRepo.setThemeMode(mode)
    fun setMotionSpeed(speed: Float) = prefsRepo.setMotionSpeed(speed)
    fun setReduceMotion(reduce: Boolean) = prefsRepo.setReduceMotion(reduce)
    fun setHapticIntensity(intensity: String) = prefsRepo.setHapticIntensity(intensity)
    fun setLabelLines(lines: Int) = prefsRepo.setLabelLines(lines)
    fun setHideLabels(hide: Boolean) = prefsRepo.setHideLabels(hide)
    fun setParallax(value: Float) = prefsRepo.setParallax(value)
    fun setSmoothness(n: Float) = prefsRepo.setSmoothness(n)
    fun setCapsuleEnabled(enabled: Boolean) = prefsRepo.setCapsuleEnabled(enabled)
    fun setShowStatusBar(show: Boolean) = prefsRepo.setShowStatusBar(show)
    fun blockCapsulePackage(pkg: String) = prefsRepo.blockCapsulePackage(pkg)
    fun unblockCapsulePackage(pkg: String) = prefsRepo.unblockCapsulePackage(pkg)
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSetHomeModel: (HomeModel) -> Unit,
    onRequestDefaultHome: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
    /** Where the Capsule docked and why, reported by the pill itself. */
    capsuleDiagnostic: String = "",
    /** Fires a ten-second self-test card into the pill. Null hides the row. */
    onCapsuleTest: (() -> Unit)? = null,
    /** Live one-liner from the media source; falls back to a static summary when null. */
    capsuleMediaStatus: (() -> String)? = null,
    /** Re-runs the D41 factory seeding: pins missing basics to the dock, adds grid seeds. */
    onApplyFactoryLayout: (() -> Unit)? = null,
) {
    val motion = LocalMotion.current
    val typography = LocalTypography.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = motion.enter()) { it / 3 } + fadeIn(motion.crossfade()),
        exit = slideOutVertically(animationSpec = motion.enter()) { it / 3 } + fadeOut(motion.crossfade()),
        modifier = modifier,
    ) {
        val prefs by vm.prefs.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        val collapsed by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                .safeDrawingPadding()
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            // §7 collapsing header; §3: cross-fade the two laid-out titles, never scale text.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            ) {
                Crossfade(targetState = collapsed, animationSpec = motion.crossfade(), label = "header") { small ->
                    BasicText(
                        text = "Settings",
                        style = (if (small) typography.headerCollapsed else typography.headerLarge)
                            .copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Text("Done")
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { SectionTitle("Home") }
                if (onApplyFactoryLayout != null) {
                    item {
                        ActionRow(
                            title = "Apply factory layout",
                            summary = "Pins your phone's default dialer, messages, browser and " +
                                "camera to the dock and puts clock and settings on the grid. " +
                                "Never duplicates what is already there.",
                        ) {
                            onApplyFactoryLayout()
                            onDismiss()
                        }
                    }
                }
                item {
                    ChoiceRow(
                        title = "Home model",
                        summary = "Packed keeps every app on a page. Freeform adds an app drawer.",
                        options = listOf("Packed", "Freeform"),
                        selected = if (prefs.homeModel == HomeModel.FREEFORM) 1 else 0,
                        onSelect = { onSetHomeModel(if (it == 1) HomeModel.FREEFORM else HomeModel.PACKED) },
                    )
                }
                item {
                    ChoiceRow(
                        title = "Columns",
                        summary = null,
                        options = listOf("4", "5", "6"),
                        selected = prefs.columns - 4,
                        onSelect = { vm.setColumns(it + 4) },
                    )
                }
                item {
                    SwitchRow("Hide labels", null, prefs.hideLabels) { vm.setHideLabels(it) }
                }
                item {
                    ChoiceRow(
                        title = "Label lines",
                        summary = null,
                        options = listOf("1", "2"),
                        selected = prefs.labelLines - 1,
                        onSelect = { vm.setLabelLines(it + 1) },
                    )
                }

                item { SectionTitle("Look") }
                item {
                    ChoiceRow(
                        title = "Theme",
                        summary = null,
                        options = listOf("Auto", "Light", "Dark", "Black"),
                        selected = when (prefs.themeMode) {
                            "LIGHT" -> 1
                            "DARK" -> 2
                            "TRUE_BLACK" -> 3
                            else -> 0
                        },
                        onSelect = {
                            vm.setThemeMode(listOf("AUTO", "LIGHT", "DARK", "TRUE_BLACK")[it])
                        },
                    )
                }
                item {
                    SliderRow(
                        title = "Icon shape",
                        summary = "Corner smoothness, circle-ish to near-square.",
                        value = prefs.smoothness,
                        range = 2f..6f,
                        onChange = { vm.setSmoothness(it) },
                    )
                }
                item {
                    SliderRow(
                        title = "Wallpaper parallax",
                        summary = "How far the wallpaper slides as you page.",
                        value = prefs.parallax,
                        range = 0f..1.5f,
                        onChange = { vm.setParallax(it) },
                    )
                }

                item { SectionTitle("Motion & haptics") }
                item {
                    SliderRow(
                        title = "Animation speed",
                        summary = null,
                        value = prefs.motionSpeed,
                        range = 0.5f..1.5f,
                        onChange = { vm.setMotionSpeed(it) },
                    )
                }
                item {
                    SwitchRow(
                        "Reduce motion",
                        "Replaces springs with quick cross-fades.",
                        prefs.reduceMotion,
                    ) { vm.setReduceMotion(it) }
                }
                item {
                    ChoiceRow(
                        title = "Haptics",
                        summary = null,
                        options = listOf("Off", "Light", "Standard", "Strong"),
                        selected = when (prefs.hapticIntensity) {
                            "OFF" -> 0
                            "LIGHT" -> 1
                            "STRONG" -> 3
                            else -> 2
                        },
                        onSelect = {
                            vm.setHapticIntensity(listOf("OFF", "LIGHT", "STANDARD", "STRONG")[it])
                        },
                    )
                }

                item { SectionTitle("Capsule") }
                item {
                    SwitchRow(
                        "Show the Capsule",
                        "The pill on the camera. Off hides it entirely.",
                        prefs.capsuleEnabled,
                    ) { vm.setCapsuleEnabled(it) }
                }
                item {
                    SwitchRow(
                        "Show the system status bar",
                        "Off by default: Lumen hides it on the home screen and draws time and " +
                            "battery in its own style beside the Capsule. Swipe down from the " +
                            "top edge to reach the shade either way.",
                        prefs.showStatusBar,
                    ) { vm.setShowStatusBar(it) }
                }
                if (onCapsuleTest != null) {
                    item {
                        ActionRow(
                            title = "Send a test card",
                            summary = "Puts a ten-second card in the pill so you can see where " +
                                "it lives and that it responds. Settings closes so you can watch.",
                        ) {
                            onCapsuleTest()
                            onDismiss()
                        }
                    }
                }
                if (capsuleDiagnostic.isNotEmpty()) {
                    item {
                        RowScaffold(
                            title = "Where the pill docked",
                            summary = capsuleDiagnostic,
                        )
                    }
                }
                item {
                    // §10: media needs notification access as its consent token, granted in system
                    // settings and revocable there. The row states the current answer plainly.
                    val context = LocalContext.current
                    var mediaSummary by remember {
                        mutableStateOf(mediaSummaryOf(context, capsuleMediaStatus))
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        mediaSummary = mediaSummaryOf(context, capsuleMediaStatus)
                    }
                    ActionRow(
                        title = "Media playback in the Capsule",
                        summary = mediaSummary,
                    ) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                            )
                        }
                    }
                }
                item {
                    // Android 13+ marks sideloaded apps "restricted": the notification-access
                    // toggle silently refuses until the user lifts the restriction from App info.
                    // Without this row that dead toggle is indistinguishable from a Lumen bug.
                    val context = LocalContext.current
                    ActionRow(
                        title = "Media toggle blocked or greyed out?",
                        summary = "Sideloaded apps need one extra step on newer Android: this row " +
                            "opens Lumen's App info. There, tap the three-dot menu (top right), " +
                            "choose Allow restricted settings, then come back and grant media " +
                            "playback above.",
                    ) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        }
                    }
                }
                // §4.1: any app can push a card without a permission, so the defence is a list the
                // user can actually see. Packages appear here the first time they push.
                if (prefs.capsuleSeenPackages.isEmpty()) {
                    item {
                        RowScaffold(
                            title = "Apps using the Capsule",
                            summary = "Nothing has pushed a card yet. Apps appear here the first " +
                                "time they do, and you can switch any of them off.",
                        )
                    }
                } else {
                    items(prefs.capsuleSeenPackages, key = { "capsule-$it" }) { pkg ->
                        val blocked = pkg in prefs.capsuleBlockedPackages
                        SwitchRow(
                            title = pkg,
                            summary = if (blocked) "Blocked" else "Allowed to push cards",
                            checked = !blocked,
                        ) { allow ->
                            if (allow) vm.unblockCapsulePackage(pkg) else vm.blockCapsulePackage(pkg)
                        }
                    }
                }

                item { SectionTitle("System") }
                item {
                    ActionRow("Set as default launcher", "Required for Lumen to own the Home button.") {
                        onRequestDefaultHome()
                    }
                }
                item {
                    ActionRow("About", "Lumen $versionName, sideload build. No network access, ever.") { }
                }
                item { Box(Modifier.padding(bottom = 32.dp)) }
            }
        }
    }
}

private fun hasNotificationAccess(context: Context): Boolean =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

private fun mediaSummaryOf(context: Context, live: (() -> String)?): String =
    live?.invoke() ?: if (hasNotificationAccess(context)) {
        "On. Now playing appears as a card with play, pause and next."
    } else {
        "Off. Requires notification access, which Android grants in system settings. Lumen " +
            "reads no notifications, only media sessions."
    }

@Composable
private fun SectionTitle(title: String) {
    val typography = LocalTypography.current
    BasicText(
        text = title,
        style = typography.tileLabel.copy(color = MaterialTheme.colorScheme.primary),
        modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
    )
}

@Composable
private fun RowScaffold(
    title: String,
    summary: String?,
    trailing: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val typography = LocalTypography.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                MaterialTheme.shapes.medium,
            )
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = title,
                    style = typography.body.copy(color = MaterialTheme.colorScheme.onSurface),
                )
                if (summary != null) {
                    BasicText(
                        text = summary,
                        style = typography.tileLabel.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            trailing?.invoke()
        }
        bottom?.invoke()
    }
}

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    RowScaffold(
        title = title,
        summary = summary,
        trailing = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    summary: String?,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    RowScaffold(
        title = title,
        summary = summary,
        bottom = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = index == selected.coerceIn(0, options.lastIndex),
                        onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) {
                        Text(label)
                    }
                }
            }
        },
    )
}

@Composable
private fun SliderRow(
    title: String,
    summary: String?,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    RowScaffold(
        title = title,
        summary = summary,
        bottom = {
            Slider(value = value, onValueChange = onChange, valueRange = range)
        },
    )
}

@Composable
private fun ActionRow(title: String, summary: String?, onClick: () -> Unit) {
    RowScaffold(title = title, summary = summary, onClick = onClick)
}
