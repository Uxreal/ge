package dev.lumen.launcher.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.data.model.HomeModel
import dev.lumen.launcher.core.design.interaction.launcherPressable
import dev.lumen.launcher.core.design.theme.LocalTypography

/**
 * First run, two decisions, one screen each:
 *
 *  1. `PACKED` or `FREEFORM` — two cards with a picture each, one line of copy.
 *  2. Make Lumen the Home app — one filled button firing the system prompt, one "Later".
 *
 * Step 2 shows on its own whenever the model is already chosen but onboarding never finished,
 * which is also how an update walks existing installs to the default-home prompt they missed.
 */
@Composable
fun OnboardingOverlay(
    needsModel: Boolean,
    onChooseModel: (HomeModel) -> Unit,
    onRequestDefaultHome: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .padding(28.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BasicText(
                text = "Lumen",
                style = typography.headerLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            )

            if (needsModel) {
                BasicText(
                    text = "How should your home screen work?",
                    style = typography.body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                ModelCard(
                    title = "Packed",
                    line = "All apps on pages, filling from the bottom. No drawer.",
                    packed = true,
                    onClick = { onChooseModel(HomeModel.PACKED) },
                )
                ModelCard(
                    title = "Freeform",
                    line = "Pages stay empty until you add apps. Swipe up for the drawer.",
                    packed = false,
                    onClick = { onChooseModel(HomeModel.FREEFORM) },
                )
                BasicText(
                    text = "You can switch later without losing your layout.",
                    style = typography.tileLabel.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            } else {
                BasicText(
                    text = "One more step: Android needs to know Lumen is your Home app, or the " +
                        "Home button keeps opening your old launcher.",
                    style = typography.body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        onRequestDefaultHome()
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Make Lumen my Home app")
                }
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Later — I'll do it in Settings")
                }
            }
        }
    }
}

@Composable
private fun ModelCard(title: String, line: String, packed: Boolean, onClick: () -> Unit) {
    val typography = LocalTypography.current
    val accent = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                MaterialTheme.shapes.large,
            )
            .launcherPressable(onClick = onClick)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModelGlyph(packed = packed, accent = accent, dim = dim, modifier = Modifier.size(56.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(
                text = title,
                style = typography.capsuleTitle.copy(color = MaterialTheme.colorScheme.primary),
            )
            BasicText(
                text = line,
                style = typography.body.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

/** A tiny phone diagram: packed shows a grid filled from the bottom, freeform a few placed dots. */
@Composable
private fun ModelGlyph(packed: Boolean, accent: Color, dim: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val corner = CornerRadius(size.width * 0.16f)
        drawRoundRect(
            color = dim.copy(alpha = 0.35f),
            size = size,
            cornerRadius = corner,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.05f),
        )
        val cols = 3
        val rows = 4
        val cellW = size.width / (cols + 1)
        val cellH = size.height / (rows + 1)
        val radius = size.width * 0.07f

        fun dotCenter(col: Int, row: Int) = Offset(
            x = cellW * (col + 1),
            y = cellH * (row + 1),
        )

        if (packed) {
            // Bottom two rows full, third row half — gravity made visible.
            val filled = listOf(
                0 to 3, 1 to 3, 2 to 3,
                0 to 2, 1 to 2, 2 to 2,
                0 to 1, 1 to 1,
            )
            filled.forEach { (col, row) -> drawCircle(accent, radius, dotCenter(col, row)) }
        } else {
            drawCircle(accent, radius, dotCenter(0, 3))
            drawCircle(accent, radius, dotCenter(2, 1))
            // The up-arrow of the drawer gesture.
            val cx = size.width / 2f
            val top = size.height * 0.30f
            val bottom = size.height * 0.62f
            val stroke = size.width * 0.05f
            drawLine(dim, Offset(cx, bottom), Offset(cx, top), strokeWidth = stroke)
            drawLine(dim, Offset(cx - size.width * 0.10f, top + size.height * 0.12f), Offset(cx, top), strokeWidth = stroke)
            drawLine(dim, Offset(cx + size.width * 0.10f, top + size.height * 0.12f), Offset(cx, top), strokeWidth = stroke)
        }
        // Keep Size referenced for the inspector-friendly signature.
        Size(size.width, size.height)
    }
}
