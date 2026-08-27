package dev.vayou.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.VayouIcons

/**
 * The one focusable row this app draws in a list: it fills with white when the focus lands on it.
 *
 * [isMarked] is where the viewer is, as against where the focus is -- a quiet plate rather than the
 * white fill, for a heading whose rows are being read on the other side of the screen.
 */
@Composable
fun TvRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isMarked: Boolean = false,
    content: @Composable RowScope.(isFocused: Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isMarked) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TvRowInset, vertical = TvRowGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvRowInset),
        ) {
            content(isFocused)
        }
    }
}

/** Black on the white plate the focus draws, and the usual two weights off it. */
@Composable
fun tvTone(isFocused: Boolean, isStrong: Boolean): Color = when {
    isFocused -> MaterialTheme.colorScheme.surface
    isStrong -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/**
 * A list of answers, over the screen rather than beside it: it is one question, and it is brief.
 *
 * The tick is in a gutter down the left, held open whether a row is the chosen one or not, as the
 * player's own lists have it: a viewer runs their eye down a column, and a mark on the right lands
 * wherever each label happens to end.
 */
@Composable
fun <T> TvChoiceList(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onChoose: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    BackHandler(onBack = onDismiss)

    TvDialog(title = title, onDismiss = onDismiss) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(TvRowGap)) {
            items(options) { (value, label) ->
                TvChoiceRow(
                    label = label,
                    isSelected = value == selected,
                    modifier = if (value == selected) Modifier.focusRequester(first) else Modifier,
                ) {
                    onChoose(value)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Something laid over the screen, waiting to be answered.
 *
 * The whole screen, and a window of its own. Both are the television's doing: a panel floating in
 * the middle of a picture is a thing a viewer has to find with a remote, and the screens that raise
 * these are lazy lists, which are free to recycle the very card that opened one out of existence
 * while it is still up.
 *
 * The window brings the back key with it, so there is no handler here -- dismissing is what back
 * already does.
 *
 * Solid, not a scrim. A dimmed screen is a phone's answer, where the panel is a card in the middle
 * of it and the finger is on the card; three metres away a grid showing faintly through a list of
 * options reads as one screen with two sets of things on it, and the eye goes looking for which of
 * them the remote is in.
 */
@Composable
fun TvDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    TvOverlay(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(TvDialogWidth),
            verticalArrangement = Arrangement.spacedBy(TvRowGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = TvRowInset, vertical = TvCardTitleGap),
            )
            content()
        }
    }
}

/**
 * The screen, taken over: a window of its own, solid, and deaf to the press that opened it.
 *
 * Everything a television lays over a screen is built on this. [TvDialog] is the common case, a
 * question in the middle of it; what can be done to one thing lays itself out differently and starts
 * here instead.
 */
@Composable
fun TvOverlay(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .ignoringOrphanPress()
                // Laid out in what is left once a keyboard is up rather than in the screen: a
                // question asking to be typed into should not be the half the keyboard covers.
                .imePadding(),
            content = content,
        )
    }
}

@Composable
fun TvChoiceRow(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TvRow(onClick = onClick, modifier = modifier) { isFocused ->
        Box(modifier = Modifier.size(TvRowTick), contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = VayouIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(TvRowTick),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = tvTone(isFocused, isStrong = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * The ways out of a question, at the foot of whatever asked it.
 *
 * Side by side, and each one the width of its own word. A decision between two actions is not a
 * list of answers, and drawn as one -- a column of plates the full width of the panel, with a tick
 * gutter down the left that can never fill -- it reads as two more options rather than as the way
 * out. The set draws this distinction itself: `Button` is the size of what is in it, `WideButton`
 * fills the row, and they are two components rather than one with a flag.
 *
 * On the same left edge as the title above, because everything in that column shares it. The one
 * that undoes nothing goes first, as it does on a phone.
 */
@Composable
fun TvActions(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.padding(horizontal = TvRowInset),
        horizontalArrangement = Arrangement.spacedBy(TvRowGap),
        content = content,
    )
}

/** One of them: the quiet plate off the focus and the white one under it, as every row here is. */
@Composable
fun TvAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = TvActionHeight),
        contentPadding = PaddingValues(horizontal = TvActionInset),
        shape = ButtonDefaults.shape(MaterialTheme.shapes.medium),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
    }
}

val TvDialogWidth = 560.dp

/** Reachable from across the room, which is what the platform asks of anything answerable. */
private val TvActionHeight = 48.dp

/** Twice the inset of a row, so a single word still reads as something to press. */
private val TvActionInset = 24.dp
