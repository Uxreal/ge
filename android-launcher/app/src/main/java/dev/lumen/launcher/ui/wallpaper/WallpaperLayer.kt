package dev.lumen.launcher.ui.wallpaper

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.data.LocalServices
import dev.lumen.launcher.data.prefs.WallpaperSource
import dev.lumen.launcher.ui.LocalBackdrop
import dev.lumen.launcher.ui.LocalSettings
import dev.lumen.launcher.ui.glass.GlassShaders
import kotlin.math.roundToInt

/**
 * The bottom-most layer of the launcher, and the reason the glass has anything to bend.
 *
 * A translucent launcher window shows the *system* wallpaper, which lives outside the app's own
 * render tree — `RenderEffect` can never sample it. So Lumen draws its own wallpaper layer inside
 * the captured backdrop: either a snapshot of the user's real wallpaper (when the platform allows
 * a home app to read it) or a GPU-rendered animated field. Either way the pixels belong to us, and
 * every glass panel can refract them.
 *
 * @param pageScroll fractional page position, for parallax.
 * @param overlayProgress 0..1 progress of any open overlay, used to sink the wallpaper back.
 */
@Composable
fun WallpaperLayer(
    pageScroll: Float,
    pageCount: Int,
    overlayProgress: Float,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val wallpaper = settings.wallpaper
    val services = LocalServices.current
    val backdrop = LocalBackdrop.current
    val snapshot by services.wallpaper.snapshot.collectAsStateWithLifecycle()
    val canRead by services.wallpaper.canReadWallpaper.collectAsStateWithLifecycle()

    // Fall back to the bundled animated field when the platform refuses to hand over the wallpaper.
    val source = remember(wallpaper.source, snapshot, canRead) {
        when (wallpaper.source) {
            WallpaperSource.SNAPSHOT -> if (snapshot != null) {
                WallpaperSource.SNAPSHOT
            } else if (canRead) {
                WallpaperSource.SNAPSHOT
            } else {
                WallpaperSource.LIQUID_MESH
            }
            else -> wallpaper.source
        }
    }

    val animated = source == WallpaperSource.LIQUID_MESH || source == WallpaperSource.AURORA
    var time by remember { mutableFloatStateOf(0f) }

    // Animated wallpapers hold a redraw ticket so the glass above them stays in sync.
    LaunchedEffect(backdrop, animated) {
        backdrop?.setContinuous("wallpaper", animated)
    }
    DisposableEffect(backdrop) {
        onDispose { backdrop?.setContinuous("wallpaper", false) }
    }
    LaunchedEffect(animated, wallpaper.meshSpeed) {
        if (!animated) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (last != 0L) {
                    time += (nanos - last) / 1_000_000_000f * wallpaper.meshSpeed.coerceIn(0f, 3f)
                }
                last = nanos
            }
        }
    }

    val accent = Color(settings.theme.accentColor)
    val secondary = Color(settings.theme.secondaryColor)
    val palette = remember(accent, secondary, wallpaper.meshFromAccent, wallpaper.solidColor) {
        if (wallpaper.meshFromAccent) {
            listOf(
                accent,
                secondary,
                lerp(accent, Color(0xFF52E0C4), 0.55f),
                lerp(secondary, Color(0xFFFF8AC2), 0.45f),
            )
        } else {
            listOf(
                Color(0xFF3E6BFF), Color(0xFF9B5CFF), Color(0xFF29D6B5), Color(0xFFFF7BAC),
            )
        }
    }
    val baseColor = remember(wallpaper.solidColor) { Color(wallpaper.solidColor) }

    val shader = remember(source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && animated) {
            val src = if (source == WallpaperSource.AURORA) GlassShaders.AURORA else GlassShaders.LIQUID_MESH
            runCatching { RuntimeShader(src) }.getOrNull()
        } else {
            null
        }
    }

    val parallaxFraction = if (wallpaper.parallax && pageCount > 1) {
        (pageScroll / (pageCount - 1).coerceAtLeast(1)) - 0.5f
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                when (source) {
                    WallpaperSource.SYSTEM -> Unit // The real wallpaper shows through the window.

                    WallpaperSource.SOLID -> drawRect(baseColor)

                    WallpaperSource.SNAPSHOT -> {
                        val bitmap = snapshot
                        if (bitmap == null) {
                            drawFallbackGradient(palette, baseColor, time)
                        } else {
                            drawWallpaperBitmap(
                                bitmap = bitmap,
                                parallaxFraction = parallaxFraction,
                                depth = wallpaper.parallaxDepth,
                                saturation = wallpaper.saturation,
                            )
                        }
                    }

                    WallpaperSource.LIQUID_MESH, WallpaperSource.AURORA -> {
                        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            shader.setFloatUniform("uSize", size.width, size.height)
                            shader.setFloatUniform("uTime", time)
                            shader.setFloatUniform("uComplexity", wallpaper.meshComplexity)
                            shader.setFloatUniform("uSat", wallpaper.saturation)
                            shader.setColorUniform("uBase", baseColor.toArgbInt())
                            shader.setColorUniform("uC0", palette[0].toArgbInt())
                            shader.setColorUniform("uC1", palette[1].toArgbInt())
                            shader.setColorUniform("uC2", palette[2].toArgbInt())
                            if (source == WallpaperSource.LIQUID_MESH) {
                                shader.setColorUniform("uC3", palette[3].toArgbInt())
                            }
                            // Parallax by drawing the field slightly oversized and sliding it.
                            val slide = parallaxFraction * size.width * wallpaper.parallaxDepth * 0.25f
                            translate(left = -slide) {
                                drawRect(ShaderBrush(shader), size = size.copy(width = size.width))
                            }
                        } else {
                            drawFallbackGradient(palette, baseColor, time)
                        }
                    }
                }

                // Dim, then an extra sink while an overlay is open (the drawer rising, a folder open).
                val dim = (wallpaper.dim + overlayProgress * wallpaper.darkenOnDrawerOpen)
                    .coerceIn(0f, 1f)
                if (dim > 0.001f) {
                    drawRect(Color.Black.copy(alpha = dim))
                }
            },
    )
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt().coerceIn(0, 255),
    (red * 255).roundToInt().coerceIn(0, 255),
    (green * 255).roundToInt().coerceIn(0, 255),
    (blue * 255).roundToInt().coerceIn(0, 255),
)

/** Centre-crops the wallpaper bitmap to fill, then slides it for page parallax. */
private fun DrawScope.drawWallpaperBitmap(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    parallaxFraction: Float,
    depth: Float,
    saturation: Float,
) {
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    if (bw <= 0f || bh <= 0f) return

    // Overscan horizontally so there is room to slide without exposing an edge.
    val overscan = 1f + 0.18f * depth.coerceIn(0f, 1f)
    val scale = maxOf(size.width * overscan / bw, size.height / bh)
    val drawWidth = bw * scale
    val drawHeight = bh * scale
    val slack = (drawWidth - size.width).coerceAtLeast(0f)
    val left = -slack / 2f + parallaxFraction * slack * -1f
    val top = (size.height - drawHeight) / 2f

    val filter = if (saturation != 1f) {
        ColorFilter.colorMatrix(saturationMatrix(saturation))
    } else {
        null
    }

    drawImage(
        image = bitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
        dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt()),
        colorFilter = filter,
    )
}

private fun saturationMatrix(saturation: Float): androidx.compose.ui.graphics.ColorMatrix =
    androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(saturation.coerceIn(0f, 3f)) }

/** Pre-API-33 (and shader-failure) path: drifting radial gradients, no AGSL required. */
private fun DrawScope.drawFallbackGradient(palette: List<Color>, base: Color, time: Float) {
    drawRect(base)
    val w = size.width
    val h = size.height
    val wells = listOf(
        Offset(w * (0.30f + 0.08f * kotlin.math.sin(time * 0.31f)), h * (0.24f + 0.06f * kotlin.math.cos(time * 0.27f))),
        Offset(w * (0.78f + 0.07f * kotlin.math.cos(time * 0.23f)), h * (0.38f + 0.08f * kotlin.math.sin(time * 0.19f))),
        Offset(w * (0.22f + 0.06f * kotlin.math.sin(time * 0.17f)), h * (0.76f + 0.05f * kotlin.math.cos(time * 0.29f))),
        Offset(w * (0.72f + 0.05f * kotlin.math.cos(time * 0.13f)), h * (0.86f + 0.06f * kotlin.math.sin(time * 0.21f))),
    )
    wells.forEachIndexed { index, centre ->
        val colour = palette[index % palette.size]
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colour.copy(alpha = 0.55f), Color.Transparent),
                center = centre,
                radius = maxOf(w, h) * 0.55f,
            ),
        )
    }
}

private fun Size.copy(width: Float = this.width, height: Float = this.height) = Size(width, height)
