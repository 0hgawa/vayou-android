package dev.vayou.tv

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.player.CastAvailability
import dev.vayou.core.player.SessionActivityProvider
import javax.inject.Singleton

/**
 * Which screen the playback notification reopens on a television.
 *
 * The service is shared with the phone and cannot name either screen itself, so each shell says
 * which of them it is. This is also why the television cannot simply take the phone's player
 * feature along with it: that module answers the same question with the phone's activity, and two
 * answers in one graph is a build that does not start.
 */
@Module
@InstallIn(SingletonComponent::class)
object TvSessionModule {

    @Provides
    @Singleton
    fun providesSessionActivity(): SessionActivityProvider = object : SessionActivityProvider {
        override val activityClass = TvMainActivity::class.java
    }

    /**
     * Never, and not for want of trying: this is the receiver.
     *
     * It is the first channel of an evening that pays for the answer being yes -- the service is
     * built on the first thing played, and building the cast wrapper loads a Play services module
     * and stands up a web server before a single byte of the stream is asked for.
     */
    @Provides
    @Singleton
    fun providesCastAvailability(): CastAvailability = CastAvailability { false }
}
