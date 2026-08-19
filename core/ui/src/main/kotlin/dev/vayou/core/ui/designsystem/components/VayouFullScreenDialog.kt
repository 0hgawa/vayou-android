package dev.vayou.core.ui.designsystem.components

import android.graphics.Color
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Makes a full-screen dialog's window reach the edges of the screen.
 *
 * A dialog gets a window of its own, and nothing the activity set for edge to edge reaches it: left
 * alone it fits its content inside the system bars and paints the platform's own background behind
 * them, which on a form that fills the screen shows as a dark strip along the top and another along
 * the bottom. What the viewer sees is a page that does not quite cover the screen it replaced.
 *
 * This says the window may draw under the bars and that the bars themselves are transparent. What
 * it does not do is inset the content: a screen that takes this must pad itself -- usually with
 * `safeDrawingPadding()` -- or its first row lands under the clock.
 *
 * Kept apart from the bottom sheet's own handling on purpose. A sheet is not full screen: it wants
 * the navigation bar transparent so it can sit against the bottom edge, and nothing else. Both go
 * through this file so the two decisions are visible side by side rather than discovered twice.
 */
@Composable
fun VayouFullScreenDialogWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        // Ignored from API 35, where a window is already transparent behind the bars. Below it this
        // is the only way to say so, which is why it is still here and still guarded.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
    }
}
