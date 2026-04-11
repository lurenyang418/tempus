package com.cappielloantonio.tempo.database

import androidx.media3.common.util.UnstableApi
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cappielloantonio.tempo.App
import com.cappielloantonio.tempo.database.converter.DateConverters
import com.cappielloantonio.tempo.database.dao.ChronologyDao
import com.cappielloantonio.tempo.database.dao.DownloadDao
import com.cappielloantonio.tempo.database.dao.FavoriteDao
import com.cappielloantonio.tempo.database.dao.LyricsDao
import com.cappielloantonio.tempo.database.dao.PlaylistDao
import com.cappielloantonio.tempo.database.dao.QueueDao
import com.cappielloantonio.tempo.database.dao.RecentSearchDao
import com.cappielloantonio.tempo.database.dao.ServerDao
import com.cappielloantonio.tempo.database.dao.SessionMediaItemDao
import com.cappielloantonio.tempo.model.Chronology
import com.cappielloantonio.tempo.model.Download
import com.cappielloantonio.tempo.model.Favorite
import com.cappielloantonio.tempo.model.LyricsCache
import com.cappielloantonio.tempo.model.Queue
import com.cappielloantonio.tempo.model.RecentSearch
import com.cappielloantonio.tempo.model.Server
import com.cappielloantonio.tempo.model.SessionMediaItem
import com.cappielloantonio.tempo.subsonic.models.Playlist

@UnstableApi
@Database(
    version = 14,
    entities = [Queue::class, Server::class, RecentSearch::class, Download::class, Chronology::class, Favorite::class, SessionMediaItem::class, Playlist::class, LyricsCache::class],
    autoMigrations = [AutoMigration(from = 10, to = 11), AutoMigration(
        from = 11,
        to = 12
    ), AutoMigration(from = 13, to = 14)]
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao?

    abstract fun serverDao(): ServerDao?

    abstract fun recentSearchDao(): RecentSearchDao?

    abstract fun downloadDao(): DownloadDao?

    abstract fun chronologyDao(): ChronologyDao?

    abstract fun favoriteDao(): FavoriteDao?

    abstract fun sessionMediaItemDao(): SessionMediaItemDao?

    abstract fun playlistDao(): PlaylistDao?

    abstract fun lyricsDao(): LyricsDao?

    companion object {
        private const val DB_NAME = "tempo_db"

        @JvmStatic
        @get:Synchronized
        var instance: AppDatabase? = null
            get() {
                if (field == null) {
                    field = databaseBuilder(
                        App.getContext()!!,
                        AppDatabase::class.java,
                        DB_NAME
                    )
                        .fallbackToDestructiveMigration(true)
                        .build()
                }

                return field
            }
            private set
    }
}
