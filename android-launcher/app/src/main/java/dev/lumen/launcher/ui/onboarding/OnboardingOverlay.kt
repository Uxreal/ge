package dev.lumen.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.design.interaction.launcherPressable
import dev.lumen.launcher.core.design.theme.LocalTypography

/**
 * First run: the one §5 decision that must precede the grid — `PACKED` or `FREEFORM` — then the
 * system's default-home prompt. Everything else is a setting, not a gate.
 */
@Composable
fun OnboardingOverlay(onChooseModel: (HomeModel) -> Unit, modifier: Modifier = Modifier) {
    val typography = LocalTypography.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText(
                text = "Lumen",
                style = typography.headerLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )
            BasicText(
                text = "One decision before your home screen exists. Both can be changed later " +
                    "without losing your layout.",
                style = typography.body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )

            ModelCard(
                title = "Packed",
                description = "Every app lives on a page, filling upward from the bottom where " +
                    "your thumb is. No app drawer. Gaps close themselves.",
                onClick = { onChooseModel(HomeModel.PACKED) },
            )
            ModelCard(
                title = "Freeform",
                description = "Pages hold only what you place, exactly where you put it. " +
                    "Swipe up for the app drawer.",
                onClick = { onChooseModel(HomeModel.FREEFORM) },
            )

            BasicText(
                text = "Next, Android will ask to make Lumen your home app.",
                style = typography.tileLabel.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun ModelCard(title: String, description: String, onClick: () -> Unit) {
    val typography = LocalTypography.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.shapes.large,
            )
            .launcherPressable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            text = title,
            style = typography.capsuleTitle.copy(color = MaterialTheme.colorScheme.primary),
        )
        BasicText(
            text = description,
            style = typography.body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
