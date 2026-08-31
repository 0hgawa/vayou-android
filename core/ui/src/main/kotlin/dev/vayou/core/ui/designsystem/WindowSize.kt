package dev.vayou.core.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

/**
 * How big the window is.
 *
 * The window, and not the layout this is read in: a dialog or a sheet has a window of its own and
 * its content is exactly as big as it is, so measuring there answers "as big as you are".
 *
 * And the window, and not the display. They are the same number on a phone held on its own and
 * different ones the moment the app is sharing the screen, which is exactly when a layout decision
 * made on the wrong one goes wrong.
 *
 * Asked of `LocalWindowInfo` rather than of a `Configuration`: the same number without building a
 * Configuration to get it, and without the deprecation that comes with asking for one. Three places
 * wanted it and each had written its own way of getting there.
 */
@Composable
@ReadOnlyComposable
fun windowSize(): DpSize = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.let { DpSize(it.width.toDp(), it.height.toDp()) }
}
