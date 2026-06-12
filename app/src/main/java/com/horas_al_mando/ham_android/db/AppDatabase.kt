package com.horas_al_mando.ham_android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PendingFlightEntity::class, InProgressFlightPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingFlightDao(): PendingFlightDao
    abstract fun inProgressFlightDao(): InProgressFlightDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `in_progress_points` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `lat` REAL NOT NULL,
                        `lng` REAL NOT NULL,
                        `timestamp` TEXT NOT NULL,
                        `altitude` REAL NOT NULL,
                        `speed` REAL NOT NULL,
                        `heading` REAL NOT NULL,
                        `verticalSpeed` REAL NOT NULL,
                        `pressure` REAL NOT NULL
                    )"""
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ham_db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
