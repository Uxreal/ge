package dev.lumen.launcher.feature.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lumen.launcher.core.data.icons.IconCache
import dev.lumen.launcher.core.data.model.AppKey

/**
 * Same shape as the home module's icon loader; duplicated deliberately — features are siblings
 * (§2's module boundaries) and twelve lines do not justify a shared UI module yet.
 */
@Composable
internal fun rememberDrawerIcon(cache: IconCache, key: AppKey, size: Dp): ImageBitmap? {
    val density = LocalDensity.current
    val sizePx = with(density) { size.roundToPx() }.coerceAtLeast(1)
    val generation by cache.generation.collectAsStateWithLifecycle()

    var bitmap by remember(key, sizePx) { mutableStateOf(cache.cached(key, sizePx)?.asImageBitmap()) }
    LaunchedEffect(key, sizePx, generation) {
        if (bitmap == null || generation > 0) {
            bitmap = cache.icon(key, sizePx)?.asImageBitmap()
        }
    }
    return bitmap
}
