package dev.lumen.launcher.core.design.surface

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.lumen.launcher.core.design.shape.Superellipse
import dev.lumen.launcher.core.design.theme.Depth
import dev.lumen.launcher.core.design.theme.drawSurfaceEdges
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * The four independently themable surface groups from §3. Each one resolves its own [FrostedTokens],
 * which is what lets the Capsule be near-opaque while sheets stay airy.
 */
enum class SurfaceRole { CAPSULE, ICONS, SHEETS, SEARCH }

/**
 * Material parameters for one frosted surface. Design-layer only — persistence maps user preferences
 * into these at the app layer, so `:core:design` never depends on `:core:data`.
 */
@Immutable
data class FrostedTokens(
    val enabled: Boolean = true,
    val blurRadius: Dp = 24.dp,
    /** Peak displacement of the backdrop at the rim. Zero yields a plain blur. */
    val refraction: Dp = 10.dp,
    /** Width of the refracting rim band — the apparent thickness of the sheet. */
    val band: Dp = 18.dp,
    val disperse: Float = 0.25f,
    val tint: Color = Color.White.copy(alpha = 0.10f),
    val specular: Float = 0.40f,
    val shade: Float = 0.18f,
    val brighten: Float = 0.03f,
    val saturation: Float = 1.12f,
    /** Degrees clockwise from straight up. One virtual light for the whole app. */
    val lightAngleDeg: Float = 315f,
) {
    companion object {
        /** §1.1: blur is not applied to every surface. Opaque surfaces use this. */
        val Off = FrostedTokens(
            enabled = false,
            blurRadius = 0.dp,
            refraction = 0.dp,
            disperse = 0f,
            specular = 0f,
        )
    }
}

/**
 * Holds the captured backdrop that frosted surfaces sample.
 *
 * Android composites the wallpaper *behind* a launcher's translucent window, and `RenderEffect` only
 * ever sees a layer's own content — so a sheet has nothing to blur unless the launcher captures its
 * own content first. The workspace records itself into one [GraphicsLayer]; each frosted surface
 * re-draws that layer into a private layer, translated so the pixels line up, with the blur and lens
 * effect attached. One capture, any number of surfaces.
 */
@Stable
class BackdropCapture internal constructor(internal val layer: GraphicsLayer) {

    internal var rootCoordinates: LayoutCoordinates? = null
    internal var hasContent by mutableStateOf(false)
    internal var frameNanos by mutableLongStateOf(0L)

    private val tickets = mutableStateMapOf<String, Unit>()

    /** Frosted surfaces currently mounted. Zero means nothing is sampling us. */
    internal var consumers by mutableIntStateOf(0)

    val isAnimating: Boolean get() = tickets.isNotEmpty() && consumers > 0

    /** True only when something is actually going to sample the capture. */
    internal val isNeeded: Boolean get() = consumers > 0

    /**
     * A frosted surface cannot know when the pixels behind it changed, so anything animating the
     * backdrop (paging, a drag, a sheet transition) holds a ticket for the duration. No tickets means
     * a still screen and no redraw cost at all.
     */
    fun setAnimating(tag: String, active: Boolean) {
        if (active) tickets[tag] = Unit else tickets.remove(tag)
    }
}

@Composable
fun rememberBackdropCapture(): BackdropCapture {
    val layer = rememberGraphicsLayer()
    val capture = remember(layer) { BackdropCapture(layer) }
    LaunchedEffect(capture) {
        snapshotFlow { capture.isAnimating }.collectLatest { active ->
            while (active) {
                withFrameNanos { nanos -> capture.frameNanos = nanos }
            }
        }
    }
    return capture
}

/** Marks the subtree whose pixels frosted surfaces sample. */
fun Modifier.backdropSource(capture: BackdropCapture): Modifier = this
    .onGloballyPositioned { capture.rootCoordinates = it }
    .drawWithContent {
        val target = IntSize(size.width.toInt(), size.height.toInt())
        if (capture.isNeeded && target.width > 0 && target.height > 0) {
            capture.layer.record(this, layoutDirection, target) {
                this@drawWithContent.drawContent()
            }
            drawLayer(capture.layer)
            if (!capture.hasContent) capture.hasContent = true
        } else {
            drawContent()
        }
    }

/**
 * Renders a frosted sheet behind the composable's content.
 *
 * Draw order: the refracted backdrop clipped to the superellipse, the tint wash, then §3's depth
 * edges — a 1dp inner top highlight and a 0.5dp hairline, which are what stop the surface reading as
 * a flat slab. There is no elevation stack; [Depth] owns the single shadow token.
 */
@Composable
fun Modifier.frosted(
    capture: BackdropCapture,
    tokens: FrostedTokens,
    cornerRadius: Dp,
    smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
): Modifier {
    if (!tokens.enabled) return this.opaqueSurface(tokens.tint, cornerRadius, smoothness)

    DisposableEffect(capture) {
        capture.consumers += 1
        onDispose { capture.consumers -= 1 }
    }

    val panelLayer = rememberGraphicsLayer()
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(FrostedShaders.FROSTED_LENS) }.getOrNull()
        } else {
            null
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { coords ->
            val root = capture.rootCoordinates
            origin = if (root != null && root.isAttached && coords.isAttached) {
                runCatching { root.localPositionOf(coords, Offset.Zero) }
                    .getOrElse { coords.positionInRoot() }
            } else {
                coords.positionInRoot()
            }
        }
        .drawWithCache {
            val radiusPx = cornerRadius.toPx()
            val path = Superellipse.path(size, radiusPx, smoothness)
            val blurPx = tokens.blurRadius.toPx()
            val refractPx = tokens.refraction.toPx()
            val bandPx = tokens.band.toPx()
            val highlightPx = Depth.InnerHighlight.toPx()
            val hairlinePx = Depth.Hairline.toPx()
            val angle = Math.toRadians(tokens.lightAngleDeg.toDouble()).toFloat()
            val lightX = sin(angle)
            val lightY = -cos(angle)
            val lightLen = hypot(lightX, lightY).coerceAtLeast(0.0001f)

            onDrawBehind {
                // Reading the frame clock is what re-samples the backdrop while it moves.
                capture.frameNanos

                if (capture.hasContent && size.minDimension > 1f) {
                    val effect = when {
                        shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            shader.setFloatUniform("uSize", size.width, size.height)
                            shader.setFloatUniform("uRadius", radiusPx)
                            shader.setFloatUniform("uSmoothness", smoothness)
                            shader.setFloatUniform("uBand", bandPx)
                            shader.setFloatUniform("uRefract", refractPx)
                            shader.setFloatUniform("uDisperse", tokens.disperse)
                            shader.setFloatUniform("uSpecular", tokens.specular)
                            shader.setFloatUniform("uShade", tokens.shade)
                            shader.setFloatUniform("uLight", lightX / lightLen, lightY / lightLen)
                            shader.setFloatUniform("uBrighten", tokens.brighten)
                            shader.setFloatUniform("uSaturation", tokens.saturation)
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

                        // API 30 has no RenderEffect at all; 31–32 has blur but no AGSL.
                        blurPx > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                            BlurEffect(blurPx, blurPx, TileMode.Clamp)

                        else -> null
                    }

                    panelLayer.renderEffect = effect
                    panelLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                        translate(-origin.x, -origin.y) {
                            drawLayer(capture.layer)
                        }
                    }
                    clipPath(path) { drawLayer(panelLayer) }
                }

                if (tokens.tint.alpha > 0.001f) {
                    drawPath(path, tokens.tint)
                }
                drawSurfaceEdges(path, highlightPx, hairlinePx)
            }
        }
}

/** §1.1's counterweight: a surface that needs no blur still gets the same shape and edges. */
private fun Modifier.opaqueSurface(tint: Color, cornerRadius: Dp, smoothness: Float): Modifier =
    drawWithCache {
        val path = Superellipse.path(size, cornerRadius.toPx(), smoothness)
        val highlightPx = Depth.InnerHighlight.toPx()
        val hairlinePx = Depth.Hairline.toPx()
        onDrawBehind {
            drawPath(path, tint)
            drawSurfaceEdges(path, highlightPx, hairlinePx)
        }
    }

/** A frosted container that resolves its material from the current [SurfaceRole] theming. */
@Composable
fun FrostedSurface(
    role: SurfaceRole,
    capture: BackdropCapture,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    smoothness: Float = Superellipse.DEFAULT_SMOOTHNESS,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalFrostedTokens.current[role] ?: FrostedTokens()
    Box(
        modifier = modifier
            .frosted(capture, tokens, cornerRadius, smoothness)
            .padding(contentPadding),
        content = content,
    )
}

val LocalFrostedTokens = staticCompositionLocalOf<Map<SurfaceRole, FrostedTokens>> { emptyMap() }

val LocalBackdropCapture = staticCompositionLocalOf<BackdropCapture?> { null }
