package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.theme.VayouTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Says what just happened, for the things a screen cannot show by itself.
 *
 * Only those. A star that fills in as it is pressed needs no sentence about having been pressed, and
 * a line that says so every time is a line nobody reads by the third one. What earns a word is an
 * action whose result landed somewhere else -- a track put in a list you are not looking at, a film
 * moved to a folder that is locked -- and a failure, which today happens in silence.
 *
 * Held as one object rather than passed as a state and a scope, because every caller wants the same
 * two things: say this, and offer one way back.
 */
@Stable
class VayouMessages internal constructor(
    internal val host: SnackbarHostState?,
    private val scope: CoroutineScope?,
    private val accessibility: AccessibilityManager?,
) {

    /** The one on screen, so the next message can take its place rather than queue behind it. */
    private var showing: Job? = null

    /**
     * Something went wrong, said the same way but marked.
     *
     * The mark is the whole reason this is a second call: "tags saved" and "could not write the
     * tags" are the same shape at a glance, and a glance is all one of these gets. A confirmation
     * needs no glyph -- it repeats what was just pressed -- but a failure has to survive being
     * half-read.
     */
    fun showProblem(text: String) = show(text, isProblem = true)

    fun show(text: String, action: String? = null, isProblem: Boolean = false, onAction: () -> Unit = {}) {
        val host = host ?: return
        val scope = scope ?: return

        // Material offers four seconds and ten. Both are long for a line that only confirms what
        // was just pressed -- by the time it goes the hand has moved on and it is a leftover on the
        // screen. Timed here instead, and the snackbar is raised as indefinite so this is the only
        // clock. Longer when something can be pressed: two seconds is not time to read a sentence
        // and decide to undo it.
        val wanted = if (action == null) BriefMs else OfferMs
        val timeout = accessibility?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = wanted,
            containsIcons = false,
            containsText = true,
            containsControls = action != null,
        ) ?: wanted

        // The newest is the true one: two of these stacked would be a queue of stale news, and the
        // one being read would be the one that stopped being true first. Cancelling the job that
        // raised the old one is what takes it off the screen.
        showing?.cancel()
        showing = scope.launch {
            val raised = launch {
                val result = host.showSnackbar(MessageVisuals(text, action, isProblem))
                if (result == SnackbarResult.ActionPerformed) onAction()
            }
            delay(timeout)
            raised.cancel()
        }
    }

    companion object {
        /** For every screen that draws no host: a television, a preview, a sheet on its own. */
        val Silent = VayouMessages(host = null, scope = null, accessibility = null)

        /** Long enough to read four words, short enough that nobody watches it leave. */
        private const val BriefMs = 2_000L

        private const val OfferMs = 4_000L
    }
}

/**
 * Carries the one thing Material's own visuals cannot: whether this is news or a complaint.
 *
 * Indefinite on purpose -- [VayouMessages] holds the only clock, so the two do not race.
 */
private class MessageVisuals(override val message: String, override val actionLabel: String?, val isProblem: Boolean) :
    SnackbarVisuals {
    override val duration = SnackbarDuration.Indefinite
    override val withDismissAction = false
}

/** Absent by default, so a screen with nowhere to put a message simply says nothing. */
val LocalVayouMessages = staticCompositionLocalOf { VayouMessages.Silent }

@Composable
fun rememberVayouMessages(): VayouMessages {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Whoever is reading the screen aloud needs longer than whoever is looking at it, and the
    // system knows by how much. Asked once here rather than guessed at.
    val accessibility = LocalAccessibilityManager.current
    return remember(host, scope, accessibility) { VayouMessages(host, scope, accessibility) }
}

/**
 * Where the messages appear: over the content, above whatever the bottom bar holds.
 *
 * Above rather than across: the bar at the foot of this app is a running player, and a message that
 * covers the pause button is a message that arrived at the worst possible moment.
 */
@Composable
fun VayouMessageHost(messages: VayouMessages) {
    val host = messages.host ?: return
    SnackbarHost(
        hostState = host,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VayouTheme.spacing.md),
    ) { data ->
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            VayouSnackbar(data)
        }
    }
}

/**
 * A card of the palette's highest surface, lifted by a shadow rather than by a colour of its own.
 *
 * It was fixed white for a while, and white is a foreign patch the moment the palette comes from the
 * wallpaper: a light dynamic scheme tints every surface, so the one square that refuses to be tinted
 * is the one that looks pasted on. Material's own answer inverts the surface instead, which gives a
 * black brick in a light theme -- the single darkest object on the screen, heavier than the thing it
 * is commenting on.
 *
 * What is left is the page's own colour, raised: the same white or the same near-black the screen
 * already is, with a shadow and a hairline saying it is in front of it. That reads right under any
 * wallpaper, because it is made of the wallpaper's own colours -- and with no colour of its own to
 * do the parting, the lift has to, which is why the shadow is a step heavier than a card's.
 *
 * Built here rather than handed to [Snackbar], whose padding is fixed and generous: a line of four
 * words came out floating in the middle of a slab twice the height it needed.
 *
 * Full width and centred. Two messages of different lengths then arrive as the same shape, and a
 * line centred in a band that always looks the same is read at a glance -- which is all the time one
 * of these gets.
 */
@Composable
private fun VayouSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            // A band, not a bar: wider than any of these sentences so they all arrive as the same
            // shape, and well short of the edges so it reads as something laid over the screen
            // rather than as a new bottom to it.
            .fillMaxWidth(MessageWidthShare)
            // Announced without stealing the focus: a confirmation is worth hearing, and worth
            // hearing after whatever the reader was already saying.
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = VayouTheme.shapes.medium,
        color = VayouTheme.colors.background,
        contentColor = VayouTheme.colors.onSurface,
        // A shadow lifts it off a light page; on a dark one no shadow is visible, and the hairline
        // is what parts two dark greys. Both are cheap, and each covers where the other cannot.
        shadowElevation = MessageLift,
        border = BorderStroke(OutlineWidth, VayouTheme.colors.outlineVariant.copy(alpha = OutlineAlpha)),
    ) {
        Row(
            // Tall and narrow-margined: the height is what gives the line air to sit in, and the
            // width is the band itself. Room at the sides of a centred sentence adds nothing.
            modifier = Modifier.padding(horizontal = VayouTheme.spacing.sm, vertical = VayouTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Only on a failure, and the pair is centred together so the line does not shift
                // sideways depending on whether the news was good.
                if ((data.visuals as? MessageVisuals)?.isProblem == true) {
                    Icon(
                        imageVector = VayouIcons.Priority,
                        contentDescription = null,
                        tint = VayouTheme.colors.error,
                        modifier = Modifier.size(VayouTheme.iconSize.sm),
                    )
                }
                Text(
                    text = data.visuals.message,
                    style = VayouTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            data.visuals.actionLabel?.let { label ->
                VayouTextButton(onClick = { data.performAction() }, contentColor = VayouTheme.colors.accent) {
                    Text(text = label, style = VayouTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Three quarters of the screen: room for a short sentence and no pretence of being a bar. */
private const val MessageWidthShare = 0.75f

private val OutlineWidth = 1.dp

/** Enough to read as being in front of the page, not enough to read as a dialog. Heavier than a
 *  card's, because this one is the same colour as what it sits on. */
private val MessageLift = 8.dp

/**
 * Barely there: on a light page the shadow is what lifts the card, and this only has to keep the
 * two greys of a dark one apart. Anything heavier draws a frame around a passing sentence.
 */
private const val OutlineAlpha = 0.22f
