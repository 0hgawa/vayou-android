package dev.vayou.core.player.service

import android.app.Activity

/**
 * Supplies the Activity class that should be launched when the user interacts with
 * the player's session notification / lock-screen controls. Each app module provides
 * its own implementation (mobile: PlayerActivity, TV: TvMainActivity).
 */
interface PlayerSessionActivityProvider {
    val activityClass: Class<out Activity>
}
