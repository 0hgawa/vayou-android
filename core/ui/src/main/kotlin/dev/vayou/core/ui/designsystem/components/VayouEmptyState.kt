package dev.vayou.core.ui.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.theme.VayouTheme

/**
 * What a screen shows instead of a list, and sometimes the way out of it.
 *
 * [action] is for an empty that the viewer can do something about, and only for those. Most of the
 * ones here are a statement of fact -- no folders, no results -- and a button under a fact is a
 * button that does nothing. A permission the system will no longer ask for is the opposite case:
 * without somewhere to go, the screen is a dead end the app cannot be talked out of.
 */
@Composable
fun VayouEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = VayouTheme.spacing.xxl),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = (iconTint ?: VayouTheme.colors.onSurfaceVariant).copy(alpha = 0.15f),
                modifier = Modifier.size(128.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = VayouTheme.typography.bodyLarge,
                color = VayouTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(VayouTheme.spacing.xl))
                VayouButton(onClick = onAction) { Text(text = actionLabel) }
            }
        }
    }
}
