package dev.vayou.feature.player.cast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import dev.vayou.core.ui.R
import dev.vayou.core.ui.components.VayouIconButton
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun CastMiniController(
    modifier: Modifier = Modifier,
    onExpandClick: () -> Unit,
) {
    val castPlayer = CastSessionManager.castPlayer
    val deviceName = CastSessionManager.deviceName
    val isVisible = castPlayer != null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        val player = castPlayer ?: return@AnimatedVisibility
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()
            ?: player.currentMediaItem?.localConfiguration?.uri?.lastPathSegment
            ?: ""

        var isPlayingLocal by remember { mutableStateOf(player.playWhenReady) }
        var progress by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            while (true) {
                val dur = player.duration.coerceAtLeast(1)
                progress = player.currentPosition.toFloat() / dur
                isPlayingLocal = player.playWhenReady && player.playbackState == Player.STATE_READY
                delay(500)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VayouTheme.colors.surfaceContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(VayouTheme.colors.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(VayouTheme.colors.accent),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandClick)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val mediaUri = player.currentMediaItem?.localConfiguration?.uri
                    ?: player.currentMediaItem?.mediaId

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(VayouTheme.colors.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mediaUri != null) {
                        AsyncImage(
                            model = mediaUri,
                            contentDescription = null,
                            modifier = Modifier
                                .height(40.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = VayouIcons.Cast,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = VayouTheme.colors.onSurfaceVariant,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifEmpty { deviceName ?: stringResource(R.string.cast) },
                        style = VayouTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = VayouTheme.colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (title.isNotEmpty()) {
                        Text(
                            text = deviceName ?: stringResource(R.string.cast),
                            style = VayouTheme.typography.labelSmall,
                            color = VayouTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                VayouIconButton(
                    onClick = {
                        isPlayingLocal = !isPlayingLocal
                        if (player.playWhenReady) player.pause() else player.play()
                    },
                ) {
                    Icon(
                        imageVector = if (isPlayingLocal) VayouIcons.Pause else VayouIcons.Play,
                        contentDescription = null,
                        tint = VayouTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}
