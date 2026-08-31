package dev.vayou.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.SubtitleFont
import dev.vayou.core.player.ui.Black
import dev.vayou.core.player.ui.SubtitleColours
import dev.vayou.core.player.ui.SubtitlePreset
import dev.vayou.core.player.ui.SubtitleSample
import dev.vayou.core.player.ui.SubtitleSizePreset
import dev.vayou.core.player.ui.isDefaultSubtitleStyle
import dev.vayou.core.player.ui.withDefaultSubtitleStyle
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouIconButton
import dev.vayou.core.ui.designsystem.components.VayouSegmentedButtons
import dev.vayou.core.ui.designsystem.components.VayouSelectableTile
import dev.vayou.core.ui.designsystem.components.VayouSelectableTileSpacing
import dev.vayou.core.ui.designsystem.components.VayouSheet
import dev.vayou.core.ui.designsystem.components.VayouSheetDefaults
import dev.vayou.core.ui.designsystem.components.VayouSheetTitle
import dev.vayou.core.ui.designsystem.components.VayouSlider
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.theme.VayouTheme
import kotlin.math.roundToInt

/**
 * How subtitles look, changed while watching them.
 *
 * A sheet over the film rather than a page in the settings, because every one of these is judged by
 * looking at a caption on a scene, not by reading its name.
 *
 * Three kinds of choice, three shapes: a segmented strip for which view is open, tiles for the three
 * sizes, framed cards for the six looks. A screen where changing tabs looks exactly like choosing a
 * size is a screen you have to read rather than glance at.
 */
@Composable
fun SubtitleStyleSheet(style: PlayerPreferences, onChange: (PlayerPreferences) -> Unit, onDismiss: () -> Unit) {
    VayouSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = VayouTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VayouSheetTitle(text = stringResource(R.string.subtitle_style), modifier = Modifier.weight(1f))
            // Only when there is something to undo: on an untouched style this is a control that
            // does nothing, in the corner the eye checks first.
            if (!style.isDefaultSubtitleStyle) {
                VayouIconButton(onClick = { onChange(style.withDefaultSubtitleStyle()) }) {
                    Icon(
                        imageVector = VayouIcons.Refresh,
                        contentDescription = stringResource(R.string.reset),
                        tint = VayouTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }

        var showCustom by remember { mutableStateOf(false) }

        // Whole looks first, the parts behind them second. Most viewers want one of six answers, not
        // seven controls.
        VayouSegmentedButtons(
            labels = listOf(stringResource(R.string.subtitle_presets), stringResource(R.string.subtitle_customise)),
            selectedIndex = if (showCustom) 1 else 0,
            onSelect = { showCustom = it == 1 },
            modifier = Modifier.padding(horizontal = VayouSheetDefaults.HorizontalPadding),
        )
        Spacer(modifier = Modifier.height(AfterTabStrip))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            if (showCustom) {
                CustomiseTab(style = style, onChange = onChange)
            } else {
                PresetsTab(style = style, onChange = onChange)
            }
            Spacer(modifier = Modifier.height(VayouSheetDefaults.BottomPadding))
        }
    }
}

/** The three sizes, then the six looks as the old player laid them out: two cards to a row. */
@Composable
private fun PresetsTab(style: PlayerPreferences, onChange: (PlayerPreferences) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = VayouSheetDefaults.HorizontalPadding)) {
        Row(horizontalArrangement = Arrangement.spacedBy(VayouSelectableTileSpacing)) {
            SubtitleSizePreset.entries.forEach { size ->
                VayouSelectableTile(
                    label = stringResource(size.label),
                    selected = size.textSize == style.subtitleTextSize,
                    onClick = { onChange(style.copy(subtitleTextSize = size.textSize)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(AfterPlainControl))

        Column(verticalArrangement = Arrangement.spacedBy(PresetGridGap)) {
            SubtitlePreset.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(PresetGridGap)) {
                    pair.forEach { preset ->
                        PresetCard(
                            preset = preset,
                            selected = preset.matches(style),
                            onClick = { onChange(preset.applyTo(style)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One look, shown rather than named.
 *
 * A border and never a fill: what is inside the frame is a caption, and filling it would be painting
 * over the thing being chosen. Neutral rather than accent, so the whole sheet says "this one" one
 * way.
 */
@Composable
private fun PresetCard(preset: SubtitlePreset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = VayouTheme.shapes.medium
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CardHeight)
                .clip(shape)
                .background(VayouTheme.colors.surface)
                .border(
                    width = if (selected) SelectedBorder else 0.dp,
                    color = if (selected) VayouTheme.colors.onSurface else Color.Transparent,
                    shape = shape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            SubtitleSample(preset = preset)
        }
        Spacer(modifier = Modifier.height(CardLabelGap))
        Text(
            text = stringResource(preset.label),
            style = VayouTheme.typography.bodySmall,
            color = if (selected) VayouTheme.colors.onSurface else VayouTheme.colors.onSurfaceVariant,
        )
    }
}

/**
 * The parts, in three blocks: how big and where, what colour, and what is drawn around the letters.
 *
 * Only what is judged by eye while watching. Whether to hand the whole question to Android and
 * whether to honour what an .ass file asks for are settings, not controls -- they are answered once
 * and they live in the settings, where they already are.
 *
 * The steps between the blocks are stated as what each control brings of its own, so every gap on
 * screen is the same regardless of whether a slider or a switch sits above it.
 */
@Composable
private fun CustomiseTab(style: PlayerPreferences, onChange: (PlayerPreferences) -> Unit) {
    SliderRow(
        label = stringResource(R.string.subtitle_style_size),
        reading = style.subtitleTextSize.toString(),
        value = style.subtitleTextSize.toFloat(),
        range = MinTextSize..MaxTextSize,
        onChange = { onChange(style.copy(subtitleTextSize = it.roundToInt())) },
    )

    Spacer(modifier = Modifier.height(AfterSlider))

    SliderRow(
        label = stringResource(R.string.subtitle_style_position),
        // A share of the picture, as a percentage of it: the number the old player showed.
        reading = "${(style.subtitleVerticalPosition * PositionPercent).roundToInt()}",
        value = style.subtitleVerticalPosition,
        range = 0f..MaxVerticalPosition,
        onChange = { onChange(style.copy(subtitleVerticalPosition = it)) },
    )

    Spacer(modifier = Modifier.height(AfterSlider))

    ColourRow(
        label = stringResource(R.string.subtitle_style_colour),
        selected = style.subtitleTextColor,
        onPick = { onChange(style.copy(subtitleTextColor = it)) },
    )

    Spacer(modifier = Modifier.height(AfterPlainControl))

    FontRow(selected = style.subtitleFont, onPick = { onChange(style.copy(subtitleFont = it)) })

    Spacer(modifier = Modifier.height(AroundSwitchGroup))

    SwitchRow(
        label = stringResource(R.string.subtitle_style_bold),
        checked = style.subtitleTextBold,
        onChange = { onChange(style.copy(subtitleTextBold = it)) },
    )
    SwitchRow(
        label = stringResource(R.string.subtitle_style_outline),
        checked = style.subtitleOutlineEnabled,
        onChange = { onChange(style.copy(subtitleOutlineEnabled = it)) },
    )
    SwitchRow(
        label = stringResource(R.string.subtitle_style_shadow),
        checked = style.subtitleShadow,
        onChange = { onChange(style.copy(subtitleShadow = it)) },
    )
    SwitchRow(
        label = stringResource(R.string.subtitle_style_background),
        checked = style.subtitleBackground,
        onChange = { onChange(style.copy(subtitleBackground = it)) },
    )

    // Only behind the outline, as the old player had it. A drop shadow is drawn in this colour too,
    // but the switch that owns the control is the one people go looking for it under.
    if (style.subtitleOutlineEnabled) {
        Spacer(modifier = Modifier.height(AroundSwitchGroup))
        ColourRow(
            label = stringResource(R.string.subtitle_style_outline_colour),
            selected = style.subtitleOutlineColor,
            onPick = { onChange(style.copy(subtitleOutlineColor = it)) },
        )
    }
}

/**
 * The face, named on its own line with the choice folded away behind it.
 *
 * A row that opens rather than four tiles on the sheet, which is the shape the old player used. Of
 * everything here this is the one nobody sets twice: three of the four are the same letterforms at
 * a glance, and a block of them on a panel about legibility is four tiles spent on the least of the
 * questions.
 */
@Composable
private fun FontRow(selected: SubtitleFont, onPick: (SubtitleFont) -> Unit) {
    var isOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isOpen = !isOpen }
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding, vertical = VayouTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.subtitle_style_font),
            style = VayouSheetDefaults.ControlLabelStyle,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(selected.label),
            style = VayouSheetDefaults.ControlLabelStyle,
            color = VayouTheme.colors.onSurfaceVariant,
        )
        Icon(
            imageVector = VayouIcons.ArrowDownward,
            contentDescription = null,
            tint = VayouTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(VayouTheme.iconSize.sm),
        )
    }

    if (!isOpen) return
    // The tick the rest of the player's sheets mark a choice with, rather than a fifth way of
    // saying it on the one panel that is about how things read.
    SubtitleFont.entries.forEach { font ->
        CheckedRow(
            text = stringResource(font.label),
            isSelected = font == selected,
            onClick = {
                onPick(font)
                isOpen = false
            },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = VayouSheetDefaults.HorizontalPadding, vertical = VayouTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = VayouSheetDefaults.ControlLabelStyle, color = VayouTheme.colors.onSurface)
        VayouSwitch(checked = checked, onCheckedChange = onChange)
    }
}

/** The name on the left, what it is set to on the right, the track under both -- as the old player
 *  had it, and as the equalizer's own effect rows do. */
@Composable
private fun SliderRow(
    label: String,
    reading: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = VayouSheetDefaults.HorizontalPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = VayouSheetDefaults.ControlLabelStyle,
                color = VayouTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = reading,
                style = VayouSheetDefaults.ControlLabelStyle,
                color = VayouTheme.colors.onSurfaceVariant,
            )
        }
        VayouSlider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ColourRow(label: String, selected: Int, onPick: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = VayouSheetDefaults.HorizontalPadding)) {
        Text(text = label, style = VayouSheetDefaults.ControlLabelStyle, color = VayouTheme.colors.onSurface)
        Row(
            modifier = Modifier
                .padding(top = VayouTheme.spacing.sm)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
        ) {
            SubtitleColours.forEach { colour ->
                Swatch(colour = colour, isSelected = colour == selected, onClick = { onPick(colour) })
            }
        }
    }
}

@Composable
private fun Swatch(colour: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .clip(CircleShape)
            .background(Color(colour))
            // Neutral, not the accent: one of these swatches is amber, and an amber ring around it
            // is a ring you cannot see.
            .border(
                width = if (isSelected) SelectedRing else UnselectedRing,
                color = if (isSelected) VayouTheme.colors.onSurface else VayouTheme.colors.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = VayouIcons.Check,
                contentDescription = null,
                // The one swatch a dark tick would vanish into.
                tint = if (colour == Black) VayouTheme.colors.onVideo else VayouTheme.colors.videoBackdrop,
                modifier = Modifier.size(TickSize),
            )
        }
    }
}

private val SubtitleFont.label: Int
    get() = when (this) {
        SubtitleFont.Default -> R.string.subtitle_font_default
        SubtitleFont.SansSerif -> R.string.subtitle_font_sans
        SubtitleFont.Serif -> R.string.subtitle_font_serif
        SubtitleFont.Monospace -> R.string.subtitle_font_mono
    }

/** The height is a fraction of the picture; the reading beside it is that fraction as a percentage. */
private const val PositionPercent = 100

private const val MinTextSize = 12f

private const val MaxTextSize = 48f

/** Half the picture. Higher than that and a caption is over the faces rather than under them. */
private const val MaxVerticalPosition = 0.5f

private val SwatchSize = 36.dp

private val SelectedRing = 3.dp

private val UnselectedRing = 1.dp

private val TickSize = 18.dp

/**
 * One gap between two labelled controls, declared three ways because different controls sit on
 * either side of it and each brings its own air: a slider carries about 16dp under its track, a
 * switch row 8dp, and a label with swatches none.
 *
 * Stated as the number the layout is given, chosen so the gap on screen is the same 28dp every time.
 */
private val AfterSlider = 12.dp

/** Around a block of switches. Between two of them there is no gap at all -- their own 8dp each side
 *  makes 16, and four toggles in a row are one group rather than four blocks. */
private val AroundSwitchGroup = 20.dp

/** After a control that brings nothing of its own. */
private val AfterPlainControl = 28.dp

/** Between the tab strip and the tab it opened. Less than the step between two blocks, because a
 *  strip and its content are one thing rather than two. */
private val AfterTabStrip = 24.dp

/** The preset grid, both ways. A grid's two axes have to agree with each other before they agree
 *  with anything else, and its rows carry a label under each card. */
private val PresetGridGap = 12.dp

private val CardHeight = 80.dp

private val CardLabelGap = 6.dp

private val SelectedBorder = 2.dp
