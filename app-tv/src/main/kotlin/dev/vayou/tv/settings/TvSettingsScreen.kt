package dev.vayou.tv.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Resume
import dev.vayou.core.player.ui.asSpeedLabel
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.feature.settings.LibraryFolder
import dev.vayou.feature.settings.R as SettingsR
import dev.vayou.feature.settings.SettingsViewModel
import dev.vayou.feature.settings.clearThumbnailCache
import dev.vayou.tv.R
import dev.vayou.tv.TvAction
import dev.vayou.tv.TvActions
import dev.vayou.tv.TvCardTitleGap
import dev.vayou.tv.TvChoiceRow
import dev.vayou.tv.TvDialog
import dev.vayou.tv.TvRow
import dev.vayou.tv.TvRowGap
import dev.vayou.tv.TvRowInset
import dev.vayou.tv.TvScreenInset
import dev.vayou.tv.TvTitleInset
import dev.vayou.tv.tvTone

/**
 * Everything the viewer can set, on a television.
 *
 * The phone's own view model, unchanged: these are the same two stores, and a second copy of the
 * plumbing would be a second place for "reset" to mean something slightly different. What is here
 * and not there is the shape -- a list of headings beside the rows of the one that is open, which is
 * the only way a long settings screen is walked with four arrows.
 *
 * What is missing is missing on purpose. Brightness, orientation, picture-in-picture and every
 * swipe are settings for a pane of glass held in one hand; a television has none of those, and a row
 * offering to change one would be a row that does nothing.
 */
@Composable
fun TvSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val application by viewModel.application.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(TvSettingsSection.Playback) }

    // Where left goes from anywhere in the rows: back to the heading whose rows they are. Left to
    // geometry the focus lands on whichever heading happens to sit nearest the row it left from, so
    // a viewer four rows down "Audio" comes back out into "Subtitles" and has to find their way
    // again.
    val headingFocus = remember { FocusRequester() }

    // And where right goes from a heading: into that heading's rows. Left to geometry the first
    // heading sends the focus to whatever sits up and to the right of it, which is the bar at the
    // top of the screen -- so the settings could be walked into and not across.
    val paneFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)

    Row(modifier = Modifier.fillMaxSize()) {
        Headings(
            current = section,
            headingFocus = headingFocus,
            paneFocus = paneFocus,
            onSelect = { section = it },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = TvScreenInset),
            verticalArrangement = Arrangement.spacedBy(TvTitleInset),
        ) {
            Text(
                text = stringResource(section.label),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = TvTitleInset),
            )
            Box(modifier = Modifier.widthIn(max = PaneWidth)) {
                CompositionLocalProvider(
                    LocalHeadingFocus provides headingFocus,
                    LocalPaneFocus provides paneFocus,
                ) {
                    when (section) {
                        TvSettingsSection.Playback -> PlaybackRows(player) { transform ->
                            viewModel.updatePlayer { transform(this) }
                        }
                        TvSettingsSection.Audio -> AudioRows(player) { transform ->
                            viewModel.updatePlayer { transform(this) }
                        }
                        TvSettingsSection.Subtitles -> SubtitleRows(player) { transform ->
                            viewModel.updatePlayer { transform(this) }
                        }
                        TvSettingsSection.Library -> LibraryRows(
                            preferences = application,
                            folders = folders,
                            onChange = { transform -> viewModel.updateApplication { transform(this) } },
                            onToggleFolder = viewModel::toggleExcludedFolder,
                            onRescan = viewModel::rescanLibrary,
                        )

                        TvSettingsSection.General -> GeneralRows(viewModel::resetSettings)
                    }
                }
            }
        }
    }
}

/**
 * The headings, down the side. Walking onto one opens it: no press required.
 *
 * The way a television's own settings behave. A heading is not a thing to commit to -- it is a
 * place to look -- so making a viewer press to see what is under it doubles every step of a walk
 * whose whole purpose is looking. Pressing still works, and does the same thing.
 */
@Composable
private fun Headings(
    current: TvSettingsSection,
    headingFocus: FocusRequester,
    paneFocus: FocusRequester,
    onSelect: (TvSettingsSection) -> Unit,
) {
    LaunchedEffect(Unit) { runCatching { headingFocus.requestFocus() } }

    // Held while the viewer is off reading the rows: the heading they came from keeps a quiet plate,
    // so the screen still says which part of the settings is open. Dropped the moment the focus is
    // back on this list, or two headings would look chosen at once.
    var hasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(HeadingsWidth)
            .fillMaxHeight()
            .padding(start = TvScreenInset, end = TvTitleInset, top = TvTitleInset)
            .onFocusChanged { hasFocus = it.hasFocus },
        verticalArrangement = Arrangement.spacedBy(TvRowGap),
    ) {
        TvSettingsSection.entries.forEach { entry ->
            TvRow(
                onClick = { onSelect(entry) },
                modifier = Modifier
                    .then(if (entry == current) Modifier.focusRequester(headingFocus) else Modifier)
                    .focusProperties { right = paneFocus },
                isMarked = entry == current && !hasFocus,
            ) { isFocused ->
                // Opened by arriving rather than by pressing. Kept here rather than in a key
                // handler because it is the focus that means it, whichever way the focus arrived.
                LaunchedEffect(isFocused) { if (isFocused) onSelect(entry) }
                Icon(imageVector = entry.icon, contentDescription = null)
                Text(
                    text = stringResource(entry.label),
                    style = MaterialTheme.typography.titleSmall,
                    color = tvTone(isFocused, isStrong = entry == current),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlaybackRows(preferences: PlayerPreferences, onChange: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    val chooser = rememberChooser()
    when (chooser.open) {
        SpeedChoice -> ChoicePane(
            title = stringResource(SettingsR.string.settings_default_speed),
            options = Speeds.map { it to it.asSpeedLabel() },
            selected = preferences.defaultPlaybackSpeed,
            onChoose = { speed -> onChange { it.copy(defaultPlaybackSpeed = speed) } },
            onDismiss = chooser::close,
        )

        TimeoutChoice -> ChoicePane(
            title = stringResource(SettingsR.string.settings_controller_timeout),
            options = Timeouts.map { it to stringResource(SettingsR.string.settings_seconds, it) },
            selected = preferences.controllerAutoHideTimeout,
            onChoose = { value -> onChange { it.copy(controllerAutoHideTimeout = value) } },
            onDismiss = chooser::close,
        )

        SeekChoice -> ChoicePane(
            title = stringResource(SettingsR.string.settings_seek_increment),
            options = SeekIncrements.map { it to stringResource(SettingsR.string.settings_seconds, it) },
            selected = preferences.seekIncrement,
            onChoose = { value -> onChange { it.copy(seekIncrement = value) } },
            onDismiss = chooser::close,
        )

        DecoderChoice -> ChoicePane(
            title = stringResource(SettingsR.string.settings_decoder_priority),
            options = DecoderPriority.entries.map { it to stringResource(it.label) },
            selected = preferences.decoderPriority,
            onChoose = { value -> onChange { it.copy(decoderPriority = value) } },
            onDismiss = chooser::close,
        )

        else -> Rows {
            item {
                SwitchRow(
                    title = stringResource(SettingsR.string.settings_resume),
                    isOn = preferences.resume == Resume.YES,
                ) { onChange { it.copy(resume = if (it.resume == Resume.YES) Resume.NO else Resume.YES) } }
            }
            item {
                SettingRow(
                    title = stringResource(SettingsR.string.settings_default_speed),
                    value = preferences.defaultPlaybackSpeed.asSpeedLabel(),
                    modifier = chooser.rowModifier(SpeedChoice),
                ) { chooser.open(SpeedChoice) }
            }
            item {
                SwitchRow(stringResource(SettingsR.string.settings_autoplay), preferences.autoplay) {
                    onChange { it.copy(autoplay = !it.autoplay) }
                }
            }
            item {
                SettingRow(
                    title = stringResource(SettingsR.string.settings_controller_timeout),
                    value = stringResource(SettingsR.string.settings_seconds, preferences.controllerAutoHideTimeout),
                    modifier = chooser.rowModifier(TimeoutChoice),
                ) { chooser.open(TimeoutChoice) }
            }
            item {
                SettingRow(
                    title = stringResource(SettingsR.string.settings_seek_increment),
                    value = stringResource(SettingsR.string.settings_seconds, preferences.seekIncrement),
                    modifier = chooser.rowModifier(SeekChoice),
                ) { chooser.open(SeekChoice) }
            }
            item {
                SettingRow(
                    title = stringResource(SettingsR.string.settings_decoder_priority),
                    value = stringResource(preferences.decoderPriority.label),
                    modifier = chooser.rowModifier(DecoderChoice),
                ) { chooser.open(DecoderChoice) }
            }
        }
    }
}

@Composable
private fun AudioRows(preferences: PlayerPreferences, onChange: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    Rows {
        item {
            SwitchRow(stringResource(SettingsR.string.settings_volume_boost), preferences.enableVolumeBoost) {
                onChange { it.copy(enableVolumeBoost = !it.enableVolumeBoost) }
            }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_audio_focus), preferences.requireAudioFocus) {
                onChange { it.copy(requireAudioFocus = !it.requireAudioFocus) }
            }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_headset), preferences.pauseOnHeadsetDisconnect) {
                onChange { it.copy(pauseOnHeadsetDisconnect = !it.pauseOnHeadsetDisconnect) }
            }
        }
    }
}

@Composable
private fun SubtitleRows(preferences: PlayerPreferences, onChange: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    val chooser = rememberChooser()
    if (chooser.open == SizeChoice) {
        ChoicePane(
            title = stringResource(SettingsR.string.settings_subtitle_size),
            options = SubtitleSizes.map { it to it.toString() },
            selected = preferences.subtitleTextSize,
            onChoose = { size -> onChange { it.copy(subtitleTextSize = size) } },
            onDismiss = chooser::close,
        )
        return
    }
    Rows {
        item {
            SettingRow(
                title = stringResource(SettingsR.string.settings_subtitle_size),
                value = preferences.subtitleTextSize.toString(),
                modifier = chooser.rowModifier(SizeChoice),
            ) { chooser.open(SizeChoice) }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_subtitle_bold), preferences.subtitleTextBold) {
                onChange { it.copy(subtitleTextBold = !it.subtitleTextBold) }
            }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_subtitle_background), preferences.subtitleBackground) {
                onChange { it.copy(subtitleBackground = !it.subtitleBackground) }
            }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_embedded_styles), preferences.applyEmbeddedStyles) {
                onChange { it.copy(applyEmbeddedStyles = !it.applyEmbeddedStyles) }
            }
        }
        item {
            SwitchRow(stringResource(SettingsR.string.settings_system_captions), preferences.useSystemCaptionStyle) {
                onChange { it.copy(useSystemCaptionStyle = !it.useSystemCaptionStyle) }
            }
        }
    }
}

@Composable
private fun LibraryRows(
    preferences: ApplicationPreferences,
    folders: List<LibraryFolder>?,
    onChange: ((ApplicationPreferences) -> ApplicationPreferences) -> Unit,
    onToggleFolder: (String) -> Unit,
    onRescan: () -> Unit,
) {
    Rows {
        item {
            SwitchRow(stringResource(SettingsR.string.settings_recent), preferences.showRecentVideos) {
                onChange { it.copy(showRecentVideos = !it.showRecentVideos) }
            }
        }
        item { SettingRow(title = stringResource(SettingsR.string.settings_rescan), onClick = onRescan) }
        // A folder is in the library or out of it, and the row that says which is the row that
        // changes it. Absent until the query has answered: an empty list would read as a television
        // with no video on it.
        folders?.takeIf { it.isNotEmpty() }?.let { found ->
            item {
                Text(
                    text = stringResource(SettingsR.string.settings_manage_folders),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = TvTitleInset, bottom = TvCardTitleGap),
                )
            }
            items(found, key = LibraryFolder::path) { folder ->
                SwitchRow(
                    title = folder.name,
                    isOn = folder.path !in preferences.excludeFolders,
                ) { onToggleFolder(folder.path) }
            }
        }
    }
}

@Composable
private fun GeneralRows(onReset: () -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    Rows {
        item {
            SettingRow(title = stringResource(SettingsR.string.settings_clear_thumbnails)) {
                pending = ThumbnailsChoice
            }
        }
        item {
            SettingRow(title = stringResource(SettingsR.string.settings_reset_all)) { pending = ResetChoice }
        }
        item {
            // Asked of the system rather than compiled in: the package already knows, and a second
            // copy of the number is a second thing to forget to bump.
            SettingRow(
                title = stringResource(SettingsR.string.settings_version),
                value = context.versionName(),
                onClick = {},
            )
        }
    }

    pending?.let { action ->
        Confirm(
            message = stringResource(
                if (action == ThumbnailsChoice) {
                    SettingsR.string.settings_clear_thumbnails_confirmation
                } else {
                    SettingsR.string.settings_reset_all_confirmation
                },
            ),
            onConfirm = {
                if (action == ThumbnailsChoice) context.clearThumbnailCache() else onReset()
                pending = null
            },
            onDismiss = { pending = null },
        )
    }
}

private fun Context.versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

/**
 * A setting's values, in the pane, in place of the rows.
 *
 * Not a dialog. A dialog over a two-pane screen is a third thing on it: it has to be laid over one
 * of the panes, where it is cut off, or over both, where it hides the row being changed. The pane
 * is already the place where the answer to whichever heading is open is shown -- a list of values is
 * that same answer one level down, and back climbs out of it the way back always does.
 */
@Composable
private fun <T> ChoicePane(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onChoose: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val chosen = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { chosen.requestFocus() } }
    BackHandler(onBack = onDismiss)

    Column(verticalArrangement = Arrangement.spacedBy(TvRowGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = TvRowInset, vertical = TvCardTitleGap),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(TvRowGap)) {
            items(options) { (value, label) ->
                TvChoiceRow(
                    label = label,
                    isSelected = value == selected,
                    modifier = if (value == selected) Modifier.focusRequester(chosen) else Modifier,
                ) {
                    onChoose(value)
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Holds the row a chooser was opened from, and hands the focus back to it on the way out.
 *
 * The rows are taken apart while the values are shown, so there is nothing to remember them by
 * except the name of the setting: it goes on the row as it is rebuilt, and the focus follows.
 */
@Stable
private class Chooser {
    var open: String? by mutableStateOf(null)
        private set

    /** The setting whose row is owed the focus back, for the one frame it takes to give it. */
    var returning: String? by mutableStateOf(null)
        private set

    val focus = FocusRequester()

    fun open(setting: String) {
        open = setting
    }

    fun close() {
        returning = open
        open = null
    }

    /**
     * The requester, for the row that is owed the focus back.
     *
     * A member and not an extension on [Modifier], which is what the convention asks of anything
     * that makes one: the two things it needs -- which row is owed and the requester to hand it --
     * belong to this holder, and an extension would have to be given both at every call.
     */
    @Suppress("ModifierFactoryExtensionFunction")
    fun rowModifier(setting: String): Modifier = if (returning == setting) Modifier.focusRequester(focus) else Modifier

    fun settled() {
        returning = null
    }
}

@Composable
private fun rememberChooser(): Chooser {
    val chooser = remember { Chooser() }
    LaunchedEffect(chooser.returning) {
        if (chooser.returning == null) return@LaunchedEffect
        runCatching { chooser.focus.requestFocus() }
        chooser.settled()
    }
    return chooser
}

/** The rows of one heading, scrolled under the D-pad. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Rows(content: LazyListScope.() -> Unit) {
    LazyColumn(
        // The whole column answers to one requester, and hands the focus to the row that had it
        // last -- or to the first, coming in for the first time. A heading pointing at one
        // particular row would send a viewer back to the top of a list they were halfway down.
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(LocalPaneFocus.current ?: remember { FocusRequester() })
            .focusRestorer(),
        verticalArrangement = Arrangement.spacedBy(TvRowGap),
        contentPadding = PaddingValues(bottom = TvScreenInset),
        content = content,
    )
}

/**
 * One setting: what it is on the left, what it is set to on the right.
 *
 * The reading is on the row rather than behind it, because a viewer scanning a settings screen from
 * the sofa is checking values, not opening them one at a time to find out.
 */
@Composable
private fun SettingRow(title: String, modifier: Modifier = Modifier, value: String? = null, onClick: () -> Unit) {
    val heading = LocalHeadingFocus.current
    TvRow(
        onClick = onClick,
        modifier = modifier.then(
            if (heading == null) Modifier else Modifier.focusProperties { left = heading },
        ),
    ) { isFocused ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = tvTone(isFocused, isStrong = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = tvTone(isFocused, isStrong = false),
                maxLines = 1,
            )
        }
    }
}

/** A setting that is on or off, which is a setting whose value happens to be one of two words. */
@Composable
private fun SwitchRow(title: String, isOn: Boolean, onToggle: () -> Unit) {
    SettingRow(
        title = title,
        value = stringResource(if (isOn) R.string.on else R.string.off),
        onClick = onToggle,
    )
}

/** Two answers, and the one that undoes nothing comes first. */
@Composable
private fun Confirm(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    TvDialog(title = message, onDismiss = onDismiss) {
        TvActions {
            TvAction(stringResource(R.string.cancel), Modifier.focusRequester(first), onDismiss)
            TvAction(stringResource(R.string.confirm), onClick = onConfirm)
        }
    }
}

/** The heading of the section being read, for the rows to send the focus back to. */
private val LocalHeadingFocus = staticCompositionLocalOf<FocusRequester?> { null }

/** The rows of the section being read, for the headings to send the focus into. */
private val LocalPaneFocus = staticCompositionLocalOf<FocusRequester?> { null }

private enum class TvSettingsSection(val label: Int, val icon: ImageVector) {
    Playback(SettingsR.string.settings_playback, VayouIcons.Player),
    Audio(SettingsR.string.settings_audio, VayouIcons.Audio),
    Subtitles(SettingsR.string.settings_subtitles, VayouIcons.Subtitle),
    Library(SettingsR.string.settings_media_library, VayouIcons.VideoLibrary),
    General(SettingsR.string.settings_general, VayouIcons.Settings),
}

private val DecoderPriority.label: Int
    get() = when (this) {
        DecoderPriority.PREFER_DEVICE -> SettingsR.string.settings_decoder_device
        DecoderPriority.PREFER_APP -> SettingsR.string.settings_decoder_app
        DecoderPriority.DEVICE_ONLY -> SettingsR.string.settings_decoder_device_only
    }

/** Which question is open. A string rather than a type: only one can be, and it is never stored. */
private const val SpeedChoice = "speed"

private const val TimeoutChoice = "timeout"

private const val SeekChoice = "seek"

private const val SizeChoice = "size"

private const val DecoderChoice = "decoder"

private const val ThumbnailsChoice = "thumbnails"

private const val ResetChoice = "reset"

/** The same rungs the phone offers, so a film watched on both behaves the same on either. */
private val Speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private val Timeouts = listOf(2, 3, 4, 5, 10, 15)

private val SeekIncrements = listOf(5, 10, 15, 30)

private val SubtitleSizes = (16..32 step 2).toList()

private val HeadingsWidth = 300.dp

private val PaneWidth = 720.dp
