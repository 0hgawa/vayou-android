package dev.vayou.core.database

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.vayou.core.database.dao.CollectionDao
import dev.vayou.core.database.dao.DirectoryDao
import dev.vayou.core.database.dao.MediumDao
import dev.vayou.core.database.dao.PlaylistDao

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideMediumDao(db: MediaDatabase): MediumDao = db.mediumDao()

    @Provides
    fun provideMediumStateDao(db: MediaDatabase) = db.mediumStateDao()

    @Provides
    fun provideDirectoryDao(db: MediaDatabase): DirectoryDao = db.directoryDao()

    @Provides
    fun providePlaylistDao(db: MediaDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideCollectionDao(db: MediaDatabase): CollectionDao = db.collectionDao()
}
