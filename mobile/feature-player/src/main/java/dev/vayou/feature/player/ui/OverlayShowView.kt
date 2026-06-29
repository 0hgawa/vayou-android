package dev.vayou.feature.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import dev.vayou.core.data.models.OpenSubtitleResult
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.VideoContentScale
import dev.vayou.core.player.OnlineSubtitleSearchState
import dev.vayou.core.player.model.SubtitleStyleState
import dev.vayou.feature.player.extensions.noRippleClickable
import dev.vayou.feature.player.state.EqualizerState
import dev.vayou.feature.player.state.SleepTimerState
import dev.vayou.core.player.state.SubtitleOptionsEvent

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayView?,
    videoContentScale: VideoContentScale,
    playerPreferences: PlayerPreferences,
    realtimeTranslationEnabled: Boolean,
    realtimeTranslationLanguage: String,
    onlineSubtitleState: OnlineSubtitleSearchState,
    equalizerState: EqualizerState? = null,
    sleepTimerState: SleepTimerState,
    onDismiss: () -> Unit = {},
    onSelectSubtitleClick: () -> Unit = {},
    onOpenSubtitleStyle: () -> Unit = {},
    onOpenSubtitleSearchOnline: () -> Unit = {},
    onOpenTranslationLanguage: () -> Unit = {},
    onBackToSubtitleSelector: () -> Unit = {},
    onRealtimeTranslationToggle: (Boolean) -> Unit = {},
    onRealtimeTranslationLanguageChange: (String) -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onSubtitleStyleChange: (SubtitleStyleState) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onOnlineSubtitleQueryChange: (String) -> Unit = {},
    onOnlineSubtitleLanguageChange: (String) -> Unit = {},
    onOnlineSubtitleSearch: () -> Unit = {},
    onOnlineSubtitleResultClick: (OpenSubtitleResult, Int) -> Unit = { _, _ -> },
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (overlayView != null) {
                    Modifier.noRippleClickable(onClick = onDismiss)
                } else {
                    Modifier
                },
            ),
    )

    AudioTrackSelectorView(
        show = overlayView == OverlayView.AUDIO_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayView.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onCustomizeClick = onOpenSubtitleStyle,
        onSearchOnlineClick = onOpenSubtitleSearchOnline,
        realtimeTranslationEnabled = realtimeTranslationEnabled,
        realtimeTranslationLanguage = realtimeTranslationLanguage,
        onRealtimeTranslationToggle = onRealtimeTranslationToggle,
        onTranslationLanguageClick = onOpenTranslationLanguage,
        onEvent = onSubtitleOptionEvent,
        onDismiss = onDismiss,
    )

    SubtitleStyleView(
        show = overlayView == OverlayView.SUBTITLE_STYLE,
        preferences = playerPreferences,
        onChange = onSubtitleStyleChange,
        onBack = onBackToSubtitleSelector,
    )

    OnlineSubtitleSearchView(
        show = overlayView == OverlayView.SUBTITLE_SEARCH_ONLINE,
        state = onlineSubtitleState,
        onQueryChange = onOnlineSubtitleQueryChange,
        onLanguageChange = onOnlineSubtitleLanguageChange,
        onSearch = onOnlineSubtitleSearch,
        onResultClick = onOnlineSubtitleResultClick,
        onBack = onBackToSubtitleSelector,
    )

    TranslationLanguageSelectorView(
        show = overlayView == OverlayView.TRANSLATION_LANGUAGE,
        selectedLanguage = realtimeTranslationLanguage,
        onLanguageSelected = onRealtimeTranslationLanguageChange,
        onBack = onBackToSubtitleSelector,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayView.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayView.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == OverlayView.PLAYLIST,
        player = player,
    )

    EqualizerView(
        show = overlayView == OverlayView.EQUALIZER,
        equalizerState = equalizerState,
    )

    SleepTimerView(
        show = overlayView == OverlayView.SLEEP_TIMER,
        sleepTimerState = sleepTimerState,
        onDismiss = onDismiss,
    )
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class OverlayView {
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    SUBTITLE_STYLE,
    SUBTITLE_SEARCH_ONLINE,
    TRANSLATION_LANGUAGE,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    EQUALIZER,
    SLEEP_TIMER,
}
