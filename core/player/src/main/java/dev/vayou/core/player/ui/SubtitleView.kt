package dev.vayou.core.player.ui

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.accessibility.CaptioningManager
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dev.vayou.core.data.subtitle.RealtimeTranslator
import dev.vayou.core.model.Font
import dev.vayou.core.player.extensions.toTypeface
import dev.vayou.core.player.state.rememberCuesState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun SubtitleView(
    modifier: Modifier = Modifier,
    player: Player,
    configuration: SubtitleConfiguration,
) {
    val cuesState = rememberCuesState(player)
    var translatedCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var cueVersion by remember { mutableIntStateOf(0) }
    var displayedVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(cuesState.cues, configuration.realtimeTranslationEnabled, configuration.realtimeTranslationLanguage) {
        if (!configuration.realtimeTranslationEnabled) {
            translatedCues = cuesState.cues
            return@LaunchedEffect
        }

        val version = ++cueVersion
        val cues = cuesState.cues
        val lang = configuration.realtimeTranslationLanguage
        val lookaheadMs = configuration.translationLookaheadMs

        scope.launch {
            val startTime = System.currentTimeMillis()

            val texts = cues.map { it.text?.toString().orEmpty() }
            val translations = RealtimeTranslator.translateBatch(texts, lang)
            val translated = cues.mapIndexed { i, cue ->
                translations[i]?.let { cue.buildUpon().setText(it).build() } ?: cue
            }

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = lookaheadMs - elapsed
            if (remaining > 0) delay(remaining)

            if (version >= displayedVersion) {
                translatedCues = translated
                displayedVersion = version
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context -> SubtitleView(context) },
        update = { subtitleView ->
            subtitleView.setCues(translatedCues)

            val captioningManager = getSystemService(subtitleView.context, CaptioningManager::class.java)
            if (configuration.useSystemCaptionStyle && captioningManager != null) {
                subtitleView.setStyle(CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle))
            } else {
                val edgeType = when {
                    configuration.outlineEnabled -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                    configuration.shadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                    else -> CaptionStyleCompat.EDGE_TYPE_NONE
                }
                subtitleView.setStyle(
                    CaptionStyleCompat(
                        configuration.textColor,
                        if (configuration.showBackground) Color.BLACK else Color.TRANSPARENT,
                        Color.TRANSPARENT,
                        edgeType,
                        configuration.outlineColor,
                        Typeface.create(
                            configuration.font.toTypeface(),
                            if (configuration.textBold) Typeface.BOLD else Typeface.NORMAL,
                        ),
                    ),
                )
            }
            subtitleView.setApplyEmbeddedStyles(configuration.applyEmbeddedStyles)

            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, configuration.textSize.toFloat())
        },
    )
}

@Stable
data class SubtitleConfiguration(
    val useSystemCaptionStyle: Boolean,
    val showBackground: Boolean,
    val font: Font,
    val textSize: Int,
    val textBold: Boolean,
    val applyEmbeddedStyles: Boolean,
    val textColor: Int,
    val shadow: Boolean,
    val outlineEnabled: Boolean,
    val outlineColor: Int,
    val verticalPosition: Float = 0f,
    val realtimeTranslationEnabled: Boolean = false,
    val realtimeTranslationLanguage: String = "en",
    val translationLookaheadMs: Long = 0L,
)
