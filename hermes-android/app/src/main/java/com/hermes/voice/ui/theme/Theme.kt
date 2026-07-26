package com.hermes.voice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val HermesCyan = Color(0xFF7CE7FF)
val HermesViolet = Color(0xFFB39DFF)
val HermesAmber = Color(0xFFFFC46B)
val HermesRose = Color(0xFFFF7A8A)
val HermesMint = Color(0xFF6EE7B7)

private val DarkColors = darkColorScheme(
    primary = HermesCyan,
    onPrimary = Color(0xFF00222D),
    primaryContainer = Color(0xFF11384A),
    onPrimaryContainer = HermesCyan,
    secondary = HermesViolet,
    onSecondary = Color(0xFF1D1338),
    secondaryContainer = Color(0xFF2A2350),
    onSecondaryContainer = HermesViolet,
    tertiary = HermesMint,
    background = Color(0xFF07090F),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF0C111B),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF161D2B),
    onSurfaceVariant = Color(0xFFA9B4C7),
    outline = Color(0xFF2A3548),
    outlineVariant = Color(0xFF1C2534),
    error = HermesRose,
    onError = Color(0xFF3A0710),
    errorContainer = Color(0xFF4A1420),
    onErrorContainer = Color(0xFFFFD9DE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00637E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFE9F8),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF5B4BA8),
    onSecondary = Color.White,
    tertiary = Color(0xFF11705A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF11151C),
    surface = Color.White,
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE6EBF3),
    onSurfaceVariant = Color(0xFF48505E),
    outline = Color(0xFFBFC7D4),
    error = Color(0xFFB3261E),
)

private val HermesTypography = Typography(
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun HermesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HermesTypography,
        content = content,
    )
}
