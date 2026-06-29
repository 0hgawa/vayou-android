package dev.vayou.tv.feature.player

import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.media3.common.MediaItem
import kotlin.math.abs
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.LineStyle
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbIncandescent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.model.Font
import dev.vayou.core.model.LoopMode
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.VideoContentScale
import dev.vayou.core.player.extensions.getManuallySelectedTrackIndex
import dev.vayou.core.player.extensions.getName
import dev.vayou.core.player.extensions.switchTrack
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme

private val PlaybackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
private val SleepTimerMinutes = listOf(15, 30, 45, 60, 90)
private val LoudnessGains = listOf(0, 500, 1000, 1500, 2000, 3000)
private val SubtitleDelaysMs = listOf(-5000L, -3000L, -1500L, -1000L, -500L, 0L, 500L, 1000L, 1500L, 3000L, 5000L)

private val SubtitleSizes = listOf(14, 16, 18, 20, 22, 24, 28, 36)
private val SubtitlePositions = listOf(-0.5f, -0.25f, 0f, 0.25f, 0.5f)

private data class NamedColor(val argb: Int, val label: String)

private val SubtitleColors = listOf(
    NamedColor(0xFFFFFFFF.toInt(), "Branco"),
    NamedColor(0xFF000000.toInt(), "Preto"),
    NamedColor(0xFFFFFF00.toInt(), "Amarelo"),
    NamedColor(0xFFFFAB00.toInt(), "Âmbar"),
    NamedColor(0xFFFF6D00.toInt(), "Laranja"),
    NamedColor(0xFFFF1744.toInt(), "Vermelho"),
    NamedColor(0xFFFF4081.toInt(), "Rosa"),
    NamedColor(0xFFD500F9.toInt(), "Roxo"),
    NamedColor(0xFF2979FF.toInt(), "Azul"),
    NamedColor(0xFF00E5FF.toInt(), "Ciano"),
    NamedColor(0xFF00E676.toInt(), "Verde"),
)

private enum class SubtitleStylePreset(
    val label: String,
    val textColor: Int,
    val outlineColor: Int,
    val background: Boolean,
    val shadow: Boolean,
    val outlineEnabled: Boolean,
    val textBold: Boolean,
) {
    LIGHT("Claro", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), true, false, false, false),
    DROP_SHADOW("Sombra", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), false, true, false, true),
    DARK("Escuro", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), false, false, true, true),
    CONTRAST("Contraste", 0xFFFFFF00.toInt(), 0xFF000000.toInt(), false, false, true, true),
    BOX("Caixa", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), true, false, false, true),
    RAISED("Relevo", 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), false, true, true, true),
}

private fun detectPreset(p: PlayerPreferences): SubtitleStylePreset? =
    SubtitleStylePreset.entries.firstOrNull {
        p.subtitleTextColor == it.textColor &&
            p.subtitleOutlineColor == it.outlineColor &&
            p.subtitleBackground == it.background &&
            p.subtitleShadow == it.shadow &&
            p.subtitleOutlineEnabled == it.outlineEnabled &&
            p.subtitleTextBold == it.textBold
    }

private fun positionLabel(value: Float): String = when {
    value <= -0.4f -> "Bem acima"
    value <= -0.15f -> "Pouco acima"
    value >= 0.4f -> "Bem abaixo"
    value >= 0.15f -> "Pouco abaixo"
    else -> "Centro"
}

private fun colorLabelFor(argb: Int): String =
    SubtitleColors.firstOrNull { it.argb == argb }?.label ?: "Personalizada"

internal val TranslationLanguages = listOf(
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
fun TvPlayerSelectorOverlay(
    mode: TvPlayerSelectorMode,
    player: Player,
    isLive: Boolean,
    currentContentScale: VideoContentScale,
    currentLoopMode: LoopMode,
    currentSleepTimerDeadline: Long?,
    currentNightMode: Boolean,
    currentLoudnessGainMb: Int,
    currentSubtitleDelayMs: Long,
    currentTranslationEnabled: Boolean,
    currentTranslationLanguage: String,
    onContentScaleChange: (VideoContentScale) -> Unit,
    onLoopModeChange: (LoopMode) -> Unit,
    onSleepTimerChange: (Int?) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onLoudnessGainChange: (Int) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onTranslationEnabledChange: (Boolean) -> Unit,
    onTranslationLanguageChange: (String) -> Unit,
    onAddExternalSubtitle: () -> Unit,
    onSwitchMode: (TvPlayerSelectorMode) -> Unit,
    onDismiss: () -> Unit,
    currentPreferences: PlayerPreferences,
    onSubtitleStyleUpdate: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
) {
    val payload = remember(
        mode,
        player.currentTracks,
        player.currentMediaItemIndex,
        player.mediaItemCount,
        currentContentScale,
        currentLoopMode,
        currentSleepTimerDeadline,
        currentNightMode,
        currentLoudnessGainMb,
        currentSubtitleDelayMs,
        currentTranslationEnabled,
        currentTranslationLanguage,
        currentPreferences,
    ) {
        when (mode) {
            TvPlayerSelectorMode.Audio -> tracksForType(player, C.TRACK_TYPE_AUDIO, "Áudio")
            TvPlayerSelectorMode.Subtitle -> subtitleActionsOptions(
                player = player,
                currentSubtitleDelayMs = currentSubtitleDelayMs,
                currentTranslationEnabled = currentTranslationEnabled,
                currentTranslationLanguage = currentTranslationLanguage,
                currentPreferences = currentPreferences,
            )
            TvPlayerSelectorMode.SubtitleStyle -> subtitleStyleOptions(currentPreferences)
            TvPlayerSelectorMode.SubtitleStyleCustom -> subtitleStyleCustomOptions(currentPreferences)
            TvPlayerSelectorMode.SubtitleSize -> subtitleSizeOptions(currentPreferences.subtitleTextSize)
            TvPlayerSelectorMode.SubtitlePosition -> subtitlePositionOptions(currentPreferences.subtitleVerticalPosition)
            TvPlayerSelectorMode.SubtitleFont -> subtitleFontOptions(currentPreferences.subtitleFont)
            TvPlayerSelectorMode.SubtitleTextColor -> subtitleColorOptions(
                currentPreferences.subtitleTextColor,
                title = "Cor do texto",
            )
            TvPlayerSelectorMode.SubtitleOutline -> subtitleToggleOptions(
                currentPreferences.subtitleOutlineEnabled,
                title = "Contorno",
            )
            TvPlayerSelectorMode.SubtitleOutlineColor -> subtitleColorOptions(
                currentPreferences.subtitleOutlineColor,
                title = "Cor do contorno",
            )
            TvPlayerSelectorMode.SubtitleShadow -> subtitleToggleOptions(
                currentPreferences.subtitleShadow,
                title = "Sombra",
            )
            TvPlayerSelectorMode.SubtitleBold -> subtitleToggleOptions(
                currentPreferences.subtitleTextBold,
                title = "Negrito",
            )
            TvPlayerSelectorMode.SubtitleBackground -> subtitleToggleOptions(
                currentPreferences.subtitleBackground,
                title = "Fundo",
            )
            TvPlayerSelectorMode.SubtitleTracks -> tracksForType(
                player,
                C.TRACK_TYPE_TEXT,
                "Selecionar legenda",
            )
            TvPlayerSelectorMode.Speed -> speedOptions(player.playbackParameters.speed)
            TvPlayerSelectorMode.Aspect -> aspectOptions(currentContentScale)
            TvPlayerSelectorMode.Loop -> loopOptions(currentLoopMode)
            TvPlayerSelectorMode.Timer -> sleepOptions(currentSleepTimerDeadline)
            TvPlayerSelectorMode.NightMode -> nightModeOptions(currentNightMode)
            TvPlayerSelectorMode.Loudness -> loudnessOptions(currentLoudnessGainMb)
            TvPlayerSelectorMode.SubtitleDelay -> subtitleDelayOptions(currentSubtitleDelayMs)
            TvPlayerSelectorMode.Translation -> translationOptions(
                currentTranslationEnabled,
                currentTranslationLanguage,
            )
            TvPlayerSelectorMode.Playlist -> playlistOptions(player)
            TvPlayerSelectorMode.More -> moreOptions(
                currentContentScale = currentContentScale,
                currentLoopMode = currentLoopMode,
                currentSleepTimerDeadline = currentSleepTimerDeadline,
                currentNightMode = currentNightMode,
                currentLoudnessGainMb = currentLoudnessGainMb,
                isLive = isLive,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.Back, Key.Escape -> {
                        onDismiss()
                        true
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = payload.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )

        val focusItemIndex = payload.options
            .indexOfFirst { it.index == payload.selectedIndex }
            .coerceAtLeast(0)

        val listState = rememberLazyListState()
        val focusRequester = remember(mode) { FocusRequester() }
        LaunchedEffect(mode) {
            listState.scrollToItem(focusItemIndex)
            runCatching { focusRequester.requestFocus() }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items = payload.options, key = { _, opt -> opt.id }) { idx, opt ->
                val isSelected = opt.index == payload.selectedIndex
                val focusModifier = if (idx == focusItemIndex) {
                    Modifier.focusRequester(focusRequester)
                } else Modifier

                OptionRow(
                    label = opt.label,
                    subLabel = opt.subLabel,
                    leadingIcon = opt.icon,
                    isSelected = isSelected,
                    showCheckGutter = opt.subMode == null && !opt.isOneShotAction,
                    modifier = focusModifier,
                    onClick = {
                        val handled = applyOption(
                            player = player,
                            mode = mode,
                            option = opt,
                            currentTranslationEnabled = currentTranslationEnabled,
                            onContentScaleChange = onContentScaleChange,
                            onLoopModeChange = onLoopModeChange,
                            onSleepTimerChange = onSleepTimerChange,
                            onNightModeChange = onNightModeChange,
                            onLoudnessGainChange = onLoudnessGainChange,
                            onSubtitleDelayChange = onSubtitleDelayChange,
                            onTranslationEnabledChange = onTranslationEnabledChange,
                            onTranslationLanguageChange = onTranslationLanguageChange,
                            onAddExternalSubtitle = onAddExternalSubtitle,
                            onSwitchMode = onSwitchMode,
                            onSubtitleStyleUpdate = onSubtitleStyleUpdate,
                        )
                        if (handled) onDismiss()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionRow(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    leadingIcon: ImageVector? = null,
    showCheckGutter: Boolean = true,
    onClick: () -> Unit,
) {
    val rowHeight = 64.dp
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth().height(rowHeight),
        shape = ClickableSurfaceDefaults.shape(VayouTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.12f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCheckGutter) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                } else {
                    Spacer(modifier = Modifier.width(VayouTheme.iconSize.sm))
                }
                Spacer(modifier = Modifier.width(VayouTheme.spacing.md))
            }
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(VayouTheme.iconSize.sm),
                )
                Spacer(modifier = Modifier.width(VayouTheme.spacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = if (isFocused) Int.MAX_VALUE else 0),
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// Sentinel negative index for the external-file subtitle action (kept disjoint from track indices).
private const val SUB_ACTION_OPEN = -100

private data class SelectorOption(
    val id: String,
    val index: Int,
    val label: String,
    val subLabel: String? = null,
    val subMode: TvPlayerSelectorMode? = null,
    val icon: ImageVector? = null,
    val isOneShotAction: Boolean = false,
)

private data class SelectorPayload(
    val title: String,
    val options: List<SelectorOption>,
    val selectedIndex: Int,
)

@OptIn(UnstableApi::class)
private fun tracksForType(
    player: Player,
    trackType: Int,
    title: String,
    extras: List<SelectorOption> = emptyList(),
): SelectorPayload {
    val trackGroups = player.currentTracks.groups.filter { it.type == trackType && it.isSupported }
    val selectedIndex = player.getManuallySelectedTrackIndex(trackType)
        ?: trackGroups.indexOfFirst { it.isSelected }
    val options = buildList {
        add(SelectorOption(id = "off-$trackType", index = -1, label = "Desativado"))
        trackGroups.forEachIndexed { idx, group ->
            add(
                SelectorOption(
                    id = "track-$trackType-$idx",
                    index = idx,
                    label = group.mediaTrackGroup.getName(trackType, idx),
                ),
            )
        }
        addAll(extras)
    }
    return SelectorPayload(title = title, options = options, selectedIndex = selectedIndex)
}

private const val SpeedEpsilon = 0.001f

private fun speedOptions(currentSpeed: Float): SelectorPayload {
    val options = PlaybackSpeeds.mapIndexed { idx, speed ->
        SelectorOption(id = "speed-$idx", index = idx, label = "${speed}x")
    }
    val selectedIndex = PlaybackSpeeds.indexOfFirst { abs(it - currentSpeed) < SpeedEpsilon }
        .coerceAtLeast(0)
    return SelectorPayload(title = "Velocidade", options = options, selectedIndex = selectedIndex)
}

private val AspectScales = listOf(VideoContentScale.BEST_FIT, VideoContentScale.STRETCH, VideoContentScale.CROP)
private val LoopModes = listOf(LoopMode.OFF, LoopMode.ONE, LoopMode.ALL)

private fun aspectOptions(current: VideoContentScale): SelectorPayload {
    val options = AspectScales.mapIndexed { idx, scale ->
        SelectorOption(id = "aspect-$idx", index = idx, label = aspectLabelFor(scale))
    }
    val selectedIndex = AspectScales.indexOf(current).coerceAtLeast(0)
    return SelectorPayload(title = "Proporção", options = options, selectedIndex = selectedIndex)
}

private fun loopOptions(current: LoopMode): SelectorPayload {
    val options = LoopModes.mapIndexed { idx, mode ->
        SelectorOption(id = "loop-$idx", index = idx, label = loopLabelFor(mode))
    }
    val selectedIndex = LoopModes.indexOf(current).coerceAtLeast(0)
    return SelectorPayload(title = "Repetição", options = options, selectedIndex = selectedIndex)
}

private fun playlistOptions(player: Player): SelectorPayload {
    val count = player.mediaItemCount
    val selectedIndex = player.currentMediaItemIndex.coerceAtLeast(0)
    val options = (0 until count).map { idx ->
        val item = player.getMediaItemAt(idx)
        SelectorOption(id = "queue-$idx", index = idx, label = "${idx + 1}. ${resolveMediaTitle(item, idx)}")
    }
    return SelectorPayload(
        title = "Lista de reprodução",
        options = options.ifEmpty {
            listOf(SelectorOption(id = "queue-empty", index = -1, label = "Nenhum vídeo na fila"))
        },
        selectedIndex = selectedIndex,
    )
}

private fun resolveMediaTitle(item: MediaItem, idx: Int): String =
    item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Vídeo ${idx + 1}"

private fun aspectLabelFor(scale: VideoContentScale): String = when (scale) {
    VideoContentScale.BEST_FIT, VideoContentScale.HUNDRED_PERCENT -> "Ajustar"
    VideoContentScale.STRETCH -> "Esticar"
    VideoContentScale.CROP -> "Cortar"
}

private fun loopLabelFor(mode: LoopMode): String = when (mode) {
    LoopMode.OFF -> "Desativado"
    LoopMode.ONE -> "Vídeo atual"
    LoopMode.ALL -> "Todos"
}

private fun moreOptions(
    currentContentScale: VideoContentScale,
    currentLoopMode: LoopMode,
    currentSleepTimerDeadline: Long?,
    currentNightMode: Boolean,
    currentLoudnessGainMb: Int,
    isLive: Boolean,
): SelectorPayload {
    val aspectLabel = aspectLabelFor(currentContentScale)
    val loopLabel = loopLabelFor(currentLoopMode)
    val timerLabel = if (currentSleepTimerDeadline == null) "Desativado" else {
        val minutes = ((currentSleepTimerDeadline - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
        "$minutes min"
    }
    val nightLabel = if (currentNightMode) "Ativado" else "Desativado"
    val loudnessLabel = if (currentLoudnessGainMb <= 0) "Desativado" else "+${currentLoudnessGainMb / 100} dB"
    val options = buildList {
        add(SelectorOption(id = "more-aspect", index = 0, label = "Proporção", subLabel = aspectLabel, subMode = TvPlayerSelectorMode.Aspect, icon = Icons.Outlined.AspectRatio))
        if (!isLive) {
            add(SelectorOption(id = "more-loop", index = 1, label = "Repetir", subLabel = loopLabel, subMode = TvPlayerSelectorMode.Loop, icon = VayouIcons.Repeat))
        }
        add(SelectorOption(id = "more-timer", index = 2, label = "Timer", subLabel = timerLabel, subMode = TvPlayerSelectorMode.Timer, icon = Icons.Outlined.Bedtime))
        add(SelectorOption(id = "more-night", index = 3, label = "Modo noturno", subLabel = nightLabel, subMode = TvPlayerSelectorMode.NightMode, icon = Icons.Outlined.ModeNight))
        add(SelectorOption(id = "more-loud", index = 4, label = "Reforço de volume", subLabel = loudnessLabel, subMode = TvPlayerSelectorMode.Loudness, icon = Icons.Outlined.GraphicEq))
    }
    return SelectorPayload(title = "Mais opções", options = options, selectedIndex = -1)
}

@OptIn(UnstableApi::class)
private fun subtitleActionsOptions(
    player: Player,
    currentSubtitleDelayMs: Long,
    currentTranslationEnabled: Boolean,
    currentTranslationLanguage: String,
    currentPreferences: PlayerPreferences,
): SelectorPayload {
    val activeTrackLabel = currentSubtitleTrackLabel(player)
    val delayLabel = subtitleDelayLabel(currentSubtitleDelayMs)
    val translationLabel = if (currentTranslationEnabled) "Ativada" else "Desativada"
    val languageLabel = TranslationLanguages.firstOrNull { it.first == currentTranslationLanguage }?.second
        ?: currentTranslationLanguage
    val styleLabel = detectPreset(currentPreferences)?.label ?: "Personalizado"
    val options = listOf(
        SelectorOption(
            id = "sub-tracks",
            index = 0,
            label = "Faixa de legenda",
            subLabel = activeTrackLabel,
            subMode = TvPlayerSelectorMode.SubtitleTracks,
            icon = VayouIcons.Caption,
        ),
        SelectorOption(
            id = "sub-open",
            index = SUB_ACTION_OPEN,
            label = "Abrir arquivo",
            icon = Icons.Outlined.FileOpen,
            isOneShotAction = true,
        ),
        SelectorOption(
            id = "sub-sync",
            index = 2,
            label = "Ajustar sincronia",
            subLabel = delayLabel,
            subMode = TvPlayerSelectorMode.SubtitleDelay,
            icon = Icons.Outlined.Schedule,
        ),
        SelectorOption(
            id = "sub-translate",
            index = 3,
            label = "Tradução em tempo real",
            subLabel = if (currentTranslationEnabled) "$translationLabel · $languageLabel" else translationLabel,
            subMode = TvPlayerSelectorMode.Translation,
            icon = Icons.Outlined.Translate,
        ),
        SelectorOption(
            id = "sub-style",
            index = 4,
            label = "Estilo",
            subLabel = styleLabel,
            subMode = TvPlayerSelectorMode.SubtitleStyle,
            icon = Icons.Outlined.Style,
        ),
    )
    return SelectorPayload(title = "Legendas", options = options, selectedIndex = -1)
}

private const val SUB_STYLE_RESET = -201

private fun subtitleStyleOptions(prefs: PlayerPreferences): SelectorPayload {
    val activePreset = detectPreset(prefs)
    val options = buildList {
        SubtitleStylePreset.entries.forEachIndexed { idx, preset ->
            add(SelectorOption(id = "style-${preset.name}", index = idx, label = preset.label))
        }
        add(
            SelectorOption(
                id = "style-custom",
                index = 0,
                label = "Personalizar...",
                subMode = TvPlayerSelectorMode.SubtitleStyleCustom,
                icon = Icons.Outlined.Tune,
            ),
        )
        add(
            SelectorOption(
                id = "style-reset",
                index = SUB_STYLE_RESET,
                label = "Restaurar padrão",
                icon = Icons.Outlined.RestartAlt,
                isOneShotAction = true,
            ),
        )
    }
    val selected = activePreset?.let { SubtitleStylePreset.entries.indexOf(it) } ?: -1
    return SelectorPayload(title = "Estilo da legenda", options = options, selectedIndex = selected)
}

private fun subtitleStyleCustomOptions(prefs: PlayerPreferences): SelectorPayload {
    val options = buildList {
        add(
            SelectorOption(
                id = "custom-size",
                index = 0,
                label = "Tamanho",
                subLabel = prefs.subtitleTextSize.toString(),
                subMode = TvPlayerSelectorMode.SubtitleSize,
                icon = Icons.Outlined.FormatSize,
            ),
        )
        add(
            SelectorOption(
                id = "custom-position",
                index = 0,
                label = "Posição vertical",
                subLabel = positionLabel(prefs.subtitleVerticalPosition),
                subMode = TvPlayerSelectorMode.SubtitlePosition,
                icon = Icons.Outlined.Height,
            ),
        )
        add(
            SelectorOption(
                id = "custom-font",
                index = 0,
                label = "Fonte",
                subLabel = fontLabel(prefs.subtitleFont),
                subMode = TvPlayerSelectorMode.SubtitleFont,
                icon = Icons.Outlined.TextFields,
            ),
        )
        add(
            SelectorOption(
                id = "custom-text-color",
                index = 0,
                label = "Cor do texto",
                subLabel = colorLabelFor(prefs.subtitleTextColor),
                subMode = TvPlayerSelectorMode.SubtitleTextColor,
                icon = Icons.Outlined.FormatColorText,
            ),
        )
        add(
            SelectorOption(
                id = "custom-bold",
                index = 0,
                label = "Negrito",
                subLabel = if (prefs.subtitleTextBold) "Ativado" else "Desativado",
                subMode = TvPlayerSelectorMode.SubtitleBold,
                icon = Icons.Outlined.FormatBold,
            ),
        )
        add(
            SelectorOption(
                id = "custom-outline",
                index = 0,
                label = "Contorno",
                subLabel = if (prefs.subtitleOutlineEnabled) "Ativado" else "Desativado",
                subMode = TvPlayerSelectorMode.SubtitleOutline,
                icon = Icons.Outlined.LineStyle,
            ),
        )
        if (prefs.subtitleOutlineEnabled) {
            add(
                SelectorOption(
                    id = "custom-outline-color",
                    index = 0,
                    label = "Cor do contorno",
                    subLabel = colorLabelFor(prefs.subtitleOutlineColor),
                    subMode = TvPlayerSelectorMode.SubtitleOutlineColor,
                    icon = Icons.Outlined.FormatColorFill,
                ),
            )
        }
        add(
            SelectorOption(
                id = "custom-shadow",
                index = 0,
                label = "Sombra",
                subLabel = if (prefs.subtitleShadow) "Ativada" else "Desativada",
                subMode = TvPlayerSelectorMode.SubtitleShadow,
                icon = Icons.Outlined.WbIncandescent,
            ),
        )
        add(
            SelectorOption(
                id = "custom-background",
                index = 0,
                label = "Fundo",
                subLabel = if (prefs.subtitleBackground) "Ativado" else "Desativado",
                subMode = TvPlayerSelectorMode.SubtitleBackground,
                icon = Icons.Outlined.Palette,
            ),
        )
    }
    return SelectorPayload(title = "Personalizar", options = options, selectedIndex = -1)
}

private fun subtitleSizeOptions(current: Int): SelectorPayload {
    val options = SubtitleSizes.mapIndexed { idx, size ->
        SelectorOption(id = "size-$idx", index = idx, label = size.toString())
    }
    val selected = SubtitleSizes.indexOf(current).coerceAtLeast(0)
    return SelectorPayload(title = "Tamanho", options = options, selectedIndex = selected)
}

private fun subtitlePositionOptions(current: Float): SelectorPayload {
    val options = SubtitlePositions.mapIndexed { idx, value ->
        SelectorOption(id = "pos-$idx", index = idx, label = positionLabel(value))
    }
    val selected = SubtitlePositions.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }.coerceAtLeast(2)
    return SelectorPayload(title = "Posição vertical", options = options, selectedIndex = selected)
}

private fun subtitleFontOptions(current: Font): SelectorPayload {
    val options = SubtitleFontOrder.mapIndexed { idx, font ->
        SelectorOption(id = "font-${font.name}", index = idx, label = fontLabel(font))
    }
    val selected = SubtitleFontOrder.indexOf(current).coerceAtLeast(0)
    return SelectorPayload(title = "Fonte", options = options, selectedIndex = selected)
}

private fun subtitleColorOptions(current: Int, title: String): SelectorPayload {
    val options = SubtitleColors.mapIndexed { idx, namedColor ->
        SelectorOption(id = "$title-$idx", index = idx, label = namedColor.label)
    }
    val selected = SubtitleColors.indexOfFirst { it.argb == current }.coerceAtLeast(0)
    return SelectorPayload(title = title, options = options, selectedIndex = selected)
}

private fun subtitleToggleOptions(current: Boolean, title: String): SelectorPayload {
    val options = listOf(
        SelectorOption(id = "$title-off", index = 0, label = "Desativado"),
        SelectorOption(id = "$title-on", index = 1, label = "Ativado"),
    )
    return SelectorPayload(title = title, options = options, selectedIndex = if (current) 1 else 0)
}

private fun translationOptions(enabled: Boolean, currentCode: String): SelectorPayload {
    val options = buildList {
        add(SelectorOption(id = "trans-off", index = -1, label = "Desativada"))
        TranslationLanguages.forEachIndexed { idx, (code, label) ->
            add(SelectorOption(id = "trans-lang-$code", index = idx, label = label))
        }
    }
    val selectedIndex = if (!enabled) -1 else {
        TranslationLanguages.indexOfFirst { it.first == currentCode }.coerceAtLeast(0)
    }
    return SelectorPayload(
        title = "Tradução",
        options = options,
        selectedIndex = selectedIndex,
    )
}

@OptIn(UnstableApi::class)
private fun currentSubtitleTrackLabel(player: Player): String {
    val trackGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
    val manualIdx = player.getManuallySelectedTrackIndex(C.TRACK_TYPE_TEXT)
    if (manualIdx == -1) return "Desativado"
    val activeIdx = manualIdx ?: trackGroups.indexOfFirst { it.isSelected }.takeIf { it >= 0 }
    val group = activeIdx?.let { trackGroups.getOrNull(it) } ?: return "Desativado"
    return group.mediaTrackGroup.getName(C.TRACK_TYPE_TEXT, activeIdx)
}

private fun subtitleDelayLabel(ms: Long): String = when {
    ms == 0L -> "Sem atraso"
    ms > 0 -> "+${ms / 1000f}s"
    else -> "${ms / 1000f}s"
}

private fun nightModeOptions(current: Boolean): SelectorPayload {
    val options = listOf(
        SelectorOption(id = "night-off", index = 0, label = "Desativado"),
        SelectorOption(id = "night-on", index = 1, label = "Ativado"),
    )
    return SelectorPayload(title = "Modo noturno", options = options, selectedIndex = if (current) 1 else 0)
}

private fun loudnessOptions(currentMb: Int): SelectorPayload {
    val options = LoudnessGains.mapIndexed { idx, mb ->
        val label = if (mb == 0) "Desativado" else "+${mb / 100} dB"
        SelectorOption(id = "loud-$idx", index = idx, label = label)
    }
    val selectedIndex = LoudnessGains.indexOfFirst { it == currentMb }.coerceAtLeast(0)
    return SelectorPayload(title = "Reforço de volume", options = options, selectedIndex = selectedIndex)
}

private fun subtitleDelayOptions(currentMs: Long): SelectorPayload {
    val options = SubtitleDelaysMs.mapIndexed { idx, ms ->
        SelectorOption(id = "delay-$idx", index = idx, label = subtitleDelayLabel(ms))
    }
    val selectedIndex = SubtitleDelaysMs.indexOfFirst { it == currentMs }.coerceAtLeast(0)
    return SelectorPayload(title = "Delay da legenda", options = options, selectedIndex = selectedIndex)
}

private fun sleepOptions(currentDeadline: Long?): SelectorPayload {
    val options = buildList {
        add(SelectorOption(id = "timer-off", index = -1, label = "Desativado"))
        SleepTimerMinutes.forEachIndexed { idx, minutes ->
            add(SelectorOption(id = "timer-$idx", index = idx, label = "$minutes min"))
        }
    }
    val selectedIndex = currentDeadline?.let {
        val remaining = ((it - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0).toInt()
        SleepTimerMinutes.indexOfFirst { preset -> preset >= remaining }
            .takeIf { idx -> idx >= 0 }
            ?: SleepTimerMinutes.lastIndex
    } ?: -1
    return SelectorPayload(title = "Timer", options = options, selectedIndex = selectedIndex)
}

@OptIn(UnstableApi::class)
private fun applyOption(
    player: Player,
    mode: TvPlayerSelectorMode,
    option: SelectorOption,
    currentTranslationEnabled: Boolean,
    onContentScaleChange: (VideoContentScale) -> Unit,
    onLoopModeChange: (LoopMode) -> Unit,
    onSleepTimerChange: (Int?) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onLoudnessGainChange: (Int) -> Unit,
    onSubtitleDelayChange: (Long) -> Unit,
    onTranslationEnabledChange: (Boolean) -> Unit,
    onTranslationLanguageChange: (String) -> Unit,
    onAddExternalSubtitle: () -> Unit,
    onSwitchMode: (TvPlayerSelectorMode) -> Unit,
    onSubtitleStyleUpdate: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
): Boolean {
    option.subMode?.let { target ->
        onSwitchMode(target)
        return false
    }
    when (mode) {
        TvPlayerSelectorMode.Audio -> player.switchTrack(C.TRACK_TYPE_AUDIO, option.index)
        TvPlayerSelectorMode.Subtitle -> when (option.index) {
            SUB_ACTION_OPEN -> onAddExternalSubtitle()
            else -> return false // Submenu entries handled above via subMode; others are no-ops.
        }
        TvPlayerSelectorMode.SubtitleTracks -> player.switchTrack(C.TRACK_TYPE_TEXT, option.index)
        TvPlayerSelectorMode.Speed -> {
            val speed = PlaybackSpeeds.getOrNull(option.index) ?: 1f
            player.setPlaybackSpeed(speed)
        }
        TvPlayerSelectorMode.Aspect -> {
            onContentScaleChange(AspectScales.getOrElse(option.index) { VideoContentScale.BEST_FIT })
        }
        TvPlayerSelectorMode.Loop -> {
            onLoopModeChange(LoopModes.getOrElse(option.index) { LoopMode.OFF })
        }
        TvPlayerSelectorMode.Timer -> {
            if (option.index < 0) onSleepTimerChange(null)
            else onSleepTimerChange(SleepTimerMinutes.getOrNull(option.index))
        }
        TvPlayerSelectorMode.NightMode -> {
            onNightModeChange(option.index == 1)
        }
        TvPlayerSelectorMode.Loudness -> {
            onLoudnessGainChange(LoudnessGains.getOrNull(option.index) ?: 0)
        }
        TvPlayerSelectorMode.SubtitleDelay -> {
            onSubtitleDelayChange(SubtitleDelaysMs.getOrNull(option.index) ?: 0L)
        }
        TvPlayerSelectorMode.Translation -> {
            if (option.index < 0) {
                onTranslationEnabledChange(false)
            } else {
                TranslationLanguages.getOrNull(option.index)?.let { (code, _) ->
                    onTranslationLanguageChange(code)
                    if (!currentTranslationEnabled) onTranslationEnabledChange(true)
                }
            }
        }
        TvPlayerSelectorMode.Playlist -> {
            if (option.index < 0) return false
            player.seekTo(option.index, 0L)
            player.play()
        }
        // Every More / SubtitleStyleCustom entry carries a subMode and returns early at the top
        // of this function; empty branches keep the when exhaustive over the enum.
        TvPlayerSelectorMode.More -> return false
        TvPlayerSelectorMode.SubtitleStyleCustom -> return false
        TvPlayerSelectorMode.SubtitleStyle -> {
            when (option.index) {
                SUB_STYLE_RESET -> onSubtitleStyleUpdate { defaults ->
                    defaults.copy(
                        subtitleTextSize = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE,
                        subtitleTextColor = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR,
                        subtitleOutlineColor = PlayerPreferences.DEFAULT_SUBTITLE_OUTLINE_COLOR,
                        subtitleBackground = false,
                        subtitleFont = Font.DEFAULT,
                        subtitleTextBold = true,
                        subtitleShadow = true,
                        subtitleOutlineEnabled = true,
                        subtitleVerticalPosition = 0f,
                    )
                }
                else -> SubtitleStylePreset.entries.getOrNull(option.index)?.let { preset ->
                    onSubtitleStyleUpdate {
                        it.copy(
                            subtitleTextColor = preset.textColor,
                            subtitleOutlineColor = preset.outlineColor,
                            subtitleBackground = preset.background,
                            subtitleShadow = preset.shadow,
                            subtitleOutlineEnabled = preset.outlineEnabled,
                            subtitleTextBold = preset.textBold,
                        )
                    }
                }
            }
            return false
        }
        TvPlayerSelectorMode.SubtitleSize -> {
            SubtitleSizes.getOrNull(option.index)?.let { size ->
                onSubtitleStyleUpdate { it.copy(subtitleTextSize = size) }
            }
        }
        TvPlayerSelectorMode.SubtitlePosition -> {
            SubtitlePositions.getOrNull(option.index)?.let { value ->
                onSubtitleStyleUpdate { it.copy(subtitleVerticalPosition = value) }
            }
        }
        TvPlayerSelectorMode.SubtitleFont -> {
            SubtitleFontOrder.getOrNull(option.index)?.let { font ->
                onSubtitleStyleUpdate { it.copy(subtitleFont = font) }
            }
        }
        TvPlayerSelectorMode.SubtitleTextColor -> {
            SubtitleColors.getOrNull(option.index)?.let { namedColor ->
                onSubtitleStyleUpdate { it.copy(subtitleTextColor = namedColor.argb) }
            }
        }
        TvPlayerSelectorMode.SubtitleOutlineColor -> {
            SubtitleColors.getOrNull(option.index)?.let { namedColor ->
                onSubtitleStyleUpdate { it.copy(subtitleOutlineColor = namedColor.argb) }
            }
        }
        TvPlayerSelectorMode.SubtitleOutline -> {
            onSubtitleStyleUpdate { it.copy(subtitleOutlineEnabled = option.index == 1) }
        }
        TvPlayerSelectorMode.SubtitleShadow -> {
            onSubtitleStyleUpdate { it.copy(subtitleShadow = option.index == 1) }
        }
        TvPlayerSelectorMode.SubtitleBold -> {
            onSubtitleStyleUpdate { it.copy(subtitleTextBold = option.index == 1) }
        }
        TvPlayerSelectorMode.SubtitleBackground -> {
            onSubtitleStyleUpdate { it.copy(subtitleBackground = option.index == 1) }
        }
    }
    return true
}
