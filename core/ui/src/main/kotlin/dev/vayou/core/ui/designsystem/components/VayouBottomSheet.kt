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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A panel that comes up from the bottom, on Material's own.
 *
 * The platform component brings drag-to-dismiss, predictive back, correct edge-to-edge insets and
 * accessibility, none of which is worth rewriting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VayouBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = VayouTheme.colors.surfaceContainer,
        contentColor = VayouTheme.colors.onSurface,
        scrimColor = VayouTheme.colors.scrim.copy(alpha = ScrimAlpha),
        // No drag handle: every sheet here skips the partially-expanded state, so a handle would be
        // advertising a drag that leads nowhere while costing the top of each sheet. Dragging the
        // sheet itself, tapping the scrim and the back gesture all still dismiss.
        dragHandle = null,
    ) {
        TransparentNavigationBar()
        content()
    }
}

/**
 * A sheet gets its own window, and nothing the activity set for edge-to-edge reaches it. Left
 * alone, that window enforces navigation-bar contrast and paints a pale strip behind the bar, so
 * the sheet looks like it stops short of the bottom of the screen.
 */
@Composable
private fun TransparentNavigationBar() {
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
fun VayouBottomSheetTitle(text: String, modifier: Modifier = Modifier) {
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
     */
    val TitleStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = VayouTheme.typography.headlineSmall

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
     */
    val ListMaxHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = screenHeight * ListHeightShare

    /** A queue is read and scrolled rather than picked from once, and it is the one list worth
     *  giving the extra fifth of the screen to. */
    val QueueMaxHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = screenHeight * QueueHeightShare

    /**
     * The screen, asked of the display rather than of the window this is being read in.
     *
     * A sheet has a window of its own, and that window is exactly as big as the sheet: asking it
     * how tall the screen is answers "as tall as you are", so a cap worked out from it collapsed to
     * whatever the content had already claimed. The queue came out one row high, which read as a
     * queue that had lost everything but the track playing.
     */
    private val screenHeight: Dp
        @Composable
        @ReadOnlyComposable
        get() = with(LocalDensity.current) {
            LocalContext.current.resources.displayMetrics.heightPixels.toDp()
        }

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
