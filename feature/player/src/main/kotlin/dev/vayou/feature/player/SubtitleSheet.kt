package dev.vayou.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vayou.core.player.ui.MediaTrack
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouSheet
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSheetRow
import dev.vayou.core.ui.designsystem.components.VayouSheetRowIcon
import dev.vayou.core.ui.designsystem.components.VayouSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.theme.VayouTheme
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Everything about subtitles, in the shape the old player used.
 *
 * Three kinds of row, kept apart by dividers, because they answer three different questions: which
 * subtitle is on, where to get another, and how the ones you have should look. Running them
 * together as one list of identical lines — which is what this was — makes twelve languages look
 * like twelve subtitle tracks.
 */
@Composable
fun SubtitleSheet(
    tracks: List<MediaTrack>,
    isOff: Boolean,
    onSelectTrack: (MediaTrack) -> Unit,
    onTurnOff: () -> Unit,
    onOpenFile: () -> Unit,
    onSearchOnline: () -> Unit,
    /** A channel has only the captions it broadcasts: there is no file to open and no title to search. */
    isLive: Boolean,
    onCustomise: () -> Unit,
    translateTo: String?,
    onTranslateToggle: (Boolean) -> Unit,
    onPickLanguage: () -> Unit,
    /** How far the captions are shifted against the sound, and how to shift them. */
    delayMs: Long,
    onDelayChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    VayouSheet(onDismissRequest = onDismiss) {
        VayouSheetTitle(text = stringResource(R.string.subtitles))

        Column(
            modifier = Modifier
                .heightIn(max = VayouSheetDefaults.ListMaxHeight)
                .verticalScroll(rememberScrollState()),
        ) {
            // Which one is on. A tick and not a radio: a ring drawn over a frame of film reads as a
            // smudge, while a tick survives any scene.
            Column(modifier = Modifier.selectableGroup()) {
                CheckedRow(
                    text = stringResource(R.string.subtitles_off),
                    isSelected = isOff,
                    onClick = {
                        onTurnOff()
                        onDismiss()
                    },
                )
                tracks.forEach { track ->
                    CheckedRow(
                        text = track.label,
                        isSelected = track.isSelected,
                        onClick = {
                            onSelectTrack(track)
                            onDismiss()
                        },
                    )
                }
            }

            Divider()

            // Where to get another.
            if (!isLive) {
                MenuRow(VayouIcons.FileOpen, stringResource(R.string.add_subtitle)) {
                    onOpenFile()
                    onDismiss()
                }
                MenuRow(VayouIcons.Search, stringResource(R.string.online_subtitles), showChevron = true) {
                    onSearchOnline()
                }
                // Inside, or a channel with nothing between the two rules draws them both.
                Divider()
            }

            // And how they should read.
            VayouSheetRow(
                text = stringResource(R.string.translate_subtitle),
                onClick = { onTranslateToggle(translateTo == null) },
                leading = { VayouSheetRowIcon(VayouIcons.Language) },
                trailing = {
                    VayouSwitch(checked = translateTo != null, onCheckedChange = onTranslateToggle)
                },
            )

            // Only once translation is on, since a language to translate into means nothing before.
            if (translateTo != null) {
                val language = TranslationLanguages.firstOrNull { it.code == translateTo }?.label ?: translateTo
                MenuRow(VayouIcons.Globe, language, showChevron = true, onClick = onPickLanguage)
            }

            MenuRow(VayouIcons.Style, stringResource(R.string.subtitle_style), showChevron = true) { onCustomise() }

            // Last, and behind its own rule. The three rows above lead somewhere; this one is the
            // only control on the sheet that acts here, while the sheet is open and the film runs
            // behind it -- which is the whole point of it being here rather than a page deeper.
            Divider()
            DelayRow(millis = delayMs, onChange = onDelayChange)

            Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
        }
    }
}

/**
 * Nudge the captions off the sound, in tenths of a second.
 *
 * Steppers and not a slider: the correction is nearly always a few tenths, and a slider wide enough
 * to reach ten seconds cannot be placed to a tenth with a thumb. Held down they repeat, faster the
 * longer they are held, so the far end is still reachable.
 *
 * The reading between them is the reset. It is the one thing you would press to put the offset back,
 * and a third button for that would sit between two that already crowd the end of the row.
 */
@Composable
private fun DelayRow(millis: Long, onChange: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = VayouSheetDefaults.RowMinHeight)
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding, vertical = VayouTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.lg),
    ) {
        VayouSheetRowIcon(VayouIcons.Timer)
        Text(
            text = stringResource(R.string.subtitle_delay),
            style = VayouTheme.typography.bodyLarge,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Stepper(VayouIcons.Remove, R.string.subtitle_delay_less) { onChange(millis - DelayStepMs) }
        Box(
            modifier = Modifier
                .width(DelayReadingWidth)
                .heightIn(min = VayouSheetDefaults.RowMinHeight)
                .clip(VayouTheme.shapes.full)
                .clickable(
                    enabled = millis != 0L,
                    onClickLabel = stringResource(R.string.reset),
                    onClick = { onChange(0L) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = millis.asDelayReading(),
                style = VayouTheme.typography.bodyLarge,
                color = VayouTheme.colors.onSurface,
            )
        }
        Stepper(VayouIcons.Add, R.string.subtitle_delay_more) { onChange(millis + DelayStepMs) }
    }
}

/**
 * One end of the delay control: a step on a tap, a run while it is held.
 *
 * The press is fed to the ripple by hand. A `clickable` would draw one for us but only fires on
 * release, and two gesture handlers on one target means whichever claims the pointer first wins --
 * so the gesture is written once and the ripple is told what it did.
 */
@Composable
private fun Stepper(icon: ImageVector, description: Int, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val presses = remember { MutableInteractionSource() }

    Icon(
        imageVector = icon,
        contentDescription = stringResource(description),
        tint = VayouTheme.colors.onSurface,
        modifier = Modifier
            .size(VayouSheetDefaults.RowMinHeight)
            .clip(VayouTheme.shapes.full)
            .indication(presses, ripple())
            .semantics { role = Role.Button }
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val press = PressInteraction.Press(down.position)
                        launch { presses.emit(press) }
                        // A tap is one step and a hold is a run: ten seconds at a tenth of a second
                        // a press is a hundred presses, and the far end has to be reachable without
                        // making them.
                        val repeat = launch {
                            var wait = FirstRepeatMs
                            while (down.pressed) {
                                step()
                                delay(wait)
                                wait = (wait * RepeatDecay).toLong().coerceAtLeast(FastestRepeatMs)
                            }
                        }
                        val cancelled = waitForUpOrCancellation() == null
                        repeat.cancel()
                        launch {
                            presses.emit(
                                if (cancelled) {
                                    PressInteraction.Cancel(press)
                                } else {
                                    PressInteraction.Release(press)
                                },
                            )
                        }
                    }
                }
            }
            .padding(StepperInset),
    )
}

/**
 * Tenths of a second, signed, in whoever's decimal mark this phone uses.
 *
 * Zero is written without a sign and the rest with one: "+0.0s" is not a thing anybody writes, and
 * the sign is what says which way the correction goes.
 */
private fun Long.asDelayReading(): String = when (this) {
    0L -> "0.0s"
    else -> "%+.1fs".format(Locale.getDefault(), this / MillisPerSecond)
}

/** A tenth of a second: the smallest step anybody can hear against a line of dialogue. */
private const val DelayStepMs = 100L

private const val MillisPerSecond = 1000.0

/** Fits "-10.0s", so the two steppers do not shuffle as the number grows a digit or a sign. */
private val DelayReadingWidth = 64.dp

/** The target is the 48dp floor; the glyph inside it is the size every other row's is. */
private val StepperInset = 14.dp

private const val FirstRepeatMs = 200L

private const val FastestRepeatMs = 20L

/** What each wait keeps of the last, so a hold runs away rather than plodding. */
private const val RepeatDecay = 0.8
