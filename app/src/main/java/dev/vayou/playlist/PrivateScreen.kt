package dev.vayou.playlist

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.MediaLayoutMode
import dev.vayou.core.model.Sort
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouEmptyState
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.feature.videopicker.state.rememberSelectionManager

@Composable
fun PrivateScreen(
    onNavigateUp: () -> Unit,
    onPlayVideos: (List<Uri>) -> Unit,
    viewModel: PrivateViewModel = hiltViewModel(),
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val selectionManager = rememberSelectionManager()
    val selectedCount = selectionManager.selectedVideos.size

    var sort by remember { mutableStateOf(Sort(Sort.By.TITLE, Sort.Order.ASCENDING)) }
    val sortedVideos = remember(videos, sort) { videos.sortedWith(sort.videoComparator()) }
    var showSortSheet by remember { mutableStateOf(false) }

    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) return@LaunchedEffect

        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val canAuthenticate = BiometricManager.from(context).canAuthenticate(authenticators)

        if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
            canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        ) {
            isUnlocked = true
            return@LaunchedEffect
        }

        val activity = context as FragmentActivity
        val prompt = BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isUnlocked = true
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onNavigateUp()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.private_section))
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    if (!isUnlocked) return

    BackHandler(enabled = selectionManager.isInSelectionMode) {
        selectionManager.exitSelectionMode()
    }

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = if (selectionManager.isInSelectionMode) "" else stringResource(R.string.private_section),
                navigationIcon = {
                    if (selectionManager.isInSelectionMode) {
                        SelectionNavigationIcon(
                            selectedCount = selectedCount,
                            totalCount = videos.size,
                            onExit = { selectionManager.exitSelectionMode() },
                        )
                    } else {
                        VayouIconButton(onClick = onNavigateUp) {
                            Icon(VayouIcons.ArrowBack, contentDescription = stringResource(R.string.navigate_up))
                        }
                    }
                },
                actions = {
                    if (selectionManager.isInSelectionMode) {
                        SelectionModeActions(
                            removeIcon = VayouIcons.Lock,
                            removeContentDescription = stringResource(R.string.remove_from_private),
                            selectedCount = selectedCount,
                            totalCount = videos.size,
                            onPlay = {
                                onPlayVideos(selectionManager.allSelectedVideos.map { Uri.parse(it.uriString) })
                                selectionManager.clearSelection()
                            },
                            onRemove = {
                                viewModel.removeVideos(selectionManager.allSelectedVideos.map { it.uriString })
                                selectionManager.clearSelection()
                            },
                            onToggleSelectAll = {
                                if (selectedCount != videos.size) {
                                    videos.forEach { selectionManager.selectVideo(it) }
                                } else {
                                    selectionManager.clearSelection()
                                }
                            },
                        )
                    } else {
                        LayoutToggleButton(
                            gridMode = preferences.mediaLayoutMode == MediaLayoutMode.GRID,
                            onToggle = viewModel::toggleLayoutMode,
                        )
                        SortButton(onClick = { showSortSheet = true })
                    }
                },
            )
        },
    ) {
        if (videos.isEmpty()) {
            VayouEmptyState(
                icon = VayouIcons.Lock,
                title = stringResource(R.string.no_private_videos_yet),
            )
        } else {
            PlaylistVideoGrid(
                videos = sortedVideos,
                preferences = preferences,
                selectionManager = selectionManager,
                origin = PlaylistVideoOrigin.Private,
                onPlay = { uri -> onPlayVideos(listOf(uri)) },
                onRemove = { viewModel.removeVideo(it.uriString) },
            )
        }
    }

    if (showSortSheet) {
        SortSheet(sort = sort, onSortChange = { sort = it }, onDismiss = { showSortSheet = false })
    }
}
