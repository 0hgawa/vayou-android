package dev.vayou.feature.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouDialog
import dev.vayou.core.ui.designsystem.components.VayouTextButton

/**
 * What went wrong, said in a way that leaves somewhere to go.
 *
 * A dialog and not a message on the frame: a file that will not open leaves a black screen, and a
 * black screen with a line of text on it is indistinguishable from a film that opens on black.
 *
 * Retry first, because the common causes pass -- a drive that had not woken, a share that had not
 * finished writing. Leaving is the other answer, and there is no third: with no queue yet there is
 * nothing to skip to.
 */
@UnstableApi
@Composable
fun PlayerErrorDialog(error: PlaybackException, onRetry: () -> Unit, onLeave: () -> Unit) {
    VayouDialog(
        // No dismiss on a tap outside. Behind this is a screen showing nothing, and closing onto it
        // would look like the app had simply stopped.
        onDismissRequest = {},
        title = stringResource(R.string.playback_failed),
        confirmButton = { VayouConfirmButton(text = stringResource(R.string.try_again), onClick = onRetry) },
        dismissButton = {
            VayouTextButton(onClick = onLeave) { Text(text = stringResource(R.string.close)) }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Text(text = error.humanCause())
    }
}

/**
 * The exception's own message names an internal class as often as it names a cause, so the codes
 * worth telling apart are spelled out and everything else falls back to the one honest sentence.
 */
@UnstableApi
@Composable
private fun PlaybackException.humanCause(): String = stringResource(
    when (errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        -> R.string.playback_failed_missing

        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> R.string.playback_failed_network

        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        -> R.string.playback_failed_format

        else -> R.string.playback_failed_unknown
    },
)
