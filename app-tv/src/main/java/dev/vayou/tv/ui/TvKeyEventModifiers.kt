package dev.vayou.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Intercepts the TV remote's long-press DPAD-Center / Enter and the dedicated Menu key,
 * routing both to [onLongPress]. Suppresses the trailing KeyUp so it doesn't double-fire
 * a regular click.
 */
@Composable
fun Modifier.tvLongPressClickable(onLongPress: (() -> Unit)?): Modifier = composed {
    if (onLongPress == null) return@composed this
    val longPressed = remember { booleanArrayOf(false) }
    onPreviewKeyEvent { event ->
        when {
            event.type == KeyEventType.KeyUp && event.key == Key.Menu -> {
                onLongPress()
                true
            }
            event.type == KeyEventType.KeyDown
                && (event.key == Key.DirectionCenter || event.key == Key.Enter)
                && event.nativeKeyEvent.isLongPress -> {
                onLongPress()
                longPressed[0] = true
                true
            }
            event.type == KeyEventType.KeyUp
                && (event.key == Key.DirectionCenter || event.key == Key.Enter)
                && longPressed[0] -> {
                longPressed[0] = false
                true
            }
            else -> false
        }
    }
}
