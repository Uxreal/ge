package dev.lumen.launcher.ui.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.sin

/**
 * Holds the captured backdrop that glass panels refract.
 *
 * Android composites a translucent launcher window over a wallpaper the app cannot sample, and
 * `RenderEffect` only ever sees a layer's *own* content. So the workspace records itself (wallpaper
 * layer included) into one [GraphicsLayer], and each glass panel re-draws that same layer into a
 * private layer of its own — translated so the pixels line up — with the blur + refraction render
 * effect attached. One capture, any number of lenses.
 */
@Stable
class BackdropState internal constructor(internal val layer: GraphicsLayer) {

    internal var rootCoordinates: LayoutCoordinates? = null
    internal var hasContent by mutableStateOf(false)

    /** Frame time in nanos; panels read this to re-refract when the content behind them moves. */
    internal var frameNanos by mutableLongStateOf(0L)

    private val continuousTags = mutableStateMapOf<String, Unit>()

    val isContinuous: Boolean get() = continuousTags.isNotEmpty()

    /**
     * Glass cannot know when the pixels behind it changed, so anything that animates the backdrop
     * (page swipe, drawer transition, animated wallpaper, drag) holds a ticket here for the
     * duration. No tickets means a still screen and zero redraw cost.
     */
    fun setContinuous(tag: String, active: Boolean) {
        if (active) continuousTags[tag] = Unit else continuousTags.remove(tag)
    }

    internal val timeSeconds: Float get() = frameNanos / 1_000_000_000f
}

@Composable
fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    val state = remember(layer) { BackdropState(layer) }
    LaunchedEffect(state) {
        snapshotFlow { state.isContinuous }.collectLatest { active ->
            while (active) {
                withFrameNanos { nanos -> state.frameNanos = nanos }
            }
        }
    }
    return state
}

/** Marks the subtree whose pixels the glass refracts. Wrap the wallpaper + workspace with this. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.rootCoordinates = it }
    .drawWithContent {
        val target = size.toIntSize()
        if (target.width > 0 && target.height > 0) {
            state.layer.record(this, layoutDirection, target) {
                this@drawWithContent.drawContent()
            }
            drawLayer(state.layer)
            if (!state.hasContent) state.hasContent = true
        } else {
            drawContent()
        }
    }

private fun Size.toIntSize(): IntSize = IntSize(width.toInt(), height.toInt())

/**
 * Turns any box into a slab of liquid glass.
 *
 * Draw order behind the composable's own content:
 *  1. the refracted, blurred backdrop, clipped to the superellipse silhouette,
 *  2. a tint wash (the "material" colour),
 *  3. a bevel — bright where the virtual light hits, dark on the opposite edge,
 *  4. a hairline rim stroke that catches the light.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: BackdropState,
    style: GlassStyle,
    cornerRadius: Dp,
    smoothing: Float = 0.72f,
    tint: Color? = null,
): Modifier {
    if (!style.enabled) {
        return this.flatGlass(tint ?: style.tint, cornerRadius, smoothing)
    }

    val capability = rememberGlassCapability()
    val panelLayer = rememberGraphicsLayer()
    val shader = remember(capability.runtimeShaders) {
        if (capability.runtimeShaders && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(GlassShaders.LIQUID_GLASS) }.getOrNull()
        } else {
            null
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val tilt = rememberTiltOffset(style.tiltReactive, style.tiltStrength)

    // Shimmer and tilt both need a frame clock; hold a ticket only while they are on.
    LaunchedEffect(backdrop, style.needsFrameClock) {
        backdrop.setContinuous("glass-animated", style.needsFrameClock)
    }
    DisposableEffect(backdrop) {
        onDispose { backdrop.setContinuous("glass-animated", false) }
    }

    val effectiveRadius = style.cornerRadiusOverride ?: cornerRadius
    val resolvedTint = tint ?: style.tint

    return this
        .onGloballyPositioned { coords ->
            val root = backdrop.rootCoordinates
            origin = if (root != null && root.isAttached && coords.isAttached) {
                runCatching { root.localPositionOf(coords, Offset.Zero) }.getOrElse { coords.positionInRoot() }
            } else {
                coords.positionInRoot()
            }
        }
        .drawWithCache {
            val radiusPx = effectiveRadius.toPx()
            val path = LauncherShapes.squirclePath(size, radiusPx, smoothing)
            val blurPx = style.blurRadius.toPx()
            val refractPx = style.refraction.toPx()
            val bandPx = style.thickness.toPx()
            val rimPx = style.rimWidth.toPx()
            val exponent = LauncherShapes.exponent(smoothing)

            onDrawBehind {
                // Reading the frame clock is what re-refracts the panel while the backdrop moves.
                val time = backdrop.timeSeconds
                val lightAngle = Math.toRadians(style.lightAngle.toDouble()).toFloat()
                val lightX = sin(lightAngle) + tilt.x
                val lightY = -cos(lightAngle) + tilt.y
                val lightLen = kotlin.math.hypot(lightX, lightY).coerceAtLeast(0.0001f)

                if (backdrop.hasContent && size.minDimension > 1f) {
                    val effect = when {
                        shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            shader.setFloatUniform("uSize", size.width, size.height)
                            shader.setFloatUniform("uRadius", radiusPx)
                            shader.setFloatUniform("uExp", exponent)
                            shader.setFloatUniform("uBand", bandPx)
                            shader.setFloatUniform("uRefract", refractPx)
                            shader.setFloatUniform("uDisp", style.dispersion)
                            shader.setFloatUniform("uSpec", style.specular)
                            shader.setFloatUniform("uShade", style.innerShadow)
                            shader.setFloatUniform("uLight", lightX / lightLen, lightY / lightLen)
                            shader.setFloatUniform("uGrain", style.grain)
                            shader.setFloatUniform("uBright", style.brightness)
                            shader.setFloatUniform("uSat", style.saturation)
                            shader.setFloatUniform("uShimmer", style.shimmer)
                            shader.setFloatUniform("uPhase", (time * style.shimmerSpeed) % 1f)
                            val lens = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
                            if (blurPx > 0.5f) {
                                AndroidRenderEffect.createChainEffect(
                                    lens,
                                    AndroidRenderEffect.createBlurEffect(
                                        blurPx, blurPx, Shader.TileMode.CLAMP,
                                    ),
                                )
                            } else {
                                lens
                            }.asComposeRenderEffect()
                        }

                        blurPx > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                            BlurEffect(blurPx, blurPx, TileMode.Clamp)

                        else -> null
                    }

                    panelLayer.renderEffect = effect
                    panelLayer.record(this, layoutDirection, size.toIntSize()) {
                        translate(-origin.x, -origin.y) {
                            drawLayer(backdrop.layer)
                        }
                    }
                    clipPath(path) {
                        drawLayer(panelLayer)
                    }
                }

                // Tint wash.
                if (resolvedTint.alpha > 0.001f) {
                    drawPath(path, resolvedTint)
                }

                // Bevel: the light-facing edge lifts, the opposite edge sinks.
                if (style.innerShadow > 0.001f || style.specular > 0.001f) {
                    val dir = Offset(lightX / lightLen, lightY / lightLen)
                    drawPath(
                        path,
                        Brush.linearGradient(
                            0f to Color.White.copy(alpha = 0.16f * style.specular),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.20f * style.innerShadow),
                            start = Offset(
                                x = size.width / 2f - dir.x * size.width / 2f,
                                y = size.height / 2f - dir.y * size.height / 2f,
                            ),
                            end = Offset(
                                x = size.width / 2f + dir.x * size.width / 2f,
                                y = size.height / 2f + dir.y * size.height / 2f,
                            ),
                        ),
                    )
                }

                // Hairline rim.
                if (rimPx > 0.05f && style.rimAlpha > 0.001f) {
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            0f to Color.White.copy(alpha = style.rimAlpha),
                            0.5f to Color.White.copy(alpha = style.rimAlpha * 0.18f),
                            1f to Color.White.copy(alpha = style.rimAlpha * 0.42f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                        ),
                        style = Stroke(width = rimPx),
                    )
                }
            }
        }
}

/** Fallback for when glass is switched off entirely: a plain translucent surface with a rim. */
private fun Modifier.flatGlass(tint: Color, cornerRadius: Dp, smoothing: Float): Modifier =
    drawWithCache {
        val path = LauncherShapes.squirclePath(size, cornerRadius.toPx(), smoothing)
        onDrawBehind {
            drawPath(path, tint)
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.16f),
                style = Stroke(width = 1f),
            )
        }
    }

/**
 * A ready-made glass container: pass a [GlassSurfaceId] and it resolves the user's settings for
 * that surface, so callers never touch shader parameters directly.
 */
@Composable
fun GlassSurface(
    surface: dev.lumen.launcher.data.prefs.GlassSurfaceId,
    backdrop: BackdropState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    smoothing: Float = 0.72f,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val style = rememberGlassStyle(surface)
    Box(
        modifier = modifier
            .liquidGlass(
                backdrop = backdrop,
                style = style,
                cornerRadius = cornerRadius,
                smoothing = smoothing,
                tint = tint,
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Accelerometer-driven nudge to the virtual light direction, so tilting the phone slides the
 * highlight across every glass surface the way it does on real glass.
 */
@Composable
fun rememberTiltOffset(enabled: Boolean, strength: Float): Offset {
    if (!enabled || strength <= 0.001f) return Offset.Zero
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(context, strength) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Gravity is ±9.81 on each axis; normalise, damp, then low-pass.
                val nx = (-event.values[0] / 9.81f).coerceIn(-1f, 1f) * strength
                val ny = (event.values[1] / 9.81f).coerceIn(-1f, 1f) * strength
                tilt = Offset(
                    x = tilt.x + (nx - tilt.x) * 0.08f,
                    y = tilt.y + (ny - tilt.y) * 0.08f,
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager?.unregisterListener(listener) }
    }
    return tilt
}

/** Convenience for drawing a glass edge highlight on arbitrary paths (used by icon tiles). */
fun DrawScope.drawGlassRim(
    path: androidx.compose.ui.graphics.Path,
    alpha: Float,
    widthPx: Float,
) {
    if (alpha <= 0.001f || widthPx <= 0.05f) return
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = alpha),
            1f to Color.White.copy(alpha = alpha * 0.25f),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        ),
        style = Stroke(width = widthPx),
    )
}
