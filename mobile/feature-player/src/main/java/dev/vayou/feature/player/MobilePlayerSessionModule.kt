package dev.vayou.feature.player

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.player.service.PlayerSessionActivityProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MobilePlayerSessionActivityProvider @Inject constructor() : PlayerSessionActivityProvider {
    override val activityClass: Class<PlayerActivity> = PlayerActivity::class.java
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MobilePlayerSessionModule {
    @Binds
    abstract fun provider(
        impl: MobilePlayerSessionActivityProvider,
    ): PlayerSessionActivityProvider
}
