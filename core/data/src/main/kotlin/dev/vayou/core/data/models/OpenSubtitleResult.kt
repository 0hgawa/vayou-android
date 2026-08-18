package dev.vayou.core.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenSubtitleResult(
    @SerialName("SubFileName") val subFileName: String = "",
    @SerialName("SubFormat") val subFormat: String = "",
    @SerialName("SubLanguageID") val subLanguageId: String = "",
    @SerialName("SubDownloadLink") val subDownloadLink: String = "",
    @SerialName("SubDownloadsCnt") val subDownloadsCnt: String = "0",
    @SerialName("MatchedBy") val matchedBy: String = "",
)
