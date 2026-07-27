package dev.lumen.launcher.feature.drawer

import android.graphics.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.lumen.launcher.core.data.apps.AppRepository
import dev.lumen.launcher.core.data.apps.UsageRepository
import dev.lumen.launcher.core.data.icons.IconCache
import dev.lumen.launcher.core.data.model.AppCategory
import dev.lumen.launcher.core.data.model.AppInfo
import dev.lumen.launcher.core.data.model.AppKey
import dev.lumen.launcher.core.design.icon.AppIcon
import dev.lumen.launcher.core.design.interaction.LocalHaptics
import dev.lumen.launcher.core.design.motion.LocalMotion
import dev.lumen.launcher.core.design.surface.FrostedSurface
import dev.lumen.launcher.core.design.surface.LocalBackdropCapture
import dev.lumen.launcher.core.design.surface.SurfaceRole
import dev.lumen.launcher.core.design.theme.LocalTypography
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The `FREEFORM` app drawer (§6): alphabetical grid, fast-scroll index rail, suggested-apps row
 * from usage, and type-to-launch — the search field filters as you type and Enter launches the
 * top hit.
 */
@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepo: AppRepository,
    private val usage: UsageRepository,
    val iconCache: IconCache,
) : ViewModel() {

    val apps: StateFlow<List<AppInfo>> = appRepo.apps

    fun launch(key: AppKey, bounds: Rect?): Boolean {
        val launched = appRepo.launch(key, bounds)
        if (launched) usage.recordLaunch(key)
        return launched
    }

    fun appInfo(key: AppKey) = appRepo.openAppInfo(key, null)

    fun canUninstall(key: AppKey) = appRepo.canUninstall(key)

    fun requestUninstall(key: AppKey) = appRepo.requestUninstall(key)

    fun suggested(limit: Int): List<AppInfo> {
        val index = apps.value.associateBy { it.key.flat }
        return usage.suggestions(limit).mapNotNull { index[it] }
    }

    fun filter(query: String): List<AppInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return apps.value
        return apps.value.filter { it.searchTokens.contains(q) }
            .sortedBy { !it.label.lowercase().startsWith(q) }
    }

    /**
     * The App Library grouping: apps bucketed by [AppCategory], biggest shelves first, with
     * SYSTEM and OTHER always last — they are where the heuristic gives up, not destinations.
     * Categories with a single app are folded into OTHER; a shelf of one is visual noise.
     */
    fun categorized(all: List<AppInfo>): List<Pair<AppCategory, List<AppInfo>>> {
        val groups = all.groupBy { it.category }.toMutableMap()
        val strays = groups.filterKeys { it != AppCategory.OTHER && it != AppCategory.SYSTEM }
            .filterValues { it.size < 2 }
        if (strays.isNotEmpty()) {
            strays.keys.forEach { groups.remove(it) }
            groups[AppCategory.OTHER] =
                (groups[AppCategory.OTHER].orEmpty() + strays.values.flatten())
        }
        val tail = listOf(AppCategory.SYSTEM, AppCategory.OTHER)
        return groups
            .mapValues { (_, list) -> list.sortedBy { it.label.lowercase() } }
            .toList()
            .sortedWith(
                compareBy<Pair<AppCategory, List<AppInfo>>> { it.first in tail }
                    .thenByDescending { it.second.size }
                    .thenBy { it.first.name },
            )
    }
}

@Composable
fun DrawerScreen(
    vm: DrawerViewModel,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** FREEFORM only: places the app on the home grid. Null hides the action. */
    onAddToHome: ((AppKey) -> Boolean)? = null,
) {
    val motion = LocalMotion.current
    val typography = LocalTypography.current
    val haptics = LocalHaptics.current
    val capture = LocalBackdropCapture.current

    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    var alphabetical by remember { mutableStateOf(false) }
    var openCategory by remember { mutableStateOf<AppCategory?>(null) }
    LaunchedEffect(visible) {
        if (!visible) {
            query = ""
            menuFor = null
            openCategory = null
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = motion.enter()) { it / 3 } + fadeIn(motion.crossfade()),
        exit = slideOutVertically(animationSpec = motion.enter()) { it / 3 } + fadeOut(motion.crossfade()),
        modifier = modifier,
    ) {
        val content: @Composable () -> Unit = {
            val apps by vm.apps.collectAsStateWithLifecycle()
            val filtered = remember(query, apps) { vm.filter(query) }
            val gridState = rememberLazyGridState()
            val focus = remember { FocusRequester() }
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 16.dp),
            ) {
                // Search field: type-to-launch (§6).
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = typography.body.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            filtered.firstOrNull()?.let { top ->
                                if (vm.launch(top.key, null)) onDismiss()
                            }
                        },
                    ),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    MaterialTheme.shapes.large,
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            if (query.isEmpty()) {
                                BasicText(
                                    text = "Search apps",
                                    style = typography.body.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    ),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 10.dp)
                        .focusRequester(focus),
                )
                LaunchedEffect(visible) { if (visible) focus.requestFocus() }

                // Suggested row (§6): usage-ranked, only while not searching.
                val suggestions = remember(apps, query) { if (query.isEmpty()) vm.suggested(4) else emptyList() }
                if (suggestions.isNotEmpty()) {
                    BasicText(
                        text = "Suggested",
                        style = typography.tileLabel.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    ) {
                        suggestions.forEach { app ->
                            DrawerAppIcon(vm, app, onLaunched = onDismiss, onMenu = { menuFor = it })
                        }
                    }
                }

                // View switch: the Library is the default; A–Z is one tap away. Searching
                // overrides both with the flat filtered list.
                if (query.isEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        ViewChip("Library", selected = !alphabetical) {
                            alphabetical = false
                            openCategory = null
                        }
                        ViewChip("A to Z", selected = alphabetical) {
                            alphabetical = true
                            openCategory = null
                        }
                    }
                }

                val category = openCategory
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        query.isNotEmpty() || alphabetical -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                state = gridState,
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(filtered, key = { _, app -> app.key.flat }) { _, app ->
                                    Box(contentAlignment = Alignment.Center) {
                                        DrawerAppIcon(vm, app, onLaunched = onDismiss, onMenu = { menuFor = it })
                                    }
                                }
                            }

                            // Fast-scroll rail (§6): drag along A–Z to jump.
                            if (query.isEmpty() && filtered.size > 30) {
                                IndexRail(
                                    apps = filtered,
                                    onJump = { index ->
                                        haptics.tick()
                                        scope.launch { gridState.scrollToItem(index) }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight(),
                                )
                            }
                        }

                        category != null -> {
                            val members = remember(apps, category) {
                                apps.filter { it.category == category }
                                    .sortedBy { it.label.lowercase() }
                            }
                            CategoryPage(
                                vm = vm,
                                category = category,
                                members = members,
                                onBack = { openCategory = null },
                                onLaunched = onDismiss,
                                onMenu = { menuFor = it },
                            )
                        }

                        else -> {
                            val shelves = remember(apps) { vm.categorized(apps) }
                            LibraryShelves(
                                vm = vm,
                                shelves = shelves,
                                onOpen = { openCategory = it },
                                onLaunched = onDismiss,
                                onMenu = { menuFor = it },
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        ) {
            if (capture != null) {
                FrostedSurface(
                    role = SurfaceRole.SHEETS,
                    capture = capture,
                    cornerRadius = 0.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    // Frost alone cannot guarantee legible theme text over an arbitrary
                    // wallpaper; this wash keeps the blur visible and the text readable.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)),
                    ) {
                        content()
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    content()
                }
            }

            menuFor?.let { app ->
                AppActionMenu(
                    app = app,
                    canAddToHome = onAddToHome != null,
                    canUninstall = vm.canUninstall(app.key),
                    onAddToHome = {
                        haptics.snap()
                        onAddToHome?.invoke(app.key)
                        menuFor = null
                        onDismiss()
                    },
                    onAppInfo = {
                        vm.appInfo(app.key)
                        menuFor = null
                    },
                    onUninstall = {
                        vm.requestUninstall(app.key)
                        menuFor = null
                    },
                    onDismiss = { menuFor = null },
                )
            }
        }
    }
}

/**
 * The long-press menu. "Add to Home" is the reason it exists — without it, `FREEFORM` had no way
 * to put an app on a page at all.
 */
@Composable
private fun AppActionMenu(
    app: AppInfo,
    canAddToHome: Boolean,
    canUninstall: Boolean,
    onAddToHome: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val typography = LocalTypography.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f))
            .pointerInput(app) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(vertical = 10.dp),
        ) {
            BasicText(
                text = app.label,
                style = typography.capsuleTitle.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            if (canAddToHome) MenuRow("Add to Home", onAddToHome)
            MenuRow("App info", onAppInfo)
            if (canUninstall) MenuRow("Uninstall", onUninstall)
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    val typography = LocalTypography.current
    BasicText(
        text = label,
        style = typography.body.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun DrawerAppIcon(
    vm: DrawerViewModel,
    app: AppInfo,
    onLaunched: () -> Unit,
    onMenu: (AppInfo) -> Unit,
) {
    val icon = rememberDrawerIcon(vm.iconCache, app.key, 56.dp)
    AppIcon(
        icon = icon,
        label = app.label,
        iconSize = 56.dp,
        labelLines = 1,
        labelColor = MaterialTheme.colorScheme.onSurface,
        accessibilityActions = buildList {
            add(
                androidx.compose.ui.semantics.CustomAccessibilityAction("Add to Home") {
                    onMenu(app)
                    true
                },
            )
            add(
                androidx.compose.ui.semantics.CustomAccessibilityAction("App info") {
                    vm.appInfo(app.key)
                    true
                },
            )
        },
        onClick = { bounds ->
            if (vm.launch(app.key, bounds)) onLaunched()
        },
        onLongPress = { onMenu(app) },
    )
}


/** The Library's segmented switch, drawn as two quiet chips rather than a Material control. */
@Composable
private fun ViewChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme
    BasicText(
        text = label,
        style = typography.tileLabel.copy(
            color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        ),
        modifier = Modifier
            .background(
                if (selected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = 0.45f),
                MaterialTheme.shapes.large,
            )
            .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * The App Library (inspired by Apple's, rebuilt in Lumen's language): two columns of category
 * tiles. Each tile launches its three most useful slots directly and opens the full shelf from
 * the mini-cluster in the fourth — the tile is a place to *use*, not just a folder to open.
 */
@Composable
private fun LibraryShelves(
    vm: DrawerViewModel,
    shelves: List<Pair<AppCategory, List<AppInfo>>>,
    onOpen: (AppCategory) -> Unit,
    onLaunched: () -> Unit,
    onMenu: (AppInfo) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(shelves, key = { _, (category, _) -> category.name }) { _, (category, members) ->
            CategoryTile(
                vm = vm,
                category = category,
                members = members,
                onOpen = { onOpen(category) },
                onLaunched = onLaunched,
                onMenu = onMenu,
            )
        }
    }
}

@Composable
private fun CategoryTile(
    vm: DrawerViewModel,
    category: AppCategory,
    members: List<AppInfo>,
    onOpen: () -> Unit,
    onLaunched: () -> Unit,
    onMenu: (AppInfo) -> Unit,
) {
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme
    val direct = members.take(3)
    val overflow = members.drop(3)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.extraLarge)
                // The tile background itself opens the shelf; icons on top win their own taps.
                .pointerInput(category) { detectTapGestures(onTap = { onOpen() }) }
                .padding(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                TileSlot(vm, direct.getOrNull(0), onLaunched, onMenu)
                TileSlot(vm, direct.getOrNull(1), onLaunched, onMenu)
            }
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                TileSlot(vm, direct.getOrNull(2), onLaunched, onMenu)
                if (overflow.isNotEmpty()) {
                    MiniCluster(vm, overflow, onOpen)
                } else {
                    Box(Modifier.size(TILE_ICON))
                }
            }
        }
        BasicText(
            text = category.displayName,
            style = typography.tileLabel.copy(color = colors.onSurfaceVariant),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

/** One of the tile's direct-launch icons. Empty slots keep the geometry honest. */
@Composable
private fun TileSlot(
    vm: DrawerViewModel,
    app: AppInfo?,
    onLaunched: () -> Unit,
    onMenu: (AppInfo) -> Unit,
) {
    if (app == null) {
        Box(Modifier.size(TILE_ICON))
        return
    }
    val icon = rememberDrawerIcon(vm.iconCache, app.key, TILE_ICON)
    AppIcon(
        icon = icon,
        label = app.label,
        iconSize = TILE_ICON,
        showLabel = false,
        onClick = { bounds -> if (vm.launch(app.key, bounds)) onLaunched() },
        onLongPress = { onMenu(app) },
    )
}

/** The fourth slot: up to four tiny icons hinting at the rest of the shelf. Tap opens it. */
@Composable
private fun MiniCluster(vm: DrawerViewModel, overflow: List<AppInfo>, onOpen: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(TILE_ICON)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                MaterialTheme.shapes.medium,
            )
            .pointerInput(Unit) { detectTapGestures(onTap = { onOpen() }) }
            .padding(5.dp),
    ) {
        overflow.take(4).chunked(2).forEach { rowApps ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                rowApps.forEach { app ->
                    val icon = rememberDrawerIcon(vm.iconCache, app.key, MINI_ICON)
                    AppIcon(
                        icon = icon,
                        label = app.label,
                        iconSize = MINI_ICON,
                        showLabel = false,
                        interactive = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

/** One open shelf: back header, then the category's full grid. */
@Composable
private fun CategoryPage(
    vm: DrawerViewModel,
    category: AppCategory,
    members: List<AppInfo>,
    onBack: () -> Unit,
    onLaunched: () -> Unit,
    onMenu: (AppInfo) -> Unit,
) {
    val typography = LocalTypography.current
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            BasicText(
                text = "Back",
                style = typography.tileLabel.copy(color = colors.primary),
                modifier = Modifier
                    .background(colors.surfaceVariant.copy(alpha = 0.45f), MaterialTheme.shapes.large)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onBack() }) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
            BasicText(
                text = category.displayName,
                style = typography.capsuleTitle.copy(color = colors.onSurface),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(members, key = { _, app -> app.key.flat }) { _, app ->
                Box(contentAlignment = Alignment.Center) {
                    DrawerAppIcon(vm, app, onLaunched = onLaunched, onMenu = onMenu)
                }
            }
        }
    }
}

private val TILE_ICON = 52.dp
private val MINI_ICON = 18.dp

/** The A–Z rail. Letters map to the first app whose label starts there. */
@Composable
private fun IndexRail(
    apps: List<AppInfo>,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = LocalTypography.current
    val letters = remember(apps) {
        apps.mapIndexed { index, app ->
            (app.label.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#') to index
        }.distinctBy { it.first }
    }
    if (letters.size < 5) return

    var railHeight by remember { mutableStateOf(1) }
    val jumpForFraction: (Float) -> Unit = { fraction ->
        val slot = (fraction * letters.size).toInt().coerceIn(0, letters.lastIndex)
        onJump(letters[slot].second)
    }

    Column(
        modifier = modifier
            .width(24.dp)
            .onSizeChangedCompat { railHeight = it }
            .pointerInput(letters) {
                detectDragGestures { change, _ ->
                    change.consume()
                    jumpForFraction(change.position.y / railHeight.coerceAtLeast(1))
                }
            }
            .pointerInput(letters) {
                detectTapGestures { offset -> jumpForFraction(offset.y / railHeight.coerceAtLeast(1)) }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { (letter, _) ->
            BasicText(
                text = letter.toString(),
                style = typography.tileLabel.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                ),
            )
        }
    }
}

private fun Modifier.onSizeChangedCompat(onHeight: (Int) -> Unit): Modifier =
    onSizeChanged { onHeight(it.height) }
