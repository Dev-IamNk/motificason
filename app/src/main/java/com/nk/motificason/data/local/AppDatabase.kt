package com.nk.motificason.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LockInEntity::class,
        HabitEntity::class,
        CheckInEntity::class,
        HabitStreakEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lockInDao(): LockInDao
    abstract fun habitDao(): HabitDao
    abstract fun checkInDao(): CheckInDao
    abstract fun habitStreakDao(): HabitStreakDao
}

object AppDatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun init(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "motificason_local.db"
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }

    fun getDatabase(): AppDatabase {
        return checkNotNull(instance) {
            "AppDatabaseProvider has not been initialized. Call init(context) in Application."
        }
    }

    fun getDatabaseOrNull(): AppDatabase? = instance
}
