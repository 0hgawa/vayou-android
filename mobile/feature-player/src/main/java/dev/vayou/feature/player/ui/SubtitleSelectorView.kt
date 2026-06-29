package dev.vayou.feature.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouHorizontalDivider
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.core.player.extensions.getName
import dev.vayou.core.player.state.SubtitleOptionsEvent
import dev.vayou.core.player.state.rememberSubtitleOptionsState
import dev.vayou.feature.player.state.rememberTracksState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val TRANSLATION_LANGUAGES = listOf(
    "pt" to "Português",
    "en" to "English",
    "es" to "Español",
    "fr" to "Français",
    "de" to "Deutsch",
    "it" to "Italiano",
    "ja" to "日本語",
    "zh" to "中文",
    "ko" to "한국어",
    "ar" to "العربية",
    "tr" to "Türkçe",
    "ru" to "Русский",
    "hi" to "हिन्दी",
)

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.SubtitleSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onSelectSubtitleClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    onSearchOnlineClick: () -> Unit,
    realtimeTranslationEnabled: Boolean,
    realtimeTranslationLanguage: String,
    onRealtimeTranslationToggle: (Boolean) -> Unit,
    onTranslationLanguageClick: () -> Unit,
    onEvent: (SubtitleOptionsEvent) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val subtitleTracksState = rememberTracksState(player, C.TRACK_TYPE_TEXT)
    val subtitleOptionsState = rememberSubtitleOptionsState(player, onEvent)

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.subtitle),
        maxHeightFraction = 0.60f,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = OverlayContentPadding),
        ) {
            // Track selection
            Column(modifier = Modifier.selectableGroup()) {
                RadioButtonRow(
                    selected = subtitleTracksState.tracks.none { it.isSelected },
                    text = stringResource(R.string.disable),
                    onClick = {
                        subtitleTracksState.switchTrack(-1)
                        onDismiss()
                    },
                )
                subtitleTracksState.tracks.forEachIndexed { index, track ->
                    RadioButtonRow(
                        selected = track.isSelected,
                        text = track.mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, index),
                        onClick = {
                            subtitleTracksState.switchTrack(index)
                            onDismiss()
                        },
                    )
                }
            }

            VayouHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Open local subtitle
            MenuRow(
                icon = VayouIcons.FileOpen,
                text = stringResource(R.string.open_subtitle),
                onClick = {
                    onSelectSubtitleClick()
                    onDismiss()
                },
            )

            // Search online
            MenuRow(
                icon = VayouIcons.Search,
                text = stringResource(R.string.search_online_subtitle),
                onClick = onSearchOnlineClick,
                showChevron = true,
            )

            // Realtime translation toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OverlayContentPadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = VayouIcons.Language,
                    contentDescription = null,
                    modifier = Modifier.size(VayouTheme.iconSize.md),
                    tint = VayouTheme.colors.onSurface,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.translate_subtitle),
                    style = VayouTheme.typography.bodyMedium,
                    color = VayouTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                VayouSwitch(
                    checked = realtimeTranslationEnabled,
                    onCheckedChange = onRealtimeTranslationToggle,
                )
            }

            // Translation language selector (only when translation is on)
            if (realtimeTranslationEnabled) {
                val selectedLabel = TRANSLATION_LANGUAGES.firstOrNull { it.first == realtimeTranslationLanguage }?.second
                    ?: realtimeTranslationLanguage
                MenuRow(
                    icon = VayouIcons.Globe,
                    text = selectedLabel,
                    onClick = onTranslationLanguageClick,
                    showChevron = true,
                )
            }

            // Customization
            MenuRow(
                icon = VayouIcons.Style,
                text = stringResource(R.string.subtitle_customization),
                onClick = onCustomizeClick,
                showChevron = true,
            )

            VayouHorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Delay
            DelayRow(
                value = subtitleOptionsState.delayMilliseconds,
                onValueChange = { subtitleOptionsState.setDelay(it) },
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = OverlayContentPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(VayouTheme.iconSize.md),
            tint = VayouTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (showChevron) {
            Icon(
                imageVector = VayouIcons.ChevronRight,
                contentDescription = null,
                tint = VayouTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DelayRow(
    value: Long,
    onValueChange: (Long) -> Unit,
) {
    val displayValue = if (value == 0L) "0.0" else "%.1f".format(value / 1000.0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OverlayContentPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = VayouIcons.Timer,
            contentDescription = null,
            modifier = Modifier.size(VayouTheme.iconSize.md),
            tint = VayouTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.delay),
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        VayouIconButton(
            onClick = { },
            modifier = Modifier.repeatingClickable(onClick = { onValueChange(value - 100) }),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
        }
        Text(
            text = "${displayValue}s",
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        VayouIconButton(
            onClick = { },
            modifier = Modifier.repeatingClickable(onClick = { onValueChange(value + 100) }),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
            )
        }
    }
}

private fun Modifier.repeatingClickable(
    enabled: Boolean = true,
    maxDelayMillis: Long = 200,
    minDelayMillis: Long = 5,
    delayDecayFactor: Float = .20f,
    onClick: () -> Unit,
): Modifier = composed {
    val updatedOnClick by rememberUpdatedState(onClick)

    this.pointerInput(enabled) {
        coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val heldButtonJob = launch {
                    var currentDelayMillis = maxDelayMillis
                    while (enabled && down.pressed) {
                        updatedOnClick()
                        delay(currentDelayMillis)
                        val nextMillis = currentDelayMillis - (currentDelayMillis * delayDecayFactor)
                        currentDelayMillis = nextMillis.toLong().coerceAtLeast(minDelayMillis)
                    }
                }
                waitForUpOrCancellation()
                heldButtonJob.cancel()
            }
        }
    }
}
