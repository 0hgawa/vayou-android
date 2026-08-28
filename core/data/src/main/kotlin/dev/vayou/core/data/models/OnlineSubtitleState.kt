package dev.vayou.core.data.models

/** Where a search has got to, for the sheet to draw. */
sealed interface OnlineSubtitleState {
    data object Idle : OnlineSubtitleState

    data object Searching : OnlineSubtitleState

    /** Empty when the search worked and found nothing, which is not the same as failing. */
    data class Found(val results: List<OpenSubtitleResult>) : OnlineSubtitleState

    data object Failed : OnlineSubtitleState

    /** One result is being fetched and attached; the rest of the list stays on screen behind it. */
    data class Downloading(val results: List<OpenSubtitleResult>, val downloading: OpenSubtitleResult) :
        OnlineSubtitleState
}
