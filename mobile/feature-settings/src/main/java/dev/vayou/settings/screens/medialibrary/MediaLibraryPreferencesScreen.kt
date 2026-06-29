package dev.vayou.settings.screens.medialibrary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.ThumbnailGenerationStrategy
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.ListSectionTitle
import dev.vayou.core.ui.components.PreferenceGroup
import dev.vayou.core.ui.components.PreferenceSwitch
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouPlayerTheme

@Composable
fun MediaLibraryPreferencesScreen(
    onNavigateUp: () -> Unit,
    onFolderSettingClick: () -> Unit = {},
    onThumbnailSettingClick: () -> Unit = {},
    viewModel: MediaLibraryPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MediaLibraryPreferencesContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onFolderSettingClick = onFolderSettingClick,
        onThumbnailSettingClick = onThumbnailSettingClick,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaLibraryPreferencesContent(
    uiState: MediaLibraryPreferencesUiState,
    onNavigateUp: () -> Unit,
    onFolderSettingClick: () -> Unit,
    onThumbnailSettingClick: () -> Unit,
    onEvent: (MediaLibraryPreferencesUiEvent) -> Unit,
) {
    val preferences = uiState.preferences

    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.media_library),
                navigationIcon = {
                    VayouIconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = VayouIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = VayouTheme.colors.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.general_name))
            PreferenceGroup {
                PreferenceSwitch(
                    title = stringResource(id = R.string.mark_last_played_media),
                    description = stringResource(id = R.string.mark_last_played_media_desc),
                    icon = VayouIcons.Check,
                    isChecked = preferences.markLastPlayedMedia,
                    onClick = { onEvent(MediaLibraryPreferencesUiEvent.ToggleMarkLastPlayedMedia) },
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.scan))
            PreferenceGroup {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.manage_folders),
                    description = stringResource(id = R.string.manage_folders_desc),
                    icon = VayouIcons.FolderOff,
                    onClick = onFolderSettingClick,
                )
            }

            ListSectionTitle(text = stringResource(id = R.string.thumbnail))
            PreferenceGroup {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.thumbnail_generation),
                    description = when (preferences.thumbnailGenerationStrategy) {
                        ThumbnailGenerationStrategy.FIRST_FRAME -> stringResource(id = R.string.first_frame)
                        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE -> stringResource(R.string.frame_at_position)
                        ThumbnailGenerationStrategy.HYBRID -> stringResource(id = R.string.hybrid)
                    },
                    icon = VayouIcons.Image,
                    onClick = onThumbnailSettingClick,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun MediaLibraryPreferencesScreenPreview() {
    VayouPlayerTheme {
        MediaLibraryPreferencesContent(
            uiState = MediaLibraryPreferencesUiState(),
            onNavigateUp = {},
            onFolderSettingClick = {},
            onThumbnailSettingClick = {},
            onEvent = {},
        )
    }
}
