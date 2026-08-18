package dev.vayou.feature.settings

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.SingletonImageLoader
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.ListSectionTitle
import dev.vayou.core.ui.designsystem.components.PreferenceGroup
import dev.vayou.core.ui.designsystem.components.PreferenceItem
import dev.vayou.core.ui.designsystem.components.VayouCancelButton
import dev.vayou.core.ui.designsystem.components.VayouConfirmButton
import dev.vayou.core.ui.designsystem.components.VayouDialog

/**
 * The two things a viewer does to the app rather than with it: throw away what it has cached, and
 * put every setting back.
 *
 * Both behind a confirmation, because both are one tap and neither can be undone.
 */
@Composable
internal fun GeneralSettings(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    var pending: DestructiveAction? by remember { mutableStateOf(null) }

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_stored_data))
        PreferenceGroup {
            PreferenceItem(
                title = stringResource(R.string.settings_clear_thumbnails),
                description = stringResource(R.string.settings_clear_thumbnails_description),
                icon = VayouIcons.Delete,
                onClick = { pending = DestructiveAction.ClearThumbnails },
            )
            PreferenceItem(
                title = stringResource(R.string.settings_reset_all),
                description = stringResource(R.string.settings_reset_all_description),
                icon = VayouIcons.History,
                onClick = { pending = DestructiveAction.ResetSettings },
            )
        }
    }

    pending?.let { action ->
        VayouDialog(
            onDismissRequest = { pending = null },
            title = stringResource(action.title),
            confirmButton = {
                VayouConfirmButton(
                    text = stringResource(action.confirmLabel),
                    onClick = {
                        when (action) {
                            DestructiveAction.ClearThumbnails -> context.clearThumbnailCache()
                            DestructiveAction.ResetSettings -> viewModel.resetSettings()
                        }
                        pending = null
                    },
                )
            },
            dismissButton = { VayouCancelButton(onClick = { pending = null }) },
            content = { Text(text = stringResource(action.confirmation)) },
        )
    }
}

/** What version is installed, for a bug report to name. */
@Composable
internal fun AboutSettings() {
    val context = LocalContext.current
    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_about))
        PreferenceGroup {
            PreferenceItem(
                title = stringResource(R.string.settings_version),
                description = remember(context) { context.versionName() },
                icon = VayouIcons.Info,
            )
        }
    }
}

private enum class DestructiveAction(val title: Int, val confirmation: Int, val confirmLabel: Int) {
    ClearThumbnails(
        R.string.settings_clear_thumbnails,
        R.string.settings_clear_thumbnails_confirmation,
        R.string.settings_delete,
    ),
    ResetSettings(
        R.string.settings_reset_all,
        R.string.settings_reset_all_confirmation,
        R.string.settings_reset,
    ),
}

/**
 * Both halves of it. The disk is where the frames actually live, and a thumbnail still in memory
 * would go on being shown after the file behind it was thrown away.
 */
fun Context.clearThumbnailCache() {
    SingletonImageLoader.get(this).apply {
        diskCache?.clear()
        memoryCache?.clear()
    }
}

@Suppress("DEPRECATION")
private fun Context.versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
