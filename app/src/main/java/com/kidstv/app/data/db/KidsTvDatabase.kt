package com.kidstv.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun fromSourceType(value: VideoSourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): VideoSourceType = VideoSourceType.valueOf(value)
}

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        WatchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KidsTvDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: KidsTvDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN channelNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE playlists ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'tv'")
                db.execSQL("ALTER TABLE playlists ADD COLUMN colorHex INTEGER NOT NULL DEFAULT 4281909240")
                db.execSQL("ALTER TABLE playlists ADD COLUMN lastVideoId INTEGER")
                db.execSQL("ALTER TABLE playlists ADD COLUMN lastPositionMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN lastWatchedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): KidsTvDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KidsTvDatabase::class.java,
                    "kidstv.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
