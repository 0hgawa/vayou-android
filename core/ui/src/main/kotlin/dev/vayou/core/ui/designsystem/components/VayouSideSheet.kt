package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import dev.vayou.core.ui.theme.VayouTheme

/**
 * A panel that comes in from the side, for a window wider than it is tall.
 *
 * The same thing a bottom sheet is, moved to the edge that costs least. A film in a wide window
 * fills it; a panel rising from the bottom takes the middle of the picture, which is where anyone
 * is looking. Coming from the side it takes a column off the end, and what was being watched stays
 * watchable behind it -- which is why every player that has been asked this question answers it
 * this way.
 *
 * Written here rather than taken from Material, which does not have one: the specification
 * describes side sheets, the view toolkit ships `SideSheetDialog`, and Compose has neither. What
 * Material's bottom sheet gives for free and this has to give for itself is the drag that dismisses
 * it, the animation on the way out, and the insets -- each below, and none of it more than it is.
 */
@Composable
internal fun VayouSideSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Held open past the request to close it. A dialog removed the moment it is dismissed cannot
    // play its way out -- it is simply gone, which reads as a glitch rather than as a panel
    // leaving. So the state closes first and the caller is told once the animation has run.
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(shown.currentState, shown.targetState) {
        if (!shown.targetState && !shown.currentState) onDismissRequest()
    }

    val dim by animateFloatAsState(if (shown.targetState) ScrimAlpha else 0f, label = "scrim")

    Dialog(
        onDismissRequest = { shown.targetState = false },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // Handled below instead, so that a tap outside plays the same way out as the back
            // gesture rather than cutting to nothing.
            dismissOnClickOutside = false,
            // Otherwise the dialog is laid out between the system bars, and the panel stops short
            // of the top and bottom of the screen with a band of dimmed wallpaper above and below
            // it -- which reads as a panel that failed to reach the edge rather than as one that
            // was meant to stop there. The insets are put back on the content alone, below.
            decorFitsSystemWindows = false,
        ),
    ) {
        TransparentSystemBars()
        StatusBarIconsFor(VayouTheme.colors.surfaceContainer)

        var pushed by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VayouTheme.colors.scrim.copy(alpha = dim))
                // A tap on the dim, not on the panel: the panel sits above this and takes
                // its own taps before they reach here.
                .pointerInput(Unit) { detectTapGestures { shown.targetState = false } },
        ) {
            AnimatedVisibility(
                visibleState = shown,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Column(
                    modifier = modifier
                        .fillMaxHeight()
                        .width(sheetWidth())
                        .graphicsLayer { translationX = pushed }
                        .background(VayouTheme.colors.surfaceContainer)
                        // Pushed off the edge to dismiss, which is the gesture a panel at an edge
                        // invites. Only away from the screen: dragging it further in would open a
                        // gap behind it that nothing fills.
                        .draggable(
                            state = rememberDraggableState { pushed = (pushed + it).coerceAtLeast(0f) },
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                val enough = with(density) { DismissDistance.toPx() }
                                if (pushed > enough) shown.targetState = false else pushed = 0f
                            },
                        )
                        // Only the edges this panel actually touches. Asked for all four, the
                        // start edge is padded too -- and that edge is in the middle of the screen,
                        // against nothing. It costs nothing in a tall window, where the system's
                        // insets are top and bottom; turned on its side they become left and right,
                        // and the panel pays for them twice out of the one dimension it is short of.
                        //
                        // The colour still runs to the edge of the screen: this insets what is read,
                        // not the surface behind it.
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                        ),
                    content = content,
                )
            }
        }
    }
}

/**
 * How wide the panel stands.
 *
 * A share of the window rather than a number, because the same number is a third of a tablet and
 * most of a small phone turned on its side.
 *
 * Half, and up to 480. Material names 256 to 400 for a side sheet, and 400 was tried first: on a
 * phone held sideways it left the equalizer's five sliders and their labels fighting for room,
 * narrower than the same sheet gets standing up. The range Material names is for a sheet beside a
 * layout that keeps working -- a tablet's list, a desktop's page. Here it is over a film, and the
 * film needs no width to go on being a film.
 */
@Composable
private fun sheetWidth() = (LocalConfiguration.current.screenWidthDp * WidthShare).dp
    .coerceIn(MinWidth, MaxWidth)

private const val ScrimAlpha = 0.32f

private const val WidthShare = 0.5f

private val MinWidth = 320.dp

private val MaxWidth = 480.dp

/** Far enough that a panel is not lost to the flick that was meant to scroll it. */
private val DismissDistance = 96.dp

/**
 * Which way round the clock and the battery are drawn, while this panel is up.
 *
 * A bottom sheet never reaches the status bar, so nothing in this app had needed to say. A side
 * sheet stands the full height and puts its own surface behind those icons: left alone they stay
 * light, and on a light panel they vanish.
 *
 * Taken from the panel's own colour rather than from the theme, which is the same answer by a
 * shorter route and the right one on both sides of the screen. Where the panel is light so is what
 * lies under the dim beside it -- a thirty-percent black over a light library is still light -- so
 * one choice of icon serves the whole bar.
 */
@Composable
private fun StatusBarIconsFor(surface: Color) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    val light = surface.luminance() > MidLuminance
    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = light
    }
}

/** Halfway between black and white, in the perceptual sense `luminance` reports. */
private const val MidLuminance = 0.5f
