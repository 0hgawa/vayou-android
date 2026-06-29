package dev.vayou.feature.player.buttons

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import dev.vayou.feature.player.LocalUseMaterialYouControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val WhiteRipple = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f),
)
private val NoRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(0f, 0f, 0f, 0f),
)
private val MaterialYouContainerColor = Color.White.copy(alpha = 0.1f)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    showBackground: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(interactionSource) {
        var isLongPressClicked = false
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongPressClicked = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    onLongClick?.let {
                        isLongPressClicked = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        it.invoke()
                    }
                }
                is PressInteraction.Release -> {
                    if (!isLongPressClicked) onClick()
                }
            }
        }
    }

    val useMaterialYou = LocalUseMaterialYouControls.current
    val resolvedContainer = when {
        containerColor != Color.Transparent -> containerColor
        useMaterialYou && showBackground -> MaterialYouContainerColor
        else -> Color.Transparent
    }

    CompositionLocalProvider(
        LocalContentColor provides Color.White,
        LocalRippleConfiguration provides if (useMaterialYou) WhiteRipple else NoRipple,
    ) {
        IconButton(
            onClick = {},
            enabled = isEnabled,
            modifier = modifier.size(if (useMaterialYou && showBackground) PlayerButtonSize.StandardCompact else PlayerButtonSize.Standard),
            interactionSource = interactionSource,
            colors = IconButtonDefaults.iconButtonColors(containerColor = resolvedContainer),
            content = content,
        )
    }
}
