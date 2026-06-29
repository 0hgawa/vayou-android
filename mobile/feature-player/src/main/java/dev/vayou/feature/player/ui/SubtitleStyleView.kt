package dev.vayou.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vayou.core.model.Font
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.player.model.SubtitleStyleState
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.components.VayouSegmentedButton
import dev.vayou.core.ui.designsystem.components.VayouSegmentedButtonRow
import dev.vayou.core.ui.designsystem.components.VayouSlider
import dev.vayou.core.ui.designsystem.components.VayouSwitch
import dev.vayou.core.ui.designsystem.theme.VayouTheme

private val subtitleColors = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFFF1744.toInt(),
    0xFFFF6D00.toInt(),
    0xFFFFAB00.toInt(),
    0xFFFFFF00.toInt(),
    0xFF00E676.toInt(),
    0xFF00E5FF.toInt(),
    0xFF2979FF.toInt(),
    0xFFD500F9.toInt(),
    0xFFFF4081.toInt(),
)

private enum class SubtitlePreset(
    val textColor: Int,
    val background: Boolean,
    val shadow: Boolean,
    val outlineEnabled: Boolean,
    val outlineColor: Int,
    val textBold: Boolean,
) {
    LIGHT(
        textColor = 0xFFFFFFFF.toInt(),
        background = true,
        shadow = false,
        outlineEnabled = false,
        outlineColor = 0xFF000000.toInt(),
        textBold = false,
    ),
    DROP_SHADOW(
        textColor = 0xFFFFFFFF.toInt(),
        background = false,
        shadow = true,
        outlineEnabled = false,
        outlineColor = 0xFF000000.toInt(),
        textBold = true,
    ),
    DARK(
        textColor = 0xFFFFFFFF.toInt(),
        background = false,
        shadow = false,
        outlineEnabled = true,
        outlineColor = 0xFF000000.toInt(),
        textBold = true,
    ),
    CONTRAST(
        textColor = 0xFFFFFF00.toInt(),
        background = false,
        shadow = false,
        outlineEnabled = true,
        outlineColor = 0xFF000000.toInt(),
        textBold = true,
    ),
    BOX(
        textColor = 0xFFFFFFFF.toInt(),
        background = true,
        shadow = false,
        outlineEnabled = false,
        outlineColor = 0xFF000000.toInt(),
        textBold = true,
    ),
    RAISED(
        textColor = 0xFFFFFFFF.toInt(),
        background = false,
        shadow = true,
        outlineEnabled = true,
        outlineColor = 0xFF000000.toInt(),
        textBold = true,
    ),
}

private enum class SubtitleSizePreset(val textSize: Int) {
    SMALL(16),
    MEDIUM(20),
    LARGE(28),
}

private fun detectPreset(state: SubtitleStyleState): SubtitlePreset? {
    return SubtitlePreset.entries.firstOrNull { preset ->
        preset.textColor == state.textColor &&
            preset.background == state.background &&
            preset.shadow == state.shadow &&
            preset.outlineEnabled == state.outlineEnabled &&
            preset.textBold == state.textBold
    }
}

private fun detectSizePreset(textSize: Int): SubtitleSizePreset? {
    return SubtitleSizePreset.entries.firstOrNull { it.textSize == textSize }
}

@Composable
fun BoxScope.SubtitleStyleView(
    modifier: Modifier = Modifier,
    show: Boolean,
    preferences: PlayerPreferences,
    onChange: (SubtitleStyleState) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var textSize by remember(preferences) { mutableIntStateOf(preferences.subtitleTextSize) }
    var font by remember(preferences) { mutableStateOf(preferences.subtitleFont) }
    var textColor by remember(preferences) { mutableIntStateOf(preferences.subtitleTextColor) }
    var shadow by remember(preferences) { mutableStateOf(preferences.subtitleShadow) }
    var textBold by remember(preferences) { mutableStateOf(preferences.subtitleTextBold) }
    var background by remember(preferences) { mutableStateOf(preferences.subtitleBackground) }
    var outlineEnabled by remember(preferences) { mutableStateOf(preferences.subtitleOutlineEnabled) }
    var outlineColor by remember(preferences) { mutableIntStateOf(preferences.subtitleOutlineColor) }
    var verticalPosition by remember(preferences) { mutableFloatStateOf(preferences.subtitleVerticalPosition) }

    fun emitChange() {
        onChange(
            SubtitleStyleState(
                textSize = textSize,
                font = font,
                textColor = textColor,
                shadow = shadow,
                textBold = textBold,
                background = background,
                outlineEnabled = outlineEnabled,
                outlineColor = outlineColor,
                verticalPosition = verticalPosition,
            ),
        )
    }

    fun resetToDefaults() {
        textSize = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE
        font = Font.DEFAULT
        textColor = PlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR
        shadow = true
        textBold = true
        background = false
        outlineEnabled = true
        outlineColor = PlayerPreferences.DEFAULT_SUBTITLE_OUTLINE_COLOR
        verticalPosition = 0f
        emitChange()
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.subtitle_size_and_style),
        maxHeightFraction = 0.80f,
        onBack = onBack,
        trailingAction = {
            VayouIconButton(onClick = ::resetToDefaults) {
                Icon(
                    imageVector = VayouIcons.Refresh,
                    contentDescription = stringResource(R.string.subtitle_reset_defaults),
                    tint = VayouTheme.colors.onSurfaceVariant,
                )
            }
        },
    ) {
        VayouSegmentedButtonRow(
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            VayouSegmentedButton(
                modifier = Modifier.weight(1f),
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = stringResource(R.string.subtitle_presets),
            )
            VayouSegmentedButton(
                modifier = Modifier.weight(1f),
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = stringResource(R.string.subtitle_customization),
            )
        }

        when (selectedTab) {
            0 -> PresetsTab(
                textSize = textSize,
                currentState = SubtitleStyleState(
                    textSize = textSize,
                    font = font,
                    textColor = textColor,
                    shadow = shadow,
                    textBold = textBold,
                    background = background,
                    outlineEnabled = outlineEnabled,
                    outlineColor = outlineColor,
                    verticalPosition = verticalPosition,
                ),
                onSizeSelected = { textSize = it; emitChange() },
                onPresetSelected = { preset ->
                    textColor = preset.textColor
                    background = preset.background
                    shadow = preset.shadow
                    outlineEnabled = preset.outlineEnabled
                    outlineColor = preset.outlineColor
                    textBold = preset.textBold
                    emitChange()
                },
            )
            1 -> CustomizationTab(
                textSize = textSize,
                font = font,
                textColor = textColor,
                shadow = shadow,
                textBold = textBold,
                background = background,
                outlineEnabled = outlineEnabled,
                outlineColor = outlineColor,
                verticalPosition = verticalPosition,
                onTextSizeChange = { textSize = it; emitChange() },
                onFontChange = { font = it; emitChange() },
                onTextColorChange = { textColor = it; emitChange() },
                onShadowChange = { shadow = it; emitChange() },
                onTextBoldChange = { textBold = it; emitChange() },
                onBackgroundChange = { background = it; emitChange() },
                onOutlineEnabledChange = { outlineEnabled = it; emitChange() },
                onOutlineColorChange = { outlineColor = it; emitChange() },
                onVerticalPositionChange = { verticalPosition = it; emitChange() },
            )
        }
    }
}

@Composable
private fun PresetsTab(
    textSize: Int,
    currentState: SubtitleStyleState,
    onSizeSelected: (Int) -> Unit,
    onPresetSelected: (SubtitlePreset) -> Unit,
) {
    val selectedSizePreset = detectSizePreset(textSize)
    val selectedPreset = detectPreset(currentState)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SizePresetChip(
                label = stringResource(R.string.subtitle_size_small),
                selected = selectedSizePreset == SubtitleSizePreset.SMALL,
                onClick = { onSizeSelected(SubtitleSizePreset.SMALL.textSize) },
                modifier = Modifier.weight(1f),
            )
            SizePresetChip(
                label = stringResource(R.string.subtitle_size_medium),
                selected = selectedSizePreset == SubtitleSizePreset.MEDIUM,
                onClick = { onSizeSelected(SubtitleSizePreset.MEDIUM.textSize) },
                modifier = Modifier.weight(1f),
            )
            SizePresetChip(
                label = stringResource(R.string.subtitle_size_large),
                selected = selectedSizePreset == SubtitleSizePreset.LARGE,
                onClick = { onSizeSelected(SubtitleSizePreset.LARGE.textSize) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PresetCard(
                label = stringResource(R.string.subtitle_preset_light),
                preset = SubtitlePreset.LIGHT,
                selected = selectedPreset == SubtitlePreset.LIGHT,
                onClick = { onPresetSelected(SubtitlePreset.LIGHT) },
                modifier = Modifier.weight(1f),
            )
            PresetCard(
                label = stringResource(R.string.subtitle_preset_drop_shadow),
                preset = SubtitlePreset.DROP_SHADOW,
                selected = selectedPreset == SubtitlePreset.DROP_SHADOW,
                onClick = { onPresetSelected(SubtitlePreset.DROP_SHADOW) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PresetCard(
                label = stringResource(R.string.subtitle_preset_dark),
                preset = SubtitlePreset.DARK,
                selected = selectedPreset == SubtitlePreset.DARK,
                onClick = { onPresetSelected(SubtitlePreset.DARK) },
                modifier = Modifier.weight(1f),
            )
            PresetCard(
                label = stringResource(R.string.subtitle_preset_contrast),
                preset = SubtitlePreset.CONTRAST,
                selected = selectedPreset == SubtitlePreset.CONTRAST,
                onClick = { onPresetSelected(SubtitlePreset.CONTRAST) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PresetCard(
                label = stringResource(R.string.subtitle_preset_box),
                preset = SubtitlePreset.BOX,
                selected = selectedPreset == SubtitlePreset.BOX,
                onClick = { onPresetSelected(SubtitlePreset.BOX) },
                modifier = Modifier.weight(1f),
            )
            PresetCard(
                label = stringResource(R.string.subtitle_preset_raised),
                preset = SubtitlePreset.RAISED,
                selected = selectedPreset == SubtitlePreset.RAISED,
                onClick = { onPresetSelected(SubtitlePreset.RAISED) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SizePresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(VayouTheme.shapes.full)
            .background(
                if (selected) VayouTheme.colors.accentContainer else VayouTheme.colors.surface,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) VayouTheme.colors.onAccentContainer else VayouTheme.colors.onSurface,
            style = VayouTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PresetCard(
    label: String,
    preset: SubtitlePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = Color(preset.textColor)
    val outlineColor = Color(preset.outlineColor)
    val fontWeight = if (preset.textBold) FontWeight.Bold else FontWeight.Normal
    val shadow = if (preset.shadow) {
        Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
    } else {
        Shadow.None
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(VayouTheme.shapes.medium)
                .background(VayouTheme.colors.surface)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) VayouTheme.colors.accent else Color.Transparent,
                    shape = VayouTheme.shapes.medium,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val isLightStyle = preset == SubtitlePreset.LIGHT
            val bgModifier = when {
                isLightStyle -> Modifier
                    .background(Color(0x80FFFFFF), VayouTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                preset.background -> Modifier
                    .background(Color.Black, VayouTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                else -> Modifier
            }
            val previewTextColor = if (isLightStyle) Color.Black else textColor

            Box(modifier = bgModifier) {
                if (preset.outlineEnabled) {
                    Text(
                        text = "Abc",
                        fontSize = 22.sp,
                        fontWeight = fontWeight,
                        color = outlineColor,
                        style = TextStyle(
                            drawStyle = Stroke(
                                width = 6f,
                                join = StrokeJoin.Round,
                            ),
                        ),
                    )
                }
                Text(
                    text = "Abc",
                    fontSize = 22.sp,
                    fontWeight = fontWeight,
                    color = previewTextColor,
                    style = TextStyle(shadow = shadow),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = VayouTheme.typography.bodySmall,
            color = if (selected) VayouTheme.colors.accent else VayouTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun CustomizationTab(
    textSize: Int,
    font: Font,
    textColor: Int,
    shadow: Boolean,
    textBold: Boolean,
    background: Boolean,
    outlineEnabled: Boolean,
    outlineColor: Int,
    verticalPosition: Float,
    onTextSizeChange: (Int) -> Unit,
    onFontChange: (Font) -> Unit,
    onTextColorChange: (Int) -> Unit,
    onShadowChange: (Boolean) -> Unit,
    onTextBoldChange: (Boolean) -> Unit,
    onBackgroundChange: (Boolean) -> Unit,
    onOutlineEnabledChange: (Boolean) -> Unit,
    onOutlineColorChange: (Int) -> Unit,
    onVerticalPositionChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            val positionPercent = (verticalPosition * 100).toInt()
            SliderRow(
                title = stringResource(R.string.subtitle_text_size),
                value = textSize.toFloat(),
                valueRange = 10f..60f,
                valueText = textSize.toString(),
                onValueChange = { onTextSizeChange(it.toInt()) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            SliderRow(
                title = stringResource(R.string.subtitle_vertical_position),
                value = verticalPosition,
                valueRange = -0.5f..0.5f,
                valueText = if (positionPercent == 0) "0" else "%+d".format(positionPercent),
                onValueChange = onVerticalPositionChange,
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        FontDropdownRow(
            title = stringResource(R.string.subtitle_font),
            selected = font,
            onSelected = onFontChange,
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            ColorRow(
                title = stringResource(R.string.subtitle_text_color),
                selectedColor = textColor,
                onColorSelected = onTextColorChange,
            )

            Spacer(modifier = Modifier.height(12.dp))

            SwitchRow(
                title = stringResource(R.string.subtitle_shadow),
                checked = shadow,
                onCheckedChange = onShadowChange,
            )

            SwitchRow(
                title = stringResource(R.string.subtitle_text_bold),
                checked = textBold,
                onCheckedChange = onTextBoldChange,
            )

            SwitchRow(
                title = stringResource(R.string.subtitle_background),
                checked = background,
                onCheckedChange = onBackgroundChange,
            )

            SwitchRow(
                title = stringResource(R.string.subtitle_outline),
                checked = outlineEnabled,
                onCheckedChange = onOutlineEnabledChange,
            )

            if (outlineEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                ColorRow(
                    title = stringResource(R.string.subtitle_outline_color),
                    selectedColor = outlineColor,
                    onColorSelected = onOutlineColorChange,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        VayouSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurfaceVariant,
            )
        }
        VayouSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

@Composable
private fun ColorRow(
    title: String,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = VayouTheme.typography.bodyMedium,
            color = VayouTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            subtitleColors.forEach { colorInt ->
                val isSelected = colorInt == selectedColor
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) VayouTheme.colors.accent else VayouTheme.colors.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onColorSelected(colorInt) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = VayouIcons.Check,
                            contentDescription = null,
                            tint = if (colorInt == 0xFF000000.toInt()) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FontDropdownRow(
    title: String,
    selected: Font,
    onSelected: (Font) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = selected.label(),
                style = VayouTheme.typography.bodyMedium,
                color = VayouTheme.colors.onSurfaceVariant,
            )
            Icon(
                imageVector = VayouIcons.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(VayouTheme.iconSize.sm),
                tint = VayouTheme.colors.onSurfaceVariant,
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Font.entries.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(option)
                                expanded = false
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = VayouIcons.Check,
                                contentDescription = null,
                                tint = VayouTheme.colors.accent,
                                modifier = Modifier.size(VayouTheme.iconSize.sm),
                            )
                        } else {
                            Spacer(modifier = Modifier.width(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option.label(),
                            style = VayouTheme.typography.bodyMedium,
                            color = VayouTheme.colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun Font.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }
