package dev.vayou.core.player.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import dev.vayou.core.data.subtitle.RealtimeTranslator
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.SubtitleFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The subtitles, drawn by Media3's own view.
 *
 * One of the few places an Android view earns its keep. This is a rendering primitive and not a set
 * of controls: it places SSA and ASS cues where the file says they go, honours the size and colour
 * a viewer set in the system's captioning settings, and has no Compose equivalent. Rewriting it
 * would be reimplementing a subtitle format, not a widget.
 */
@OptIn(UnstableApi::class)
@Composable
fun SubtitleOverlay(
    player: Player,
    modifier: Modifier = Modifier,
    /** A language code to translate every line into, or null to show the file's own words. */
    translateTo: String? = null,
    style: PlayerPreferences = PlayerPreferences(),
) {
    val spoken = rememberCuesState(player).cues
    var shown by remember { mutableStateOf(spoken) }
    // Counted rather than compared: a slow translation must not overwrite a line that arrived
    // after it was asked for, and two batches can be in flight at once.
    var asked by remember { mutableIntStateOf(0) }
    var drawn by remember { mutableIntStateOf(0) }

    // Launched on the composition's scope and not from the effect itself.
    //
    // A LaunchedEffect keyed on the cues is cancelled the moment the next line arrives -- and since
    // a batch is held back to the end of the lookahead window, the next line always arrives first.
    // Every translation was being killed a breath before it was due to show, which is the whole of
    // the bug: the words that did appear were the ones the guard let through late.
    val scope = rememberCoroutineScope()

    LaunchedEffect(spoken, translateTo) {
        if (translateTo == null) {
            shown = spoken
            return@LaunchedEffect
        }
        val version = ++asked
        val cues = spoken
        scope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            val translations = RealtimeTranslator.translateBatch(
                texts = cues.map { it.text?.toString().orEmpty() },
                targetLanguage = translateTo,
            )
            val translated = cues.mapIndexed { index, cue ->
                translations.getOrNull(index)?.let { cue.buildUpon().setText(it).build() } ?: cue
            }
            // Held back to the end of the window the cues were pulled forward by, so the line lands
            // on the moment it was written for. What moves is where the waiting happens: before the
            // line is due rather than after it.
            val remaining = TranslationLookaheadMs - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)

            if (version >= drawn) {
                drawn = version
                shown = translated
            }
        }
    }

    val cues = shown

    AndroidView(
        factory = ::SubtitleView,
        update = { view ->
            view.setCues(cues)
            view.setApplyEmbeddedStyles(style.applyEmbeddedStyles)
            if (style.useSystemCaptionStyle) {
                // Handed wholesale to Android's own captioning settings, for a viewer who has
                // already told the system how they need captions to look.
                view.setUserDefaultStyle()
                view.setUserDefaultTextSize()
                return@AndroidView
            }
            view.setStyle(
                CaptionStyleCompat(
                    style.subtitleTextColor,
                    if (style.subtitleBackground) SubtitleBackgroundScrim else Transparent,
                    Transparent,
                    subtitleEdgeType(style.subtitleOutlineEnabled, style.subtitleShadow),
                    style.subtitleOutlineColor,
                    Typeface.create(
                        style.subtitleFont.typeface,
                        if (style.subtitleTextBold) Typeface.BOLD else Typeface.NORMAL,
                    ),
                ),
            )
            // Absolute and not fractional: a fraction of the *view* moves with the framing, and a
            // caption that jumps when the picture is cropped is a caption nobody placed.
            view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, style.subtitleTextSize.toFloat())
            // On top of the view's own default rather than in place of it. Zero is the foot of the
            // picture itself, which is lower than any player puts a caption; the stored number is
            // how far the viewer has raised it from where it belongs, and that is what the slider
            // that sets it reads as.
            view.setBottomPaddingFraction(
                SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION + style.subtitleVerticalPosition,
            )
        },
        modifier = modifier,
    )
}

/** The platform face this choice names. */
@OptIn(UnstableApi::class)
private val SubtitleFont.typeface: Typeface
    get() = when (this) {
        SubtitleFont.Default -> Typeface.DEFAULT
        SubtitleFont.SansSerif -> Typeface.SANS_SERIF
        SubtitleFont.Serif -> Typeface.SERIF
        SubtitleFont.Monospace -> Typeface.MONOSPACE
    }

/**
 * What is drawn around the letters, from the two switches that ask for it.
 *
 * One edge and not two: the caption view takes a single edge type, so asking for an outline and a
 * shadow at once used to be answered with the outline alone -- which is why "Outlined" and "Raised"
 * were the same style wearing two names. Both together is what a raised edge is.
 */
@OptIn(UnstableApi::class)
fun subtitleEdge(outline: Boolean, shadow: Boolean): SubtitleEdge = when {
    outline && shadow -> SubtitleEdge.Raised
    outline -> SubtitleEdge.Outline
    shadow -> SubtitleEdge.Shadow
    else -> SubtitleEdge.None
}

@OptIn(UnstableApi::class)
enum class SubtitleEdge { None, Outline, Shadow, Raised }

@OptIn(UnstableApi::class)
private fun subtitleEdgeType(outline: Boolean, shadow: Boolean): Int = when (subtitleEdge(outline, shadow)) {
    SubtitleEdge.None -> CaptionStyleCompat.EDGE_TYPE_NONE
    SubtitleEdge.Outline -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
    SubtitleEdge.Shadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    SubtitleEdge.Raised -> CaptionStyleCompat.EDGE_TYPE_RAISED
}

/**
 * How far ahead of time the cues are asked for while translating.
 *
 * Two seconds, as the old player had it. Both halves of the trick are needed and neither works
 * alone: the renderer is told to deliver every line this much early, and each translated batch is
 * held until the window has run out. What moves is where the waiting happens.
 */
const val TranslationLookaheadMs = 2_000L

/** Behind the words only, and dark enough to read white on over a bright frame. */
private const val SubtitleBackgroundScrim = 0xA6000000.toInt()

private const val Transparent = Color.TRANSPARENT
