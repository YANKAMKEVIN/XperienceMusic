package com.xperiencemusic.shared.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * Gaps and paddings. The main scale runs 2, 4, 8, 12, 16, 24, 32. The half steps [xsm], [sm],
 * [ml] and [lxl] (6, 10, 14, 20) are tokens of their own rather than snapped onto the scale:
 * together they are about 60 uses, mostly the inner padding of pills, rows and cards, where a
 * 2 dp move shows (a pill grows, a row loses its rhythm with its neighbours).
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    /** Between [xs] and [s]: the gap inside a compact pill or a dense row. */
    val xsm = 6.dp
    val s = 8.dp
    /** Between [s] and [m]: the side padding of a pill, the gap of a dense list. */
    val sm = 10.dp
    val m = 12.dp
    /** Between [m] and [l]: the inner padding of a raised row or a small tile. */
    val ml = 14.dp
    val l = 16.dp
    /** Between [l] and [xl]: the inner padding of a hero card, the sides of a brand button. */
    val lxl = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Inner paddings of the small rounded labels: a key, a role, "Toi", a status. */
object PillPadding {
    /** A one-line tag: 10 × 3. */
    val tag = PaddingValues(horizontal = Spacing.sm, vertical = 3.dp)
    /** A badge with a little more air: 10 × 4. */
    val badge = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs)
}

/**
 * Corner radii. [field], [card] and [hero] are also wired into Material's `Shapes` (see
 * `AppTheme`), so `MaterialTheme.shapes.medium`, `.large` and `.extraLarge` draw the same corners.
 * Bars and cursors as thick as twice their radius use `CircleShape` instead.
 */
object Radius {
    /** A thin bar of the level gauge. */
    val bar = 3.dp
    /** Small tags and the stop square of the recorder. */
    val small = 6.dp
    /** Text fields, menus and snack bars: softer than Material's 4 dp, a step under the cards. */
    val field = 12.dp
    /** Icon tiles and the compact buttons that sit next to a card. */
    val chip = 14.dp
    val card = 16.dp
    /** A raised list row (an objective of the progress screen). */
    val row = 18.dp
    /** The large choice cards and the warm-up card. */
    val cardLarge = 20.dp
    /** The large hero surfaces: the focus card, the empty-state tile. */
    val hero = 28.dp
}

/**
 * Fixed sizes: touch targets, icons, avatars and line widths. Drawing geometry that belongs to
 * a single component (a canvas, a gauge) stays as named constants next to that component.
 */
object Sizes {
    /** Borders and separators. */
    val hairline = 1.dp
    /** The border of a selected card: a touch heavier than [hairline]. */
    val strokeMedium = 1.5.dp
    /** Progress spinners and focus borders. */
    val stroke = 2.dp
    /** Gradient rings and the current step of a path. */
    val strokeBold = 3.dp

    val iconXs = 16.dp
    val iconS = 18.dp
    val iconM = 20.dp
    val iconL = 22.dp
    /** Material's default icon size. */
    val icon = 24.dp
    val iconXl = 26.dp
    /** The glyph of a large round action (record, pause). */
    val iconXxl = 36.dp

    /** The smallest tappable height, for a control inside a field or a segmented bar (44 pt on iOS). */
    val compactTarget = 44.dp
    /** The rounded square behind an icon at the start of a row. */
    val iconTile = 44.dp
    /** Material's minimum touch target. */
    val touchTarget = 48.dp
    /** A full-width secondary action. */
    val actionHeight = 52.dp
    /** A tall tappable area: the tuner card and the report rows. */
    val tallTarget = 64.dp
    /** Brand buttons, raised rows and the large controls of the stage mode. */
    val rowMin = 56.dp

    /** The signed-in member in a header. */
    val avatarS = 40.dp
    /** The member's own avatar on "Mon compte". */
    val avatarL = 112.dp
}
