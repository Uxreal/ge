package dev.lumen.launcher.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.data.prefs.GlassQuality
import dev.lumen.launcher.data.prefs.GlassSettings
import dev.lumen.launcher.data.prefs.GlassSurfaceId
import dev.lumen.launcher.ui.LocalSettings

/** Everything the glass renderer needs, already resolved for one surface on this device. */
@Immutable
data class GlassStyle(
    val enabled: Boolean = true,
    val blurRadius: Dp = 28.dp,
    val refraction: Dp = 14.dp,
    val thickness: Dp = 22.dp,
    val dispersion: Float = 0.35f,
    val tint: Color = Color.White.copy(alpha = 0.12f),
    val brightness: Float = 0.04f,
    val saturation: Float = 1.18f,
    val specular: Float = 0.55f,
    val innerShadow: Float = 0.22f,
    val rimWidth: Dp = 1.2.dp,
    val rimAlpha: Float = 0.5f,
    val grain: Float = 0.02f,
    val lightAngle: Float = 315f,
    val tiltReactive: Boolean = true,
    val tiltStrength: Float = 0.6f,
    val shimmer: Float = 0.10f,
    val shimmerSpeed: Float = 0.22f,
    val cornerRadiusOverride: Dp? = null,
) {
    val needsFrameClock: Boolean get() = enabled && (shimmer > 0.001f || tiltReactive)

    companion object {
        val Disabled = GlassStyle(
            enabled = false,
            blurRadius = 0.dp,
            refraction = 0.dp,
            dispersion = 0f,
            specular = 0f,
            grain = 0f,
            shimmer = 0f,
            tiltReactive = false,
        )
    }
}

/** What this device can actually render. */
@Immutable
data class GlassCapability(
    val runtimeShaders: Boolean,
    val renderEffectBlur: Boolean,
    val lowRam: Boolean,
) {
    companion object {
        fun detect(context: Context): GlassCapability {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            return GlassCapability(
                runtimeShaders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                renderEffectBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                lowRam = am?.isLowRamDevice == true,
            )
        }
    }
}

@Composable
fun rememberGlassCapability(): GlassCapability {
    val context = LocalContext.current
    return remember(context) { GlassCapability.detect(context) }
}

/**
 * Resolves the global glass settings, the per-surface override, and the device's quality tier into
 * a single immutable style. Every glass surface in the launcher goes through here, which is what
 * makes one slider in Settings move all of them at once.
 */
@Composable
fun rememberGlassStyle(surface: GlassSurfaceId): GlassStyle {
    val settings = LocalSettings.current.glass
    val capability = rememberGlassCapability()
    return remember(settings, capability, surface) { resolveGlassStyle(settings, capability, surface) }
}

fun resolveGlassStyle(
    settings: GlassSettings,
    capability: GlassCapability,
    surface: GlassSurfaceId,
): GlassStyle {
    val override = settings.surfaces[surface]
    val tier = when (settings.quality) {
        GlassQuality.AUTO -> when {
            !capability.renderEffectBlur -> GlassQuality.POWER_SAVER
            capability.lowRam || !capability.runtimeShaders -> GlassQuality.BALANCED
            else -> GlassQuality.HIGH
        }
        else -> settings.quality
    }

    val enabled = (override?.enabled ?: settings.enabled) &&
        settings.quality != GlassQuality.OFF &&
        tier != GlassQuality.OFF
    if (!enabled) return GlassStyle.Disabled

    // Tier acts as a ceiling on the expensive terms, never on the cheap ones.
    val refractionAllowed = tier == GlassQuality.HIGH || tier == GlassQuality.BALANCED
    val dispersionAllowed = tier == GlassQuality.HIGH
    val blurScale = when (tier) {
        GlassQuality.HIGH -> 1f
        GlassQuality.BALANCED -> 0.85f
        else -> 0.5f
    }

    return GlassStyle(
        enabled = true,
        blurRadius = (settings.blurRadius * blurScale * (override?.blurScale ?: 1f)).coerceIn(0f, 80f).dp,
        refraction = if (refractionAllowed) {
            (settings.refraction * (override?.refractionScale ?: 1f)).coerceIn(0f, 64f).dp
        } else {
            0.dp
        },
        thickness = settings.thickness.coerceIn(1f, 96f).dp,
        dispersion = if (dispersionAllowed) settings.dispersion.coerceIn(0f, 1f) else 0f,
        tint = Color(settings.tintColor).copy(
            alpha = (settings.tintAlpha * (override?.tintAlphaScale ?: 1f)).coerceIn(0f, 1f),
        ),
        brightness = settings.brightness.coerceIn(-0.5f, 0.5f),
        saturation = settings.saturation.coerceIn(0f, 3f),
        specular = (settings.specular * (override?.specularScale ?: 1f)).coerceIn(0f, 2f),
        innerShadow = settings.innerShadow.coerceIn(0f, 1f),
        rimWidth = settings.rimWidth.coerceIn(0f, 8f).dp,
        rimAlpha = settings.rimAlpha.coerceIn(0f, 1f),
        grain = if (tier == GlassQuality.HIGH) settings.grain.coerceIn(0f, 0.2f) else 0f,
        lightAngle = settings.lightAngle,
        tiltReactive = settings.tiltReactive && tier == GlassQuality.HIGH,
        tiltStrength = settings.tiltStrength.coerceIn(0f, 1f),
        shimmer = if (settings.shimmer && tier == GlassQuality.HIGH) {
            settings.shimmerStrength.coerceIn(0f, 0.5f)
        } else {
            0f
        },
        shimmerSpeed = settings.shimmerSpeed.coerceIn(0.02f, 2f),
        cornerRadiusOverride = override?.cornerRadiusOverride?.takeIf { it >= 0f }?.dp,
    )
}
