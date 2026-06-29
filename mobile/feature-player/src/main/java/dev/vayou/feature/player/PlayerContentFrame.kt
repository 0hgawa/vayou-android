package dev.vayou.feature.player

import android.graphics.Rect
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import dev.vayou.feature.player.extensions.toContentScale
import dev.vayou.feature.player.state.ControlsVisibilityState
import dev.vayou.feature.player.state.PictureInPictureState
import dev.vayou.feature.player.state.SeekGestureState
import dev.vayou.feature.player.state.TapGestureState
import dev.vayou.core.player.state.VideoZoomAndContentScaleState
import dev.vayou.feature.player.state.VolumeAndBrightnessGestureState
import dev.vayou.feature.player.ui.PlayerGestures
import dev.vayou.feature.player.ui.ShutterView
import dev.vayou.core.player.ui.SubtitleConfiguration
import dev.vayou.core.player.ui.SubtitleView

@OptIn(UnstableApi::class)
@Composable
fun PlayerContentFrame(
    modifier: Modifier = Modifier,
    player: Player,
    pictureInPictureState: PictureInPictureState,
    controlsVisibilityState: ControlsVisibilityState,
    tapGestureState: TapGestureState,
    seekGestureState: SeekGestureState,
    videoZoomAndContentScaleState: VideoZoomAndContentScaleState,
    volumeAndBrightnessGestureState: VolumeAndBrightnessGestureState,
    subtitleConfiguration: SubtitleConfiguration,
    onSurfaceViewCreated: ((SurfaceView) -> Unit)? = null,
) {
    val presentationState = rememberPresentationState(player)
    val density = LocalDensity.current
    val contentScale = videoZoomAndContentScaleState.videoContentScale.toContentScale()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }

        val surfaceModifier = Modifier.resizeWithContentScale(
            contentScale = contentScale,
            sourceSizeDp = presentationState.videoSizeDp?.let { size ->
                size.copy(
                    width = with(density) { size.width.toDp().value },
                    height = with(density) { size.height.toDp().value },
                )
            },
        )

        AndroidView(
            factory = { context ->
                SurfaceView(context).also { surface ->
                    player.setVideoSurfaceView(surface)
                    onSurfaceViewCreated?.invoke(surface)
                }
            },
            modifier = surfaceModifier
                .onGloballyPositioned {
                    val bounds = it.boundsInWindow()
                    pictureInPictureState.setVideoViewRect(
                        Rect(
                            bounds.left.toInt(),
                            bounds.top.toInt(),
                            bounds.right.toInt(),
                            bounds.bottom.toInt(),
                        ),
                    )
                }
                .graphicsLayer {
                    scaleX = videoZoomAndContentScaleState.zoom
                    scaleY = videoZoomAndContentScaleState.zoom
                    translationX = videoZoomAndContentScaleState.offset.x
                    translationY = videoZoomAndContentScaleState.offset.y
                },
        )

        SubtitleView(
            modifier = surfaceModifier.graphicsLayer {
                translationY = containerHeightPx * subtitleConfiguration.verticalPosition
            },
            player = player,
            configuration = subtitleConfiguration,
        )

        if (presentationState.coverSurface) {
            ShutterView()
        }

        DisposableEffect(player) {
            onDispose { player.clearVideoSurface() }
        }
    }

    PlayerGestures(
        controlsVisibilityState = controlsVisibilityState,
        tapGestureState = tapGestureState,
        pictureInPictureState = pictureInPictureState,
        seekGestureState = seekGestureState,
        videoZoomAndContentScaleState = videoZoomAndContentScaleState,
        volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
    )
}
