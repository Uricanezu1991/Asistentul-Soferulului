package com.example.driverassistant.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailySessionEntity::class, EventLogEntity::class, GpsTrackPointEntity::class, DestinationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailySessionDao(): DailySessionDao
    abstract fun eventDao(): EventDao
    abstract fun gpsTrackDao(): GpsTrackDao
    abstract fun destinationDao(): DestinationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "driver_assistant.db"
                ).build().also { INSTANCE = it }
            }
    }
}
