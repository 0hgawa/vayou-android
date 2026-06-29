package dev.vayou.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.designsystem.components.VayouSlider

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PreferenceSlider(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    trailingContent: @Composable () -> Unit = {},
) {
    VayouSegmentedListItem(
        modifier = modifier,
        onClick = {},
        onLongClick = null,
        enabled = enabled,
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        supportingContent = {
            Column {
                description?.let {
                    Text(text = description)
                }
                VayouSlider(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    value = value,
                    valueRange = valueRange,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                )
            }
        },
        content = {
            Text(text = title)
        },
        trailingContent = trailingContent,
    )
}
