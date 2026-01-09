package com.example.dwn.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadItem::class, MediaPlayStats::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun mediaPlayStatsDao(): MediaPlayStatsDao

    companion object {
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        // Migration from version 1 to 2: Add playCount and lastPlayedAt columns
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE downloads ADD COLUMN lastPlayedAt INTEGER")
            }
        }

        // Migration from version 2 to 3: Add media_play_stats table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS media_play_stats (
                        id TEXT PRIMARY KEY NOT NULL,
                        mediaUri TEXT NOT NULL,
                        mediaType TEXT NOT NULL,
                        mediaSource TEXT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT,
                        album TEXT,
                        duration INTEGER NOT NULL DEFAULT 0,
                        playCount INTEGER NOT NULL DEFAULT 0,
                        lastPlayedAt INTEGER,
                        totalPlayDuration INTEGER NOT NULL DEFAULT 0,
                        completedPlays INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_media_play_stats_mediaUri ON media_play_stats(mediaUri)")

                // Migrate existing play counts from downloads table to media_play_stats
                database.execSQL("""
                    INSERT OR IGNORE INTO media_play_stats (id, mediaUri, mediaType, mediaSource, title, playCount, lastPlayedAt, createdAt)
                    SELECT id, filePath, 
                           CASE WHEN mediaType = 'MP3' THEN 'AUDIO' ELSE 'VIDEO' END,
                           'DOWNLOAD', 
                           title, 
                           playCount, 
                           lastPlayedAt, 
                           createdAt
                    FROM downloads 
                    WHERE playCount > 0 AND status = 'COMPLETED'
                """)
            }
        }

        fun getDatabase(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DownloadDatabase::class.java,
                    "download_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

