package dev.vayou.tv.feature.player

import dev.vayou.core.model.Font

internal val SubtitleFontOrder: List<Font> = listOf(Font.DEFAULT, Font.SANS_SERIF, Font.SERIF, Font.MONOSPACE)

internal fun fontLabel(font: Font): String = when (font) {
    Font.DEFAULT -> "Padrão"
    Font.SANS_SERIF -> "Sans-serif"
    Font.SERIF -> "Serif"
    Font.MONOSPACE -> "Monoespaçada"
}
