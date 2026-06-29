package dev.vayou.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = VayouTheme.colors.onSurfaceVariant,
    shape: Shape = VayouTheme.shapes.extraSmall,
) {
    val hasBackground = backgroundColor != Color.Transparent
    Text(
        text = text,
        style = VayouTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        color = contentColor,
        modifier = if (hasBackground) {
            modifier
                .clip(shape)
                .background(backgroundColor)
                .padding(vertical = 1.dp, horizontal = 3.dp)
        } else {
            modifier
        },
    )
}
