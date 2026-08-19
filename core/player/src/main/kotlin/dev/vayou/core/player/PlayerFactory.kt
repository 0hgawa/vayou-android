package dev.vayou.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.smb.SmbAwareDataSourceFactory
import io.github.anilbeesetti.nextlib.media3ext.ffdecoder.NextRenderersFactory
import javax.inject.Inject

/**
 * Builds a player for one screen's lifetime.
 *
 * Not a singleton and not injected as one: a player holds a codec and a surface, and one kept alive
 * between screens is one holding hardware nothing is using. The screen that opens it releases it.
 */
@OptIn(UnstableApi::class)
class PlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    /** So a file on a share opens the same way a file on this phone does. */
    private val dataSources: SmbAwareDataSourceFactory,
    /** Read at build time, because which decoder to try and who gets a say in the sound are both
     *  fixed for a player's lifetime. Changing either takes effect on the next film. */
    private val preferencesRepository: PreferencesRepository,
) {

    fun create(): ExoPlayer {
        val preferences = preferencesRepository.playerPreferences.value

        val renderers = NextRenderersFactory(context)
            // Falling back rather than preferring, by default: hardware decoding is what keeps a
            // phone cool, and the bundled decoder is for the files it will not open.
            .setEnableDecoderFallback(preferences.decoderPriority != DecoderPriority.DEVICE_ONLY)
            .setExtensionRendererMode(
                when (preferences.decoderPriority) {
                    DecoderPriority.PREFER_DEVICE -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                    DecoderPriority.PREFER_APP -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    DecoderPriority.DEVICE_ONLY -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                },
            )

        return ExoPlayer.Builder(context, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSources))
            .setLoadControl(startFastLoadControl())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                // Pauses for a phone call and ducks for a notification, rather than talking over
                // whatever else the phone is doing.
                preferences.requireAudioFocus,
            )
            // Stops when the headphones come out, rather than playing the film to the room.
            .setHandleAudioBecomingNoisy(preferences.pauseOnHeadsetDisconnect)
            .build()
    }
}

/**
 * How much has to be buffered before playback may start, which is not the same question as how much
 * to buffer.
 *
 * ExoPlayer waits for two and a half seconds of media before it shows the first frame, and five more
 * after a stall. That is a figure for a stream arriving over a network at an unknown rate: it buys
 * the odds that the next two and a half seconds are already in hand. A file on this device is not
 * arriving at an unknown rate, so the wait buys nothing and costs a spinner on every video opened
 * from the library.
 *
 * A quarter of a second is enough to have decoded something to show. What is *kept* buffered is left
 * at the defaults, which is the part that matters over a network: the same player opens files from a
 * server, and this changes when it begins, not how far ahead it reads.
 */
@OptIn(UnstableApi::class)
private fun startFastLoadControl(): LoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
        BufferForPlaybackMs,
        BufferForPlaybackAfterRebufferMs,
    )
    .build()

/** Enough to have decoded something to show, and no more. */
private const val BufferForPlaybackMs = 250

/** Longer than the first, shorter than the default's five seconds: a stall means the source is
 *  struggling, so give it a moment — but a moment, not a pause you sit through. */
private const val BufferForPlaybackAfterRebufferMs = 1_000
