package dev.vayou.core.player.model

import dev.vayou.core.model.Font

data class SubtitleStyleState(
    val textSize: Int,
    val font: Font,
    val textColor: Int,
    val shadow: Boolean,
    val textBold: Boolean,
    val background: Boolean,
    val outlineEnabled: Boolean,
    val outlineColor: Int,
    val verticalPosition: Float,
)
