package dev.vayou.feature.videopicker.screens

import dev.vayou.core.model.Folder

sealed interface MediaState {
    data object Loading : MediaState
    data class Success(val data: Folder?) : MediaState
}
