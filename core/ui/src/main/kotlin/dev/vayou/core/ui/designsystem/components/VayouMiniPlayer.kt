package dev.vayou.core.ui.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What is playing, reduced to a bar above the navigation.
 *
 * It slides rather than fades: the full player it reopens comes up from the bottom, and the bar is
 * where it goes when it is put away, so the two ends of the gesture agree.
 *
 * The hairline of progress along the bottom is the whole reason it can be this short. Without it
 * the bar says what is playing and nothing about how far in, and a listener who wants that has to
 * open the player to find out.
 */
@Composable
fun VayouMiniPlayer(
    visible: Boolean,
    title: String,
    subtitle: String,
    /** 0f..1f. */
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Only the sides: what sits under this bar owns the bottom inset, and taking it
                // here as well would lift the bar off whatever it is standing on.
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = OuterHorizontal, vertical = OuterVertical)
                .clip(VayouTheme.shapes.medium)
                .background(VayouTheme.colors.surfaceContainerHigh),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(start = VayouTheme.spacing.lg, end = VayouTheme.spacing.sm)
                    .padding(vertical = VayouTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.md),
            ) {
                leading()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = VayouTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = VayouTheme.colors.onSurface,
                        maxLines = 1,
                        // A long title travels rather than being cut. It only runs when the text
                        // actually overflows, so a short one stays still.
                        modifier = Modifier.basicMarquee(),
                    )
                    Text(
                        text = subtitle,
                        style = VayouTheme.typography.labelSmall,
                        color = VayouTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                actions()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .background(VayouTheme.colors.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(ProgressHeight)
                        .background(VayouTheme.colors.accent),
                )
            }
        }
    }
}

private val OuterHorizontal = 8.dp

private val OuterVertical = 6.dp

private val ProgressHeight = 2.dp
