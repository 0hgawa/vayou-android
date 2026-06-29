package dev.vayou.core.ui.components

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
import dev.vayou.core.ui.designsystem.theme.VayouTheme

@Composable
fun VayouEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
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
        }
    }
}
