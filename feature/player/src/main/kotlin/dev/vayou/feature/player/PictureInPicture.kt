package dev.vayou.feature.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.common.Player

/**
 * The floating window, and the one button on it.
 *
 * Everything here is API 26 and up. Below that the mode does not exist, the caller checks
 * [isSupported] first, and nothing on screen offers it.
 */
object PictureInPicture {

    val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    /**
     * The window takes the film's own shape, so a wide film is not letterboxed twice -- once by the
     * player and again by a square window. Android refuses ratios beyond roughly 2.39:1, so the
     * value is clamped rather than allowed to throw.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun paramsFor(player: Player, context: Context, isPlaying: Boolean): PictureInPictureParams {
        val size = player.videoSize
        val builder = PictureInPictureParams.Builder()
            .setActions(listOf(playPauseAction(context, isPlaying)))

        if (size.width > 0 && size.height > 0) {
            val ratio = size.width.toFloat() / size.height
            val clamped = ratio.coerceIn(MinRatio, MaxRatio)
            builder.setAspectRatio(Rational((clamped * RatioScale).toInt(), RatioScale))
        }
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun playPauseAction(context: Context, isPlaying: Boolean): RemoteAction {
        val title = context.getString(if (isPlaying) R.string.pause else R.string.play)
        return RemoteAction(
            Icon.createWithResource(
                context,
                if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
            ),
            title,
            title,
            PendingIntent.getBroadcast(
                context,
                RequestCode,
                Intent(ActionToggle).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    /** Listens for the button in the floating window, which is a broadcast and not a click. */
    fun registerToggleReceiver(activity: ComponentActivity, onToggle: () -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ActionToggle) onToggle()
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ActionToggle),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        return receiver
    }

    private const val ActionToggle = "dev.vayou.player.PIP_TOGGLE"

    private const val RequestCode = 1

    /** What Android accepts, minus a hair, so a 2.40:1 film rounds down instead of being rejected. */
    private const val MinRatio = 0.42f

    private const val MaxRatio = 2.38f

    /** Rational wants integers; a thousandth is finer than any window is measured in. */
    private const val RatioScale = 1000
}
