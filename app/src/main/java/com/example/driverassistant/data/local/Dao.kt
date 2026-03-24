package com.example.driverassistant.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySessionDao {
    @Insert
    suspend fun insert(entity: DailySessionEntity): Long

    @Update
    suspend fun update(entity: DailySessionEntity)

    @Query("SELECT * FROM daily_session WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1")
    suspend fun getOpenSession(): DailySessionEntity?

    @Query("SELECT * FROM daily_session ORDER BY id DESC LIMIT 1")
    fun observeLatest(): Flow<DailySessionEntity?>

    @Query("SELECT * FROM daily_session ORDER BY startTimeEpochSeconds DESC LIMIT :limit")
    fun observeRecent(limit: Int = 28): Flow<List<DailySessionEntity>>

    @Query("SELECT * FROM daily_session WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): DailySessionEntity?

    @Query("SELECT COALESCE(SUM(totalDriveSeconds), 0) FROM daily_session WHERE startTimeEpochSeconds >= :fromEpochSeconds")
    suspend fun sumDriveSince(fromEpochSeconds: Long): Long

    @Query("SELECT COALESCE(SUM(totalDistanceMeters), 0) FROM daily_session WHERE startTimeEpochSeconds >= :fromEpochSeconds")
    suspend fun sumDistanceSince(fromEpochSeconds: Long): Long

    @Query("SELECT COUNT(*) FROM daily_session WHERE startTimeEpochSeconds >= :fromEpochSeconds AND totalDriveSeconds > :thresholdSeconds")
    suspend fun countSessionsOverDriveSince(fromEpochSeconds: Long, thresholdSeconds: Long = 9 * 3600): Int
}

@Dao
interface EventDao {
    @Insert
    suspend fun insert(entity: EventLogEntity): Long

    @Query("SELECT * FROM event_log WHERE sessionId = :sessionId ORDER BY timestampEpochSeconds ASC")
    suspend fun eventsForSession(sessionId: Long): List<EventLogEntity>

    @Query("SELECT * FROM event_log ORDER BY timestampEpochSeconds DESC LIMIT 50")
    fun observeLatest(): Flow<List<EventLogEntity>>
}

@Dao
interface GpsTrackDao {
    @Insert
    suspend fun insert(entity: GpsTrackPointEntity): Long

    @Query("SELECT * FROM gps_track_point WHERE sessionId = :sessionId ORDER BY timestampEpochSeconds ASC")
    suspend fun pointsForSession(sessionId: Long): List<GpsTrackPointEntity>
}

@Dao
interface DestinationDao {
    @Insert
    suspend fun insert(entity: DestinationEntity): Long
}
