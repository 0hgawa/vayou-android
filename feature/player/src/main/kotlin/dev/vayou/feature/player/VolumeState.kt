package dev.vayou.feature.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.media3.session.MediaController
import dev.vayou.core.player.MaxVolumeBoostMillibels
import dev.vayou.core.player.NoVolumeBoost
import dev.vayou.core.player.isVolumeBoostSupported
import dev.vayou.core.player.setVolumeBoost

/**
 * Media volume, as the system holds it, and past it.
 *
 * One number covering two mechanisms. Up to the device's own maximum this is the stream volume, so
 * the hardware keys and this agree. Above it the stream stays at its top and the difference becomes
 * a gain on the audio session -- which is the only way to make a film mixed too quietly audible on
 * a phone, and the reason the range goes to twice what the device offers.
 */
@Stable
class VolumeState(private val audioManager: AudioManager) {

    /** As loud as the device will go on its own. */
    private val systemMax: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /** False until the service has been asked, and on any device whose framework will not amplify. */
    private var canBoost: Boolean by mutableStateOf(false)

    private var controller: MediaController? = null

    /** Steps, counting past [systemMax] into the boosted range when there is one. */
    var value: Int by mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        private set

    val max: Int get() = if (canBoost) systemMax * BoostedRangeFactor else systemMax

    /** What the readout says: a hundred at the device's own maximum, two hundred at the top. */
    val percent: Int get() = value * FullVolumePercent / systemMax

    /**
     * Ask the service whether this device amplifies, once, if [isAllowed] at all.
     *
     * Until it answers the range is the device's own, which is the honest state: a readout offering
     * 200% on a phone that cannot reach it is a control that lies about what it did.
     *
     * Turning the setting off while a film is playing gives the gain back straight away rather than
     * waiting for the next one -- and brings the level down with it, since the top half of the
     * range it was sitting in no longer exists.
     */
    suspend fun bind(controller: MediaController, isAllowed: Boolean) {
        this.controller = controller
        canBoost = isAllowed && controller.isVolumeBoostSupported()
        if (canBoost || value <= systemMax) return
        value = systemMax
        controller.setVolumeBoost(NoVolumeBoost)
    }

    /** [fraction] of the whole range, so a drag of the same length moves the same amount on a phone
     *  with fifteen steps and one with a hundred. */
    fun nudge(fraction: Float) {
        val next = (value + fraction * max).toInt().coerceIn(0, max)
        if (next == value) return

        val wasBoost = boostFor(value)
        value = next
        // No UI flag: this app is already drawing a readout, and the system's panel over it would
        // be two answers to one gesture.
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next.coerceAtMost(systemMax), 0)

        // Only when it moves. Most of a drag happens below the device's own maximum, where the gain
        // is zero at both ends of every step, and a session command per frame to say so is a
        // round trip per frame for no change.
        val boost = boostFor(next)
        if (boost != wasBoost) controller?.setVolumeBoost(boost)
    }

    /** Everything above the device's own maximum, as a share of the amplification on offer. */
    private fun boostFor(steps: Int): Int = (steps - systemMax).coerceAtLeast(0) *
        MaxVolumeBoostMillibels / systemMax
}

@Composable
fun rememberVolumeState(context: Context): VolumeState =
    remember(context) { VolumeState(requireNotNull(context.getSystemService())) }

/** Twice the device's own range: the top half is amplified. */
private const val BoostedRangeFactor = 2

/** The device's own maximum, as the readout counts it. Above this the gain takes over. */
private const val FullVolumePercent = 100
