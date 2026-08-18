package dev.vayou.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import dev.vayou.core.ui.designsystem.components.VayouBottomSheet
import dev.vayou.core.ui.designsystem.components.VayouBottomSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults

/** One row of a [PlayerOptionsSheet]. */
@Immutable
data class PlayerOption(val label: String, val isSelected: Boolean, val onSelect: () -> Unit)

/**
 * A list of choices with one of them on, in the same panel every other list in the app uses.
 *
 * A sheet and not a dialog in the middle of the frame: these are picked while watching, and the
 * bottom of the screen is where a thumb already is and where the least of the picture is covered.
 * It is also the shape the music player uses for the same job, and one app should not have two.
 *
 * The rows use the palette and not the fixed white the controls use. Those lie on a frame whose
 * colours nobody chose; this lies on a surface of the app's own, like any other panel.
 */
@Composable
fun PlayerOptionsSheet(
    title: String,
    options: List<PlayerOption>,
    onDismiss: () -> Unit,
    /**
     * Under the list, for a sheet whose subject has a switch as well as a set -- the speed, which
     * carries whether the silences are skipped. Outside the scrolling part, because it is one line
     * and it should not be scrolled to.
     */
    footer: (@Composable () -> Unit)? = null,
) {
    VayouBottomSheet(onDismissRequest = onDismiss) {
        VayouBottomSheetTitle(text = title)
        OptionRows(options = options, onDismiss = onDismiss)
        footer?.let {
            Divider()
            it()
        }
        Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
    }
}

@Composable
private fun OptionRows(options: List<PlayerOption>, onDismiss: () -> Unit) {
    Column(
        // Capped rather than free: a file with twenty subtitle tracks would otherwise push the
        // sheet to the top of the screen and leave nothing of the film behind it.
        modifier = Modifier
            .heightIn(max = VayouSheetDefaults.ListMaxHeight)
            .verticalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            CheckedRow(
                text = option.label,
                isSelected = option.isSelected,
                onClick = {
                    option.onSelect()
                    onDismiss()
                },
            )
        }
    }
}
