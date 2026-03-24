package com.example.driverassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_session")
data class DailySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeEpochSeconds: Long,
    val endTimeEpochSeconds: Long? = null,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val totalDistanceMeters: Long = 0,
    val totalDriveSeconds: Long = 0,
    val totalBreakSeconds: Long = 0,
    val totalWorkSeconds: Long = 0,
    val status: String = "OPEN"
)

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val eventType: String,
    val timestampEpochSeconds: Long,
    val lat: Double? = null,
    val lng: Double? = null,
    val notes: String? = null
)

@Entity(tableName = "gps_track_point")
data class GpsTrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampEpochSeconds: Long,
    val lat: Double,
    val lng: Double,
    val speedMps: Float? = null,
    val accuracyMeters: Float? = null
)

@Entity(tableName = "destination")
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val address: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val etaEpochSeconds: Long? = null,
    val remainingDistanceMeters: Int? = null,
    val remainingDurationSeconds: Int? = null
)
