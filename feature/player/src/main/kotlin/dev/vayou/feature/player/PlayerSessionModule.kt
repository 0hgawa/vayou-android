package dev.vayou.feature.player

import android.app.Activity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.player.CastAvailability
import dev.vayou.core.player.SessionActivityProvider

/** Names the screen the media notification reopens, which core:player cannot see for itself. */
@Module
@InstallIn(SingletonComponent::class)
object PlayerSessionModule {

    @Provides
    fun providesSessionActivity(): SessionActivityProvider = object : SessionActivityProvider {
        override val activityClass: Class<out Activity> = PlayerActivity::class.java
    }

    /** A film in a hand is a film somebody wants on the television across the room. */
    @Provides
    fun providesCastAvailability(): CastAvailability = CastAvailability { true }
}
