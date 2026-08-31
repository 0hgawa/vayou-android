package dev.vayou.core.ui.designsystem.components

import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A panel over what is on screen, at whichever edge costs least.
 *
 * Up from the bottom in a tall window, in from the side in a wide one. The choice is made here and
 * nowhere else: thirty-five sheets are opened across this app and none of them says which kind it
 * wants, because none of them knows the shape of the window it will be opened in.
 *
 * By the shape of the space and not by the orientation flag, which is the rule the music player
 * already follows for the same reason: a tall window on a foldable or in split screen wants the
 * stacked answer even on a device that calls itself landscape.
 *
 * Asked of the configuration rather than measured here. A sheet is opened from inside some caller's
 * layout, and measuring there answers how big that caller is, not how big the window is.
 */
@Composable
fun VayouSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val window = LocalConfiguration.current
    if (window.screenWidthDp > window.screenHeightDp) {
        CompositionLocalProvider(LocalSheetFillsHeight provides true) {
            VayouSideSheet(onDismissRequest, modifier, content)
        }
    } else {
        BottomSheet(onDismissRequest, modifier, content)
    }
}

/**
 * The bottom half of that choice, on Material's own.
 *
 * The platform component brings drag-to-dismiss, predictive back, correct edge-to-edge insets and
 * accessibility, none of which is worth rewriting -- which is also the list [VayouSideSheet] had to
 * write by hand, there being no side sheet in Compose to borrow one from.
 */
/**
 * `rememberModalBottomSheetState` is deprecated and is kept anyway.
 *
 * What replaces it does not take `skipPartiallyExpanded`. The state still holds the notion, and
 * the only way to set it is a function the library keeps to itself -- so moving off the deprecated
 * call would quietly restore the half-open stop that every sheet in this app skips, and which is
 * the reason there is no drag handle below.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // Deprecated, and staying: see the note on this function.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VayouTheme.colors.surfaceContainer,
        contentColor = VayouTheme.colors.onSurface,
        scrimColor = VayouTheme.colors.scrim.copy(alpha = ScrimAlpha),
        // No drag handle: every sheet here skips the partially-expanded state, so a handle would be
        // advertising a drag that leads nowhere while costing the top of each sheet. Dragging the
        // sheet itself, tapping the scrim and the back gesture all still dismiss.
        dragHandle = null,
    ) {
        TransparentSystemBars()
        content()
    }
}

/**
 * Whether the sheet being drawn stands the full height of the window.
 *
 * A cap worked out as a share of the screen is what stops a bottom sheet becoming a screen. A side
 * sheet is already the full height, so the same cap would leave it half empty with nothing under
 * the list -- see [VayouSheetDefaults.ListMaxHeight].
 */
internal val LocalSheetFillsHeight = staticCompositionLocalOf { false }

/**
 * A sheet gets its own window, and nothing the activity set for edge-to-edge reaches it. Left
 * alone, that window enforces navigation-bar contrast and paints a pale strip behind the bar, so
 * the sheet looks like it stops short of the bottom of the screen.
 */
@Composable
internal fun TransparentSystemBars() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        // Ignored from API 35, where a window is already transparent behind the bars. Below it this
        // is the only way to say so, which is why it is still here and still guarded.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
    }
}

/** Same weight and inset as the player's overlay titles, so every panel in the app reads alike. */
@Composable
fun VayouSheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = VayouSheetDefaults.TitleStyle,
        color = VayouTheme.colors.onSurface,
        modifier = modifier.padding(
            start = VayouSheetDefaults.HorizontalPadding,
            end = VayouSheetDefaults.HorizontalPadding,
            top = VayouSheetDefaults.TitleTopPadding,
            bottom = VayouSheetDefaults.TitleBottomPadding,
        ),
    )
}

/**
 * How a sheet frames its title, in one place, because the player's overlays are the same kind of
 * panel on a different container and drifted apart by a few dp — a difference you only notice by
 * opening the same thing from both sides, which is exactly when you notice it.
 */
object VayouSheetDefaults {
    val HorizontalPadding = 24.dp
    val TitleTopPadding = 24.dp

    /**
     * How a sheet says its own name.
     *
     * Named rather than written out, because two sheets grew their own titles -- the equalizer needed
     * a switch on the line and the action sheet a thumbnail, so both were built by hand and both
     * landed a size below the plain ones.
     *
     * The size Material gives a sheet its name in. It was a rung above, and a rung above is what
     * made a sheet of six short rows read as an announcement.
     */
    val TitleStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.typography.titleLarge

    /**
     * What a control on a sheet calls itself, as against a row you pick.
     *
     * A step below a row, and on purpose: this names a slider or a switch sitting right beside it
     * rather than an item in a list, and at a row's weight seven of them stacked read as seven
     * headings instead of as one block of settings.
     */
    val ControlLabelStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.typography.bodyMedium

    /**
     * How tall the scrolling part of a sheet may grow.
     *
     * A share of the screen and not a number of dp. Seven sheets had picked seven numbers between
     * 280 and 480, so on a small phone one of them filled the screen while on a tall one another
     * stopped halfway up for no reason anybody could see.
     *
     * Uncapped in a side sheet, which stands the full height already: the share exists to stop a
     * sheet becoming a screen, and a panel at the edge is not covering the screen to begin with.
     */
    val ListMaxHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = if (LocalSheetFillsHeight.current) Dp.Infinity else screenHeight * ListHeightShare

    /** A queue is read and scrolled rather than picked from once, and it is the one list worth
     *  giving the extra fifth of the screen to. */
    val QueueMaxHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = if (LocalSheetFillsHeight.current) Dp.Infinity else screenHeight * QueueHeightShare

    /**
     * How tall the window is.
     *
     * The window and not the layout this is read in: a sheet has a window of its own and its
     * content is exactly as tall as the sheet, so a cap measured there answers "as tall as you
     * are" and collapses to whatever the content had already claimed. The queue came out one row
     * high, which read as a queue that had lost everything but the track playing.
     *
     * And the window and not the display, which is what this used to ask. They are the same number
     * on a phone held on its own and different ones the moment the app is sharing the screen: in
     * split screen the display is the whole panel and the window is the half this app was given, so
     * a cap worked out from the display let a sheet grow past the app it belongs to.
     */
    private val screenHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }

    /** Sixteen: the step Material puts between a header and what it heads, and the first one on the
     *  4dp grid that reads as a break rather than as leading. */
    val TitleBottomPadding = 16.dp

    /** Under the last row, so nothing sits on the edge of a sheet. */
    val BottomPadding = 24.dp

    /**
     * What a row in a sheet must be tall enough to hit. The floor, stated -- rows say this and let
     * their padding stay on the grid, rather than carrying an off-grid padding chosen to add up
     * to it.
     */
    val RowMinHeight = 48.dp
}

private const val ScrimAlpha = 0.32f

/** Enough that a dozen rows are worth scrolling, little enough that what the sheet is over is still
 *  visible behind it -- which is what says it is a sheet rather than a screen. */
private const val ListHeightShare = 0.6f

private const val QueueHeightShare = 0.8f
