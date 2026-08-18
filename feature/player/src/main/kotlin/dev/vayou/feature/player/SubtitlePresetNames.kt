package dev.vayou.feature.player

import dev.vayou.core.player.ui.SubtitlePreset
import dev.vayou.core.player.ui.SubtitleSizePreset

/**
 * What each ready-made caption style is called on this phone.
 *
 * Beside the sheet that lists them rather than on the enum: the shape of a style is the same on
 * either device, and the two shells keep their own strings.
 */
internal val SubtitlePreset.label: Int
    get() = when (this) {
        SubtitlePreset.Raised -> R.string.subtitle_preset_raised
        SubtitlePreset.Outlined -> R.string.subtitle_preset_outlined
        SubtitlePreset.DropShadow -> R.string.subtitle_preset_shadow
        SubtitlePreset.Contrast -> R.string.subtitle_preset_contrast
        SubtitlePreset.Light -> R.string.subtitle_preset_light
        SubtitlePreset.Box -> R.string.subtitle_preset_box
    }

internal val SubtitleSizePreset.label: Int
    get() = when (this) {
        SubtitleSizePreset.Small -> R.string.subtitle_size_small
        SubtitleSizePreset.Medium -> R.string.subtitle_size_medium
        SubtitleSizePreset.Large -> R.string.subtitle_size_large
    }
