package dev.vayou.core.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.vayou.core.ui.designsystem.VayouIcons
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun PlaylistThumbnail(
    thumbnailUris: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(VayouTheme.shapes.small)
            .background(VayouTheme.colors.surfaceContainerHigh)
            .aspectRatio(16f / 10f),
        contentAlignment = Alignment.Center,
    ) {
        when {
            thumbnailUris.isEmpty() -> {
                Icon(
                    imageVector = VayouIcons.PlaylistFilled,
                    contentDescription = null,
                    tint = VayouTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize(0.4f),
                )
            }
            thumbnailUris.size == 1 -> {
                ThumbnailCell(context = context, uri = thumbnailUris[0], modifier = Modifier.fillMaxSize())
            }
            thumbnailUris.size <= 3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    ThumbnailCell(context, thumbnailUris[0], Modifier.weight(1f).fillMaxHeight())
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(VayouTheme.colors.surface))
                    ThumbnailCell(context, thumbnailUris[1], Modifier.weight(1f).fillMaxHeight())
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ThumbnailCell(context, thumbnailUris[0], Modifier.weight(1f).fillMaxHeight())
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(VayouTheme.colors.surface))
                        ThumbnailCell(context, thumbnailUris[1], Modifier.weight(1f).fillMaxHeight())
                    }
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(VayouTheme.colors.surface))
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ThumbnailCell(context, thumbnailUris[2], Modifier.weight(1f).fillMaxHeight())
                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(VayouTheme.colors.surface))
                        ThumbnailCell(context, thumbnailUris[3], Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailCell(context: Context, uri: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(VayouTheme.colors.surfaceContainerHigh)) {
        Icon(
            imageVector = VayouIcons.Video,
            contentDescription = null,
            tint = VayouTheme.colors.surfaceContainerHighest,
            modifier = Modifier.align(Alignment.Center).fillMaxSize(0.5f),
        )
        AsyncImage(
            model = remember(uri) { ImageRequest.Builder(context).data(uri).crossfade(true).build() },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
