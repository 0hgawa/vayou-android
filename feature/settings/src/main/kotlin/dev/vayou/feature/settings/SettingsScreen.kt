package dev.vayou.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.DefaultSubtitleTextSize
import dev.vayou.core.model.ThemeConfig
import dev.vayou.core.model.ThumbnailGenerationStrategy
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.ListSectionTitle
import dev.vayou.core.ui.designsystem.components.PreferenceGroup
import dev.vayou.core.ui.designsystem.components.PreferenceItem
import dev.vayou.core.ui.designsystem.components.PreferenceSlider
import dev.vayou.core.ui.designsystem.components.PreferenceSwitch
import dev.vayou.core.ui.designsystem.components.PreferenceSwitchWithDivider
import dev.vayou.core.ui.designsystem.components.SelectablePreference
import dev.vayou.core.ui.designsystem.components.SingleSelectablePreference
import dev.vayou.core.ui.designsystem.components.VayouBackButton
import dev.vayou.core.ui.designsystem.components.VayouCircularProgress
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouListItemInset
import dev.vayou.core.ui.designsystem.components.VayouScaffold
import dev.vayou.core.ui.designsystem.components.VayouSelectableTile
import dev.vayou.core.ui.designsystem.components.VayouSelectableTileSpacing
import dev.vayou.core.ui.designsystem.components.VayouTopAppBar
import dev.vayou.core.ui.theme.supportsDynamicColors

/**
 * Where in the settings the reader is.
 *
 * A stack of pages rather than one long scroll: the settings that matter on any given day are three
 * or four, and finding them among a hundred rows is worse than one tap into the right dozen.
 *
 * [Folders] and [Thumbnails] are reached from inside [MediaLibrary] rather than from the root, which
 * is why they are here and not in [RootPages].
 */
private enum class SettingsPage(@param:StringRes val title: Int) {
    Root(R.string.settings),
    Appearance(R.string.settings_appearance),
    MediaLibrary(R.string.settings_media_library),
    Folders(R.string.settings_manage_folders),
    Thumbnails(R.string.settings_thumbnail_generation),
    Player(R.string.settings_player),
    Gestures(R.string.settings_gestures),
    Decoder(R.string.settings_decoder),
    Audio(R.string.settings_audio),
    Subtitles(R.string.settings_subtitles),
    General(R.string.settings_general),
    About(R.string.settings_about),
}

/** The rows of the root, in the order the app being replaced puts them. */
private val RootPages = listOf(
    SettingsPage.Appearance to VayouIcons.Appearance,
    SettingsPage.MediaLibrary to VayouIcons.Video,
    SettingsPage.Player to VayouIcons.Player,
    SettingsPage.Gestures to VayouIcons.SwipeHorizontal,
    SettingsPage.Decoder to VayouIcons.Decoder,
    SettingsPage.Audio to VayouIcons.Audio,
    SettingsPage.Subtitles to VayouIcons.Subtitle,
    SettingsPage.General to VayouIcons.ExtraSettings,
    SettingsPage.About to VayouIcons.Info,
)

private val SettingsPage.description: Int
    @StringRes get() = when (this) {
        SettingsPage.Appearance -> R.string.settings_appearance_description
        SettingsPage.MediaLibrary -> R.string.settings_media_library_description
        SettingsPage.Player -> R.string.settings_player_description
        SettingsPage.Gestures -> R.string.settings_gestures_description
        SettingsPage.Decoder -> R.string.settings_decoder_description
        SettingsPage.Audio -> R.string.settings_audio_description
        SettingsPage.Subtitles -> R.string.settings_subtitles_description
        SettingsPage.General -> R.string.settings_general_description
        SettingsPage.About -> R.string.settings_about_description
        // Never a row of the root, so never asked for a line under one.
        SettingsPage.Root, SettingsPage.Folders, SettingsPage.Thumbnails -> R.string.settings
    }

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    // A list rather than one value, so a page reached from another goes back to the one it came
    // from. Manage folders is two deep, and popping to the root would drop a level.
    var stack by rememberSaveable { mutableStateOf(listOf(SettingsPage.Root)) }
    val page = stack.last()
    val open: (SettingsPage) -> Unit = { stack = stack + it }
    val back = { stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1) { back() }

    VayouScaffold(
        topBar = {
            VayouTopAppBar(
                title = stringResource(page.title),
                // No arrow at the root: it is one of the four tabs, and none of them has anywhere
                // to go back to.
                navigationIcon = { if (stack.size > 1) VayouBackButton(onClick = back) },
            )
        },
    ) {
        when (page) {
            SettingsPage.Root -> SettingsColumn {
                RootPages.forEach { (target, icon) ->
                    PreferenceItem(
                        title = stringResource(target.title),
                        description = stringResource(target.description),
                        icon = icon,
                        onClick = { open(target) },
                    )
                }
            }

            SettingsPage.Appearance -> AppearanceSettings(viewModel)
            SettingsPage.MediaLibrary -> MediaLibrarySettings(
                viewModel = viewModel,
                onManageFolders = { open(SettingsPage.Folders) },
                onThumbnails = { open(SettingsPage.Thumbnails) },
            )
            SettingsPage.Folders -> FolderSettings(viewModel)
            SettingsPage.Thumbnails -> ThumbnailSettings(viewModel)
            SettingsPage.Player -> PlayerSettings(viewModel, isPipSupported = isPipSupported())
            SettingsPage.Gestures -> GestureSettings(viewModel)
            SettingsPage.Decoder -> DecoderSettings(viewModel)
            SettingsPage.Audio -> AudioSettings(viewModel)
            SettingsPage.Subtitles -> SubtitleSettings(viewModel)
            SettingsPage.General -> GeneralSettings(viewModel)
            SettingsPage.About -> AboutSettings()
        }
    }
}

/** True where the system has a floating window to shrink into. */
@Composable
private fun isPipSupported(): Boolean = LocalContext.current.packageManager
    .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

@Composable
internal fun SettingsColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = BottomPadding),
    ) {
        content()
    }
}

@Composable
private fun AppearanceSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.application.collectAsStateWithLifecycle()

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_theme))
        // The four in front of the reader rather than behind a row that opens a dialog. One is
        // always filled, which is the point: the setting has no off state, so a switch has to
        // invent one and gets it wrong -- "system" reading as unchecked while the app sits there in
        // the dark.
        //
        // Two by two, not four across: a quarter of a phone's width leaves each tile narrower than
        // the word in it. On the margin the rows below use, so the block lines up with them.
        val current = ThemeOption.of(preferences.themeConfig, preferences.useHighContrastDarkTheme)
        Column(
            modifier = Modifier
                .padding(horizontal = VayouListItemInset)
                // The gap a section title opens above itself, closing this block the way the next
                // one will open. Without it the last tile and the switch under it read as one run.
                .padding(bottom = ThemeBlockGap),
            verticalArrangement = Arrangement.spacedBy(VayouSelectableTileSpacing),
        ) {
            ThemeOption.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(VayouSelectableTileSpacing)) {
                    pair.forEach { option ->
                        VayouSelectableTile(
                            label = stringResource(option.label),
                            icon = option.icon,
                            selected = option == current,
                            onClick = {
                                viewModel.updateApplication {
                                    copy(
                                        themeConfig = option.config,
                                        useHighContrastDarkTheme = option.isHighContrast,
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Hidden where it would do nothing rather than sitting there doing nothing.
        if (supportsDynamicColors()) {
            PreferenceGroup {
                PreferenceSwitch(
                    title = stringResource(R.string.settings_dynamic_colour),
                    description = stringResource(R.string.settings_dynamic_colour_description),
                    icon = VayouIcons.Appearance,
                    isChecked = preferences.useDynamicColors,
                    onClick = { viewModel.updateApplication { copy(useDynamicColors = !useDynamicColors) } },
                )
            }
        }
    }
}

@Composable
private fun MediaLibrarySettings(viewModel: SettingsViewModel, onManageFolders: () -> Unit, onThumbnails: () -> Unit) {
    val preferences by viewModel.application.collectAsStateWithLifecycle()

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_general))
        PreferenceGroup {
            PreferenceSwitch(
                title = stringResource(R.string.settings_mark_watched),
                description = stringResource(R.string.settings_mark_watched_description),
                icon = VayouIcons.Check,
                isChecked = preferences.markLastPlayedMedia,
                onClick = { viewModel.updateApplication { copy(markLastPlayedMedia = !markLastPlayedMedia) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_recent),
                description = stringResource(R.string.settings_recent_description),
                icon = VayouIcons.History,
                isChecked = preferences.showRecentVideos,
                onClick = { viewModel.updateApplication { copy(showRecentVideos = !showRecentVideos) } },
            )
        }

        ListSectionTitle(text = stringResource(R.string.settings_scan))
        PreferenceGroup {
            PreferenceItem(
                title = stringResource(R.string.settings_manage_folders),
                description = stringResource(R.string.settings_manage_folders_description),
                icon = VayouIcons.FolderOff,
                onClick = onManageFolders,
            )
            PreferenceItem(
                title = stringResource(R.string.settings_rescan),
                description = stringResource(R.string.settings_rescan_description),
                icon = VayouIcons.Refresh,
                onClick = viewModel::rescanLibrary,
            )
        }

        ListSectionTitle(text = stringResource(R.string.settings_thumbnails))
        PreferenceGroup {
            PreferenceItem(
                title = stringResource(R.string.settings_thumbnail_generation),
                description = stringResource(preferences.thumbnailGenerationStrategy.label),
                icon = VayouIcons.Image,
                onClick = onThumbnails,
            )
        }
    }
}

/**
 * Which folders the library leaves out.
 *
 * Everything it found, with the excluded ones struck through: a list of what is being left out says
 * nothing about what there was to leave out, and the question being answered here is which of the
 * folders on this phone belong in the library.
 */
@Composable
private fun FolderSettings(viewModel: SettingsViewModel) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val preferences by viewModel.application.collectAsStateWithLifecycle()

    val found = folders
    if (found == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            VayouCircularProgress(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(found, key = { it.path }) { folder ->
            SelectablePreference(
                title = folder.name,
                description = folder.path,
                selected = folder.path in preferences.excludeFolders,
                onClick = { viewModel.toggleExcludedFolder(folder.path) },
            )
        }
    }
}

@Composable
private fun ThumbnailSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.application.collectAsStateWithLifecycle()
    val isPositionUsed = preferences.thumbnailGenerationStrategy != ThumbnailGenerationStrategy.FIRST_FRAME

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_thumbnail_strategy))
        PreferenceGroup(modifier = Modifier.selectableGroup()) {
            ThumbnailGenerationStrategy.entries.forEach { strategy ->
                SingleSelectablePreference(
                    title = stringResource(strategy.label),
                    description = stringResource(strategy.description),
                    selected = strategy == preferences.thumbnailGenerationStrategy,
                    onClick = { viewModel.updateApplication { copy(thumbnailGenerationStrategy = strategy) } },
                )
            }
        }

        ListSectionTitle(text = stringResource(R.string.settings_position))
        PreferenceGroup {
            PreferenceSlider(
                title = stringResource(R.string.settings_frame_position),
                description = stringResource(
                    R.string.settings_percent,
                    (preferences.thumbnailFramePosition * 100).toInt(),
                ),
                icon = VayouIcons.Frame,
                // The first frame is the first frame wherever the slider sits.
                enabled = isPositionUsed,
                value = preferences.thumbnailFramePosition,
                onValueChange = { viewModel.updateApplication { copy(thumbnailFramePosition = it) } },
                trailingContent = {
                    VayouIconButton(
                        enabled = isPositionUsed,
                        onClick = {
                            viewModel.updateApplication {
                                copy(thumbnailFramePosition = ApplicationPreferences.DEFAULT_THUMBNAIL_FRAME_POSITION)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = VayouIcons.History,
                            contentDescription = stringResource(R.string.settings_reset),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SubtitleSettings(viewModel: SettingsViewModel) {
    val preferences by viewModel.player.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Everything below is the app's own styling, which the system's overrides wholesale.
    val isAppStyled = !preferences.useSystemCaptionStyle

    SettingsColumn {
        ListSectionTitle(text = stringResource(R.string.settings_style))
        PreferenceGroup {
            PreferenceSwitchWithDivider(
                title = stringResource(R.string.settings_system_captions),
                description = stringResource(R.string.settings_system_captions_description),
                icon = VayouIcons.Caption,
                isChecked = preferences.useSystemCaptionStyle,
                onCheckedChange = { viewModel.updatePlayer { copy(useSystemCaptionStyle = !useSystemCaptionStyle) } },
                onClick = { context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS)) },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_subtitle_bold),
                description = stringResource(R.string.settings_subtitle_bold_description),
                icon = VayouIcons.Bold,
                enabled = isAppStyled,
                isChecked = preferences.subtitleTextBold,
                onClick = { viewModel.updatePlayer { copy(subtitleTextBold = !subtitleTextBold) } },
            )
            PreferenceSlider(
                title = stringResource(R.string.settings_subtitle_size),
                description = preferences.subtitleTextSize.toString(),
                icon = VayouIcons.FontSize,
                enabled = isAppStyled,
                value = preferences.subtitleTextSize.toFloat(),
                valueRange = MinSubtitleTextSize..MaxSubtitleTextSize,
                onValueChange = { viewModel.updatePlayer { copy(subtitleTextSize = it.toInt()) } },
                trailingContent = {
                    VayouIconButton(
                        enabled = isAppStyled,
                        onClick = { viewModel.updatePlayer { copy(subtitleTextSize = DefaultSubtitleTextSize) } },
                    ) {
                        Icon(
                            imageVector = VayouIcons.History,
                            contentDescription = stringResource(R.string.settings_reset),
                        )
                    }
                },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_subtitle_background),
                description = stringResource(R.string.settings_subtitle_background_description),
                icon = VayouIcons.Background,
                enabled = isAppStyled,
                isChecked = preferences.subtitleBackground,
                onClick = { viewModel.updatePlayer { copy(subtitleBackground = !subtitleBackground) } },
            )
            PreferenceSwitch(
                title = stringResource(R.string.settings_embedded_styles),
                description = stringResource(R.string.settings_embedded_styles_description),
                icon = VayouIcons.Style,
                isChecked = preferences.applyEmbeddedStyles,
                onClick = { viewModel.updatePlayer { copy(applyEmbeddedStyles = !applyEmbeddedStyles) } },
            )
        }
    }
}

/**
 * The four looks, as one choice.
 *
 * Stored as two preferences and deliberately: `themeConfig` is a three-way choice and
 * `useHighContrastDarkTheme` a switch on top of it. Folding them into one enum here rather than in
 * the model keeps what is on disk readable -- the choice a reader makes is one thing, how it is
 * filed is another.
 */
private enum class ThemeOption(
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val config: ThemeConfig,
    val isHighContrast: Boolean,
) {
    System(R.string.settings_theme_system, VayouIcons.Settings, ThemeConfig.SYSTEM, false),
    Light(R.string.settings_theme_light, VayouIcons.Brightness, ThemeConfig.OFF, false),
    Dark(R.string.settings_theme_dark, VayouIcons.DarkMode, ThemeConfig.ON, false),
    Black(R.string.settings_theme_black, VayouIcons.Contrast, ThemeConfig.ON, true),
    ;

    companion object {
        /**
         * Total by construction: every pair of stored values lands on exactly one option, including
         * the pair no option writes -- follow the system, and use black when it turns dark -- which
         * an older build could leave behind. That one reads as [System], which is how the app draws
         * it, so what is shown and what is drawn cannot disagree.
         */
        fun of(config: ThemeConfig, isHighContrast: Boolean): ThemeOption = when {
            config == ThemeConfig.OFF -> Light
            config == ThemeConfig.SYSTEM -> System
            isHighContrast -> Black
            else -> Dark
        }
    }
}

private val ThumbnailGenerationStrategy.label: Int
    @StringRes get() = when (this) {
        ThumbnailGenerationStrategy.FIRST_FRAME -> R.string.settings_frame_first
        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE -> R.string.settings_frame_at
        ThumbnailGenerationStrategy.HYBRID -> R.string.settings_frame_hybrid
    }

private val ThumbnailGenerationStrategy.description: Int
    @StringRes get() = when (this) {
        ThumbnailGenerationStrategy.FIRST_FRAME -> R.string.settings_frame_first_description
        ThumbnailGenerationStrategy.FRAME_AT_PERCENTAGE -> R.string.settings_frame_at_description
        ThumbnailGenerationStrategy.HYBRID -> R.string.settings_frame_hybrid_description
    }

private const val MinSubtitleTextSize = 10f

private const val MaxSubtitleTextSize = 60f

/** Matches the space a section title holds above itself, so two blocks part by the same distance. */
private val ThemeBlockGap = 20.dp

private val BottomPadding = 16.dp
