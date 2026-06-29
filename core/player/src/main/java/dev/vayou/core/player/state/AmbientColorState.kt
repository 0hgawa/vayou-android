package dev.vayou.core.player.state

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun rememberAmbientColor(player: Player, surfaceView: SurfaceView? = null): Color {
    var color by remember { mutableStateOf(Color.Black) }
    val currentSurfaceView by rememberUpdatedState(surfaceView)

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 1000),
        label = "ambientColor",
    )

    LaunchedEffect(player) {
        player.listen { events ->
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                color = Color.Black
            }
            if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                captureFrameColor(currentSurfaceView) { color = it }
            }
        }
    }

    return animatedColor
}

private fun captureFrameColor(surfaceView: SurfaceView?, onColor: (Color) -> Unit) {
    if (surfaceView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    PixelCopy.request(surfaceView, bitmap, { result ->
        if (result == PixelCopy.SUCCESS) {
            val scaled = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
            onColor(Color(scaled.getPixel(0, 0)))
            scaled.recycle()
        }
        bitmap.recycle()
    }, Handler(Looper.getMainLooper()))
}
