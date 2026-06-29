package dev.vayou.core.data.mappers

import dev.vayou.core.database.entities.SubtitleStreamInfoEntity
import dev.vayou.core.model.SubtitleStreamInfo

fun SubtitleStreamInfoEntity.toSubtitleStreamInfo() = SubtitleStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
)
