package dev.lumen.launcher.core.design.grid

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * §1's identity commitment: page 1 fills from the bottom up, so icons live where the thumb is and
 * empty space collects at the top under the Capsule, where a widget can occupy it.
 */
enum class GridGravity { BOTTOM, TOP, CENTER }

/** The user-selectable part of the grid. */
@Immutable
data class GridSpec(
    /** §3: 4 (default), 5 or 6. */
    val columns: Int = 4,
    val gravity: GridGravity = GridGravity.BOTTOM,
    /** §3: icon labels are one line by default, two optional. */
    val labelLines: Int = 1,
    val showLabels: Boolean = true,
) {
    init {
        require(columns in MIN_COLUMNS..MAX_COLUMNS) { "columns must be $MIN_COLUMNS..$MAX_COLUMNS" }
    }

    companion object {
        const val MIN_COLUMNS = 4
        const val MAX_COLUMNS = 6
    }
}

/**
 * Resolved grid geometry. §3 forbids hardcoded dp for anything here — every value derives from the
 * container, so a foldable posture change or a column change reflows without a second set of numbers.
 */
@Immutable
data class GridGeometry(
    val columns: Int,
    val rows: Int,
    val cellWidth: Dp,
    val cellHeight: Dp,
    val iconSize: Dp,
    val labelGap: Dp,
    val pageMargin: Dp,
    val gravity: GridGravity,
) {
    val cellCount: Int get() = columns * rows
}

/**
 * §3, verbatim:
 * ```
 * usableWidth = screenWidth - horizontalInsets - (2 * pageMargin)
 * cellWidth   = usableWidth / columns
 * iconSize    = cellWidth * 0.62      (clamped 44dp..76dp)
 * labelGap    = cellWidth * 0.06
 * pageMargin  = 20dp phone / 32dp tablet
 * rows        = computed from usableHeight, minimum 5
 * ```
 *
 * @param reservedTop space the Capsule owns; the grid never grows into it.
 * @param reservedBottom space for the page indicator and dock, if any.
 * @param labelLineHeight measured height of one line of `iconLabel` text, passed in so this stays
 *   free of font metrics and therefore unit-testable.
 */
fun computeGridGeometry(
    screenWidth: Dp,
    screenHeight: Dp,
    horizontalInsets: Dp,
    verticalInsets: Dp,
    reservedTop: Dp,
    reservedBottom: Dp,
    isTablet: Boolean,
    spec: GridSpec,
    labelLineHeight: Dp,
): GridGeometry {
    val pageMargin = if (isTablet) TABLET_PAGE_MARGIN else PHONE_PAGE_MARGIN

    val usableWidth = (screenWidth - horizontalInsets - pageMargin * 2).coerceAtLeast(1.dp)
    val cellWidth = usableWidth / spec.columns
    val iconSize = (cellWidth * ICON_RATIO).coerceIn(MIN_ICON, MAX_ICON)
    val labelGap = cellWidth * LABEL_GAP_RATIO

    val labelBlock = if (spec.showLabels) labelLineHeight * spec.labelLines else 0.dp
    val naturalCellHeight = iconSize + labelGap + labelBlock

    val usableHeight = (screenHeight - verticalInsets - reservedTop - reservedBottom)
        .coerceAtLeast(naturalCellHeight)

    // Rows come from the container, floored, but never fewer than five (§3). When five will not fit
    // at natural height, the cell compresses rather than the row count dropping — the grid's shape
    // is the promise, not the icon's breathing room.
    val fittingRows = floor(usableHeight / naturalCellHeight).toInt()
    val rows = fittingRows.coerceAtLeast(MIN_ROWS)
    val cellHeight = if (fittingRows >= MIN_ROWS) naturalCellHeight else usableHeight / rows

    return GridGeometry(
        columns = spec.columns,
        rows = rows,
        cellWidth = cellWidth,
        cellHeight = cellHeight,
        iconSize = iconSize,
        labelGap = labelGap,
        pageMargin = pageMargin,
        gravity = spec.gravity,
    )
}

const val ICON_RATIO = 0.62f
const val LABEL_GAP_RATIO = 0.06f
const val MIN_ROWS = 5
val MIN_ICON: Dp = 44.dp
val MAX_ICON: Dp = 76.dp
val PHONE_PAGE_MARGIN: Dp = 20.dp
val TABLET_PAGE_MARGIN: Dp = 32.dp
