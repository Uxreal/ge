package dev.lumen.launcher.feature.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import dev.lumen.launcher.core.data.model.Cell
import dev.lumen.launcher.core.data.model.GridItem
import dev.lumen.launcher.core.data.model.WorkspaceState
import kotlinx.coroutines.channels.Channel

/**
 * One drag, from lift to settle. The pointer drives [position] 1:1 while the finger is down (§3's
 * `follow`), and the drop animates the same [Animatable] with the release velocity carried into the
 * `morph` spring — the handoff §3 demands.
 */
@Stable
class DragSession(
    val item: GridItem,
    val sourcePage: Int,
    val grabOffset: Offset,
    start: Offset,
) {
    /** Top-left of the dragged icon, in home-surface coordinates. */
    val position = Animatable(start, Offset.VectorConverter)

    val velocity = VelocityTracker()

    var hoverPage by mutableStateOf(sourcePage)
    var hoverCell by mutableStateOf<Cell?>(null)

    /** Set after §5's 520ms dwell over another item; release then combines into a folder. */
    var combineTargetId by mutableStateOf<String?>(null)

    /** The item currently under the finger (candidate for combine/swap dwell). */
    var hoverItemId by mutableStateOf<String?>(null)

    /** −1, 0, +1: finger in the left/right page-advance zone. */
    var edgeDirection by mutableStateOf(0)

    /** True from release until the settle animation finishes; input is ignored meanwhile. */
    var dropping by mutableStateOf(false)
}

/** Home-surface state that the app shell needs to see (back handling, the home-press sequence). */
@Stable
class HomeState {
    var editMode by mutableStateOf(false)

    var openFolderId by mutableStateOf<String?>(null)

    var drag by mutableStateOf<DragSession?>(null)

    /** §5's PACKED live reflow: the layout everyone renders while a drag hovers, else null. */
    var preview by mutableStateOf<WorkspaceState?>(null)

    /** Requests the pager scroll home; consumed by the pager effect in HomeScreen. */
    val scrollToFirst = Channel<Unit>(Channel.CONFLATED)

    val hasOverlay: Boolean get() = openFolderId != null

    /**
     * §5's home-press ladder, one step per press: sheet → wiggle → page 1. Returns true when this
     * press was consumed (the shell stops the sequence there).
     */
    fun onHomePress(): Boolean = when {
        openFolderId != null -> {
            openFolderId = null
            true
        }
        editMode -> {
            editMode = false
            true
        }
        else -> {
            scrollToFirst.trySend(Unit)
            false
        }
    }

    /** Back gesture: same ladder, but consuming the page scroll too. */
    fun onBack(): Boolean = when {
        openFolderId != null -> {
            openFolderId = null
            true
        }
        editMode -> {
            editMode = false
            true
        }
        else -> false
    }
}
