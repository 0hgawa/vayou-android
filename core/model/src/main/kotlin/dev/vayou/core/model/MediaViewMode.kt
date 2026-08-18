package dev.vayou.core.model

enum class MediaViewMode {
    FOLDERS,
    VIDEOS,

    /** Appended, never reordered: this is stored as an ordinal. */
    PLAYLISTS,
}
