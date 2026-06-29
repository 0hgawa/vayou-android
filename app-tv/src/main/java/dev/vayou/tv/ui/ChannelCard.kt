package dev.vayou.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.vayou.core.smb.PlaylistChannel
import dev.vayou.core.ui.designsystem.theme.VayouTheme

val ChannelCardHeight = 88.dp
private val LogoSize = 72.dp

@Composable
fun ChannelCard(
    channel: PlaylistChannel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(ChannelCardHeight)
            .tvLongPressClickable(onLongPress),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = TvDimensions.FocusScale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            contentColor = cs.onBackground,
            focusedContentColor = cs.onBackground,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(cs.surfaceVariant.copy(alpha = if (isFocused) 0.40f else 0.18f))
                .border(
                    width = 2.dp,
                    color = if (isFocused) cs.border else Color.Transparent,
                    shape = MaterialTheme.shapes.medium,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(VayouTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(LogoSize)
                        .clip(MaterialTheme.shapes.small)
                        .background(cs.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!channel.logo.isNullOrBlank()) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.LiveTv,
                            contentDescription = null,
                            modifier = Modifier.size(VayouTheme.iconSize.lg),
                            tint = cs.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = VayouTheme.spacing.md, end = VayouTheme.spacing.xxl)
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = if (isFocused) Int.MAX_VALUE else 0),
                    )
                    if (!channel.group.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        GroupPill(label = channel.group.orEmpty())
                    }
                }
            }
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(VayouTheme.spacing.sm)
                        .size(VayouTheme.iconSize.xs),
                )
            }
        }
    }
}

@Composable
private fun GroupPill(label: String) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(cs.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
