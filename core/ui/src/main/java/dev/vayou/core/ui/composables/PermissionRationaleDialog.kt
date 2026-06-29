package dev.vayou.core.ui.composables

import android.Manifest
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouDialog
import dev.vayou.core.ui.preview.DayNightPreview
import dev.vayou.core.ui.theme.VayouPlayerTheme

@Composable
fun PermissionRationaleDialog(
    text: String,
    modifier: Modifier = Modifier,
    onConfirmButtonClick: () -> Unit,
) {
    VayouDialog(
        onDismissRequest = {},
        modifier = modifier,
        title = { Text(text = stringResource(R.string.permission_request)) },
        content = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onConfirmButtonClick) {
                Text(stringResource(R.string.grant_permission))
            }
        },
    )
}

@DayNightPreview
@Composable
fun PermissionRationaleDialogPreview() {
    VayouPlayerTheme {
        Surface {
            PermissionRationaleDialog(
                text = stringResource(
                    id = R.string.permission_info,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ),
                onConfirmButtonClick = {},
            )
        }
    }
}
