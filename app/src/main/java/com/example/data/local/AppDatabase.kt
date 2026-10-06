package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Movie
import com.example.data.model.PlaybackProgress
import com.example.data.model.Profile
import com.example.data.model.UserAccount
import com.example.data.model.Watchlist

@Database(
    entities = [
        UserAccount::class,
        Profile::class,
        Movie::class,
        Watchlist::class,
        PlaybackProgress::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN episodeIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN episodeNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN seasonNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN episodeTitle TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        email TEXT NOT NULL,
                        password TEXT NOT NULL,
                        name TEXT NOT NULL,
                        isAdmin INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN isApproved INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE profiles ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "primeplex_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
