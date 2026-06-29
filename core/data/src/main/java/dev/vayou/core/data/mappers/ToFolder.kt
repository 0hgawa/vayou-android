package dev.vayou.core.data.mappers

import dev.vayou.core.common.Utils
import dev.vayou.core.database.relations.DirectoryWithMedia
import dev.vayou.core.database.relations.MediumWithInfo
import dev.vayou.core.model.Folder

fun DirectoryWithMedia.toFolder() = Folder(
    name = directory.name,
    path = directory.path,
    dateModified = directory.modified,
    parentPath = directory.parentPath,
    formattedMediaSize = Utils.formatFileSize(media.sumOf { it.mediumEntity.size }),
    mediaList = media.map(MediumWithInfo::toVideo),
)
