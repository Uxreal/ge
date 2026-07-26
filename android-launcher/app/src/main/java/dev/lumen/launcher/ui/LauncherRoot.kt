package dev.lumen.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.BuildConfig
import dev.lumen.launcher.MainActivity
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.data.system.CrashLog
import dev.lumen.launcher.core.data.system.DefaultHome
import dev.lumen.launcher.core.design.interaction.HapticIntensity
import dev.lumen.launcher.core.design.motion.MotionTokens
import dev.lumen.launcher.core.design.surface.LocalBackdropCapture
import dev.lumen.launcher.core.design.surface.backdropSource
import dev.lumen.launcher.core.design.surface.rememberBackdropCapture
import dev.lumen.launcher.core.design.theme.LumenTheme
import dev.lumen.launcher.core.design.theme.LumenThemeConfig
import dev.lumen.launcher.core.design.theme.LocalTypography
import dev.lumen.launcher.core.design.theme.LumenTypography
import dev.lumen.launcher.core.design.theme.ThemeMode
import dev.lumen.launcher.feature.capsule.CapsuleViewModel
import dev.lumen.launcher.feature.capsule.ui.CapsuleHost
import dev.lumen.launcher.feature.drawer.DrawerScreen
import dev.lumen.launcher.feature.drawer.DrawerViewModel
import dev.lumen.launcher.feature.home.HomeViewModel
import dev.lumen.launcher.feature.home.ui.HomeScreen
import dev.lumen.launcher.feature.home.ui.HomeState
import dev.lumen.launcher.feature.settings.SettingsScreen
import dev.lumen.launcher.feature.settings.SettingsViewModel
import dev.lumen.launcher.feature.widgets.WidgetFrame
import dev.lumen.launcher.feature.widgets.WidgetPickerSheet
import androidx.compose.foundation.layout.safeDrawingPadding
import dev.lumen.launcher.ui.onboarding.OnboardingOverlay

/** Which full-screen surface sits above the home grid. At most one. */
private enum class Overlay { NONE, DRAWER, SETTINGS, WIDGET_PICKER }

/**
 * Composition root. The home surface (the thing frosted sheets refract) lives inside the backdrop
 * capture; the drawer, settings, picker and onboarding sit above it as lenses.
 */
@Composable
fun LauncherRoot(
    activity: MainActivity,
    homeVm: HomeViewModel,
    drawerVm: DrawerViewModel,
    settingsVm: SettingsViewModel,
    capsuleVm: CapsuleViewModel,
) {
    val prefs by homeVm.prefs.collectAsStateWithLifecycle()

    val themeConfig = remember(prefs) {
        LumenThemeConfig(
            mode = when (prefs.themeMode) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                "TRUE_BLACK" -> ThemeMode.TRUE_BLACK
                else -> ThemeMode.AUTO
            },
            typography = LumenTypography(),
            motion = MotionTokens(speed = prefs.motionSpeed, reduceMotion = prefs.reduceMotion),
            hapticIntensity = runCatching { HapticIntensity.valueOf(prefs.hapticIntensity) }
                .getOrDefault(HapticIntensity.STANDARD),
            smoothness = prefs.smoothness,
        )
    }

    LumenTheme(config = themeConfig) {
        val capture = rememberBackdropCapture()
        val homeState = remember { HomeState() }
        var overlay by remember { mutableStateOf(Overlay.NONE) }
        val view = LocalView.current

        // The user leaves for the system's default-apps screen and comes straight back, so the
        // check must re-run on every resume — not once.
        var isDefaultHome by remember { mutableStateOf(DefaultHome.isDefault(activity)) }
        androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            isDefaultHome = DefaultHome.isDefault(activity)
        }
        val requestDefaultHome: () -> Unit = {
            val intent = DefaultHome.requestRoleIntent(activity)
            if (intent != null) {
                runCatching { activity.startActivity(intent) }
                    .onFailure { DefaultHome.openHomeSettings(activity) }
            } else {
                DefaultHome.openHomeSettings(activity)
            }
        }

        // §5 home-press sequence: close sheet → exit wiggle → page 1. One step per press.
        LaunchedEffect(activity) {
            for (unit in activity.homePresses) {
                if (overlay != Overlay.NONE) {
                    overlay = Overlay.NONE
                } else {
                    homeState.onHomePress()
                }
            }
        }

        // Widget bind/configure results place the item once the system flows finish.
        var pendingWidgetLabel by remember { mutableStateOf("") }
        var pendingProviderFlat by remember { mutableStateOf("") }
        var pendingSpan by remember { mutableStateOf(1 to 1) }
        LaunchedEffect(activity) {
            for (result in activity.widgetFlowResults) {
                if (result != null && pendingProviderFlat.isNotEmpty()) {
                    homeVm.placeWidget(
                        appWidgetId = result,
                        providerFlat = pendingProviderFlat,
                        label = pendingWidgetLabel,
                        spanX = pendingSpan.first,
                        spanY = pendingSpan.second,
                    )
                }
                pendingProviderFlat = ""
            }
        }

        BackHandler(enabled = true) {
            when {
                overlay != Overlay.NONE -> overlay = Overlay.NONE
                homeState.onBack() -> Unit
                else -> Unit // A launcher consumes Back at the root; there is nowhere to go.
            }
        }

        CompositionLocalProvider(LocalBackdropCapture provides capture) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ---- refractable content: the home surface --------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .backdropSource(capture),
                ) {
                    HomeScreen(
                        vm = homeVm,
                        homeState = homeState,
                        onPagerChanged = { page, offset, count ->
                            // §5 wallpaper offsets, scaled by the user's parallax multiplier.
                            val fraction = if (count > 1) {
                                ((page + offset) / (count - 1f)).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            val scaled = 0.5f + (fraction - 0.5f) * prefs.parallax
                            homeVm.wallpaper.setOffsets(view.windowToken, scaled)
                        },
                        onOpenDrawer = if (prefs.homeModel == HomeModel.FREEFORM) {
                            { overlay = Overlay.DRAWER }
                        } else {
                            null
                        },
                        onRequestWidgetPicker = { overlay = Overlay.WIDGET_PICKER },
                        onRequestSettings = { overlay = Overlay.SETTINGS },
                        onReleaseWidget = { id -> activity.widgetHost.releaseId(id) },
                        widgetContent = { item, itemModifier, editing, onResize ->
                            WidgetFrame(
                                hostManager = activity.widgetHost,
                                item = item,
                                modifier = itemModifier,
                                editing = editing,
                                onResize = onResize,
                            )
                        },
                    )
                }

                // ---- the Capsule ----------------------------------------------------------
                // §1: the single pill at the top, and the only handle the OS gets on this screen.
                // It sits above the backdrop source so it refracts the home surface, and it is
                // hidden whenever a full-screen lens or onboarding owns the display.
                // Wiggle mode's chip bar owns the same strip, so the two never share it.
                if (prefs.capsuleEnabled && prefs.onboardingDone &&
                    overlay == Overlay.NONE && !homeState.editMode
                ) {
                    val deck by capsuleVm.deck.collectAsStateWithLifecycle()
                    CapsuleHost(
                        deck = deck,
                        capture = capture,
                        onPin = capsuleVm::pinFront,
                        onDismiss = capsuleVm::dismiss,
                        onLaunch = { intent -> runCatching { intent.send() } },
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopCenter)
                            .safeDrawingPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }

                // ---- lenses ---------------------------------------------------------------
                DrawerScreen(
                    vm = drawerVm,
                    visible = overlay == Overlay.DRAWER,
                    onDismiss = { overlay = Overlay.NONE },
                    onAddToHome = if (prefs.homeModel == HomeModel.FREEFORM) {
                        { key -> homeVm.addToHome(key) }
                    } else {
                        null
                    },
                )

                SettingsScreen(
                    vm = settingsVm,
                    visible = overlay == Overlay.SETTINGS,
                    onDismiss = { overlay = Overlay.NONE },
                    onSetHomeModel = { model -> homeVm.setModel(model) },
                    onRequestDefaultHome = requestDefaultHome,
                    versionName = BuildConfig.VERSION_NAME,
                )

                WidgetPickerSheet(
                    hostManager = activity.widgetHost,
                    visible = overlay == Overlay.WIDGET_PICKER,
                    onPick = { provider ->
                        pendingWidgetLabel = activity.widgetHost.labelOf(provider)
                        pendingProviderFlat = provider.provider.flattenToString()
                        pendingSpan = activity.widgetHost.spanFor(provider, 180f, 220f)
                        overlay = Overlay.NONE
                        activity.startWidgetFlow(provider)
                    },
                    onDismiss = { overlay = Overlay.NONE },
                )

                // The banner that answers "I can't use it as my launcher": visible whenever
                // Lumen is not the default home, one tap to the system prompt.
                if (!isDefaultHome && prefs.homeModel != null && prefs.onboardingDone &&
                    overlay == Overlay.NONE && !homeState.editMode
                ) {
                    DefaultHomeBanner(
                        onSet = requestDefaultHome,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.TopCenter)
                            .safeDrawingPadding()
                            .padding(top = 10.dp),
                    )
                }

                if (prefs.homeModel == null || !prefs.onboardingDone) {
                    OnboardingOverlay(
                        needsModel = prefs.homeModel == null,
                        onChooseModel = { model -> homeVm.setModel(model) },
                        onRequestDefaultHome = requestDefaultHome,
                        onDone = { homeVm.completeOnboarding() },
                    )
                }

                // If the previous run died, say so and hand over the trace. Field debugging
                // depends on this card existing — "it does not load" carries no stack trace.
                var crashText by remember { mutableStateOf(CrashLog.read(activity)) }
                crashText?.let { trace ->
                    CrashCard(
                        trace = trace,
                        onShare = { CrashLog.share(activity) },
                        onDismiss = {
                            CrashLog.clear(activity)
                            crashText = null
                        },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

/** The last run crashed: one card, the first stack frame, share and dismiss. */
@Composable
private fun CrashCard(
    trace: String,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme
    val headline = trace.lineSequence()
        .firstOrNull { it.contains("Exception") || it.contains("Error") }
        ?.trim()?.take(140) ?: "Previous session crashed"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(colors.errorContainer, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = "Lumen crashed last time",
            style = typography.capsuleTitle.copy(color = colors.onErrorContainer),
        )
        BasicText(
            text = headline,
            style = typography.tileLabel.copy(color = colors.onErrorContainer),
        )
        Row {
            TextButton(onClick = onShare) { Text("Share log") }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/** Lumen is installed but not the Home app: nothing works until this is tapped. Say so, plainly. */
@Composable
private fun DefaultHomeBanner(onSet: () -> Unit, modifier: Modifier = Modifier) {
    val typography = LocalTypography.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        BasicText(
            text = "Lumen isn't your Home app yet",
            style = typography.tileLabel.copy(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        TextButton(onClick = onSet) { Text("Set as Home") }
    }
}
