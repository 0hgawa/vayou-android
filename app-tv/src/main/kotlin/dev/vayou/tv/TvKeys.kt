package dev.vayou.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Swallows an activation whose press began somewhere else.
 *
 * A button acts on the way up of a press, and the way up is not always delivered to whoever took the
 * way down. Two places it goes wrong, and both are things a viewer does every day:
 *
 * The app is started by pressing OK on its icon. The launcher takes the way down and is gone by the
 * time the way up arrives, so it lands on whatever this app has just focused and presses it -- the
 * app opened with a dialog already up that nobody asked for.
 *
 * A card is held to open its menu. The hold fires while the button is still down; the menu appears,
 * takes the focus, and is handed the way up of the very press that opened it -- so it chose its first
 * option and closed again, too fast to read.
 *
 * One orphan at most, once per window, and every press after it is a whole one.
 *
 * Only the first of a held key's downs counts as a press beginning. A key held down repeats, and
 * the repeats land here too once this window has the focus -- taking one for a press of its own was
 * enough to make the way up look like the end of it, which is how holding a card straight through
 * its menu was still choosing the first option.
 */
@Composable
fun Modifier.ignoringOrphanPress(): Modifier {
    var sawPressBegin by remember { mutableStateOf(false) }
    return onPreviewKeyEvent { event ->
        if (event.key != Key.DirectionCenter && event.key != Key.Enter) return@onPreviewKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> {
                if (event.nativeKeyEvent.repeatCount == 0) sawPressBegin = true
                false
            }

            KeyEventType.KeyUp -> !sawPressBegin
            else -> false
        }
    }
}
