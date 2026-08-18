package dev.vayou.feature.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import dev.vayou.core.model.ScreenOrientation

/**
 * Which way up the player opens.
 *
 * [isPortraitVideo] is only read for [ScreenOrientation.VIDEO_ORIENTATION], and is null until the
 * first frame has been measured -- until then that choice behaves like the sensor.
 */
fun Activity.applyOrientation(orientation: ScreenOrientation, isPortraitVideo: Boolean? = null) {
    requestedOrientation = when (orientation) {
        ScreenOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ScreenOrientation.LANDSCAPE_REVERSE -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        ScreenOrientation.LANDSCAPE_AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        ScreenOrientation.VIDEO_ORIENTATION -> when (isPortraitVideo) {
            true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            false -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            null -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }
}

/**
 * Pins the screen to the other way round, for the film whose shape does not match the phone's — a
 * portrait clip you want filling a landscape screen, or the reverse.
 *
 * Sensor-portrait rather than portrait, so upside down still works once pinned.
 */
fun Activity.toggleOrientation() {
    requestedOrientation = when (resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}
