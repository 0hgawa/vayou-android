package dev.vayou.settings

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.ClickablePreferenceItem
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.components.VayouTopAppBar
import dev.vayou.core.ui.designsystem.VayouIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onItemClick: (Setting) -> Unit,
) {
    Scaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(id = R.string.settings),
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
            settingRows.forEach { row ->
                ClickablePreferenceItem(
                    title = stringResource(id = row.titleResId),
                    icon = row.icon,
                    onClick = { onItemClick(row.setting) },
                )
            }
        }
    }
}

enum class Setting {
    APPEARANCE,
    MEDIA_LIBRARY,
    PLAYER,
    GESTURES,
    DECODER,
    AUDIO,
    SUBTITLE,
    GENERAL,
    ABOUT,
}

private data class SettingRow(
    val titleResId: Int,
    val icon: ImageVector,
    val setting: Setting,
)

private val settingRows = listOf(
    SettingRow(R.string.appearance_name, VayouIcons.Appearance, Setting.APPEARANCE),
    SettingRow(R.string.media_library, VayouIcons.Movie, Setting.MEDIA_LIBRARY),
    SettingRow(R.string.player_name, VayouIcons.Player, Setting.PLAYER),
    SettingRow(R.string.gestures_name, VayouIcons.SwipeHorizontal, Setting.GESTURES),
    SettingRow(R.string.decoder, VayouIcons.Decoder, Setting.DECODER),
    SettingRow(R.string.audio, VayouIcons.Audio, Setting.AUDIO),
    SettingRow(R.string.subtitle, VayouIcons.Subtitle, Setting.SUBTITLE),
    SettingRow(R.string.general_name, VayouIcons.ExtraSettings, Setting.GENERAL),
    SettingRow(R.string.about_name, VayouIcons.Info, Setting.ABOUT),
)
