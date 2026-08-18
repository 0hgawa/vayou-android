package dev.vayou.feature.player

import android.app.Activity
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Screen brightness, for this window only.
 *
 * Set on the window rather than through `Settings.System`, which needs a permission and would leave
 * the phone dimmed after the viewer has left. Android restores it when the window goes away.
 */
@Stable
class BrightnessState(
    private val activity: Activity,
    /** What the last film was left at, for a viewer who asked to be met there. */
    remembered: Float?,
    private val onChanged: (Float) -> Unit,
) {

    var value: Float by mutableFloatStateOf(remembered ?: activity.initialBrightness())
        private set

    init {
        // Applied straight away when it was remembered. Without this the window opens at whatever
        // the phone is on and only moves on the first drag, which is the setting doing nothing.
        if (remembered != null) apply(remembered)
    }

    fun nudge(delta: Float) {
        value = (value + delta).coerceIn(0f, 1f)
        apply(value)
        onChanged(value)
    }

    private fun apply(level: Float) {
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = level }
    }
}

@Composable
fun rememberBrightnessState(activity: Activity, remembered: Float?, onChanged: (Float) -> Unit): BrightnessState =
    remember(activity) { BrightnessState(activity, remembered, onChanged) }

/**
 * What the window is already at, or -- the first time, when it is at
 * [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE] -- what the phone is at, so the first drag
 * moves from where the viewer can see rather than jumping.
 */
private fun Activity.initialBrightness(): Float {
    val windowValue = window.attributes.screenBrightness
    if (windowValue >= 0f) return windowValue

    val system = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, SystemBrightnessMax / 2)
    return (system.toFloat() / SystemBrightnessMax).coerceIn(0f, 1f)
}

/** `Settings.System.SCREEN_BRIGHTNESS` is 0..255 regardless of what the window takes. */
private const val SystemBrightnessMax = 255
