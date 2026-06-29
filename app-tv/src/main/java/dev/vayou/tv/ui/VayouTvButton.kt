package dev.vayou.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.vayou.core.ui.designsystem.theme.VayouTheme

/**
 * Canonical TV action button (chip / icon button / dialog button).
 *
 * - Icon-only: pass [icon], leave [label] null → 44dp square.
 * - Icon + label: pass both → pill with 16dp horizontal padding.
 * - Label only: pass just [label] → pill with 20dp horizontal padding.
 *
 * [primary] = emphasized variant (filled with `primary` color, no focus tint).
 * Default variant uses `surfaceVariant` resting and `inverseSurface` on focus,
 * matching the rest of the app (settings menu, content cards on focus).
 */
@Composable
fun VayouTvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    contentDescription: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.extraLarge),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (primary) cs.primary else Color.Transparent,
            focusedContainerColor = if (primary) cs.primary else cs.inverseSurface,
            disabledContainerColor = if (primary) cs.primary.copy(alpha = 0.4f) else Color.Transparent,
            contentColor = if (primary) cs.onPrimary else cs.onSurface,
            focusedContentColor = if (primary) cs.onPrimary else cs.inverseOnSurface,
            disabledContentColor = if (primary) cs.onPrimary.copy(alpha = 0.6f) else cs.onSurface.copy(alpha = 0.4f),
        ),
    ) {
        if (label == null && icon != null) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(VayouTheme.iconSize.sm))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = if (icon != null) VayouTheme.spacing.lg else VayouTheme.spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VayouTheme.spacing.sm),
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(VayouTheme.iconSize.sm))
                }
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
