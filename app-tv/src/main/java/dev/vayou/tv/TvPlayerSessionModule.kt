package dev.vayou.tv

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.player.service.PlayerSessionActivityProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvPlayerSessionActivityProvider @Inject constructor() : PlayerSessionActivityProvider {
    override val activityClass: Class<TvMainActivity> = TvMainActivity::class.java
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TvPlayerSessionModule {
    @Binds
    abstract fun provider(
        impl: TvPlayerSessionActivityProvider,
    ): PlayerSessionActivityProvider
}
