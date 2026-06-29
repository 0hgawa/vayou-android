package dev.vayou.settings.screens.medialibrary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.ui.R
import dev.vayou.core.ui.base.DataState
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.components.SelectablePreference
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.extensions.plus
import dev.vayou.core.ui.theme.VayouPlayerTheme
import dev.vayou.core.ui.components.VayouIconButton

@Composable
fun FolderPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: FolderPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)

    FolderPreferencesContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderPreferencesContent(
    uiState: FolderPreferencesUiState,
    onNavigateUp: () -> Unit,
    onEvent: (FolderPreferencesUiEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.manage_folders),
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
        when (uiState.foldersDataState) {
            is DataState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    VayouCircularProgress(modifier = Modifier.align(Alignment.Center))
                }
            }

            is DataState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(uiState.foldersDataState.value, key = { _, folder -> folder.path }) { index, folder ->
                        SelectablePreference(
                            title = folder.name,
                            description = folder.path,
                            selected = folder.path in uiState.preferences.excludeFolders,
                            onClick = { onEvent(FolderPreferencesUiEvent.UpdateExcludeList(folder.path)) },
                        )
                    }
                }
            }

            is DataState.Error -> Unit
        }
    }
}

@PreviewLightDark
@Composable
private fun FolderPreferencesScreenPreview() {
    VayouPlayerTheme {
        FolderPreferencesContent(
            uiState = FolderPreferencesUiState(),
            onNavigateUp = {},
            onEvent = {},
        )
    }
}
