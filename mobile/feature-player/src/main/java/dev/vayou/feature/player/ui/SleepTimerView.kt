package dev.vayou.feature.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vayou.core.ui.R
import dev.vayou.core.ui.designsystem.components.VayouFilterChip
import dev.vayou.core.ui.designsystem.components.VayouTextButton
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.feature.player.state.SleepTimerState

@Composable
fun BoxScope.SleepTimerView(
    show: Boolean,
    sleepTimerState: SleepTimerState,
    onDismiss: () -> Unit,
) {
    val remaining = sleepTimerState.formattedRemaining
    OverlayView(
        show = show,
        title = stringResource(R.string.sleep_timer),
        maxHeightFraction = 0.40f,
        trailingAction = if (remaining != null) {
            {
                VayouTextButton(
                    onClick = {
                        sleepTimerState.cancel()
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        } else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (remaining != null) {
                    Text(
                        text = remaining,
                        style = VayouTheme.typography.headlineLarge,
                        color = VayouTheme.colors.accent,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 10, 15, 20, 30, 45, 60, 90).forEach { minutes ->
                    VayouFilterChip(
                        selected = sleepTimerState.activeMinutes == minutes,
                        onClick = {
                            sleepTimerState.setTimer(minutes)
                            onDismiss()
                        },
                        label = stringResource(R.string.sleep_timer_minutes, minutes),
                        modifier = Modifier.widthIn(min = 72.dp),
                    )
                }
            }
        }
    }
}
