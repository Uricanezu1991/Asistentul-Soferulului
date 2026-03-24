package com.example.driverassistant.data.local

import android.location.Location
import com.example.driverassistant.domain.model.EventType
import com.example.driverassistant.domain.model.RouteSummary
import com.example.driverassistant.domain.model.SessionHistoryItem
import com.example.driverassistant.network.RouteApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DriverRepository(
    private val eventDao: EventDao,
    private val sessionDao: DailySessionDao,
    private val gpsDao: GpsTrackDao,
    private val destinationDao: DestinationDao,
    private val routeApi: RouteApi
) {
    fun observeLatestSession(): Flow<DailySessionEntity?> = sessionDao.observeLatest()
    fun observeLatestEvents(): Flow<List<EventLogEntity>> = eventDao.observeLatest()
    fun observeRecentSessions(limit: Int = 28): Flow<List<SessionHistoryItem>> =
        sessionDao.observeRecent(limit).map { sessions ->
            sessions.map {
                SessionHistoryItem(
                    id = it.id,
                    startEpochSeconds = it.startTimeEpochSeconds,
                    endEpochSeconds = it.endTimeEpochSeconds,
                    driveSeconds = it.totalDriveSeconds,
                    breakSeconds = it.totalBreakSeconds,
                    workSeconds = it.totalWorkSeconds,
                    distanceMeters = it.totalDistanceMeters,
                    status = it.status
                )
            }
        }

    suspend fun startDay(lat: Double?, lng: Double?): Long {
        val session = DailySessionEntity(
            startTimeEpochSeconds = now(),
            startLat = lat,
            startLng = lng,
            status = "OPEN"
        )
        val id = sessionDao.insert(session)
        eventDao.insert(EventLogEntity(sessionId = id, eventType = EventType.START_DAY.name, timestampEpochSeconds = now(), lat = lat, lng = lng))
        return id
    }

    suspend fun getOrCreateOpenSession(lat: Double?, lng: Double?): Long {
        val open = sessionDao.getOpenSession()
        return open?.id ?: startDay(lat, lng)
    }

    suspend fun logEvent(sessionId: Long, type: EventType, lat: Double?, lng: Double?) {
        eventDao.insert(
            EventLogEntity(
                sessionId = sessionId,
                eventType = type.name,
                timestampEpochSeconds = now(),
                lat = lat,
                lng = lng
            )
        )
        if (lat != null && lng != null) {
            gpsDao.insert(GpsTrackPointEntity(sessionId = sessionId, timestampEpochSeconds = now(), lat = lat, lng = lng))
        }
    }

    suspend fun stopDay(sessionId: Long, lat: Double?, lng: Double?) {
        logEvent(sessionId, EventType.STOP_DAY, lat, lng)
        val open = sessionDao.getById(sessionId) ?: return
        val events = eventDao.eventsForSession(sessionId)
        val points = gpsDao.pointsForSession(sessionId)
        val totals = calculateTotals(events)
        val totalDistance = calculateDistanceMeters(points)
        sessionDao.update(
            open.copy(
                endTimeEpochSeconds = now(),
                endLat = lat,
                endLng = lng,
                totalDistanceMeters = totalDistance,
                totalDriveSeconds = totals.driveSeconds,
                totalBreakSeconds = totals.breakSeconds,
                totalWorkSeconds = totals.workSeconds,
                status = "CLOSED"
            )
        )
    }

    suspend fun fetchRouteSummary(originLat: Double, originLng: Double, destinationText: String): RouteSummary {
        return try {
            val response = routeApi.computeRoute(
                mapOf(
                    "origin" to mapOf("location" to mapOf("latLng" to mapOf("latitude" to originLat, "longitude" to originLng))),
                    "destination" to mapOf("address" to destinationText),
                    "travelMode" to "DRIVE"
                )
            )
            val first = response.routes.firstOrNull()
            val distance = first?.distanceMeters ?: 0
            val duration = first?.duration?.removeSuffix("s")?.toIntOrNull() ?: 0
            destinationDao.insert(DestinationEntity(label = destinationText, address = destinationText, remainingDistanceMeters = distance, remainingDurationSeconds = duration))
            RouteSummary(distanceMeters = distance, durationSeconds = duration, destinationLabel = destinationText)
        } catch (_: Exception) {
            RouteSummary(destinationLabel = destinationText)
        }
    }

    suspend fun currentWeekDriveSeconds(nowEpoch: Long = now()): Long {
        val from = startOfWeekEpoch(nowEpoch)
        return sessionDao.sumDriveSince(from)
    }

    suspend fun currentBiWeekDriveSeconds(nowEpoch: Long = now()): Long {
        val from = nowEpoch - 14 * 24 * 3600
        return sessionDao.sumDriveSince(from)
    }

    suspend fun currentWeekDistanceMeters(nowEpoch: Long = now()): Long {
        val from = startOfWeekEpoch(nowEpoch)
        return sessionDao.sumDistanceSince(from)
    }

    suspend fun extensionsUsedThisWeek(nowEpoch: Long = now()): Int {
        val from = startOfWeekEpoch(nowEpoch)
        return sessionDao.countSessionsOverDriveSince(from).coerceAtMost(2)
    }

    data class Totals(val driveSeconds: Long, val breakSeconds: Long, val workSeconds: Long)

    private fun calculateTotals(events: List<EventLogEntity>): Totals {
        var drive = 0L
        var breakTime = 0L
        var work = 0L
        events.sortedBy { it.timestampEpochSeconds }.zipWithNext().forEach { (a, b) ->
            val delta = (b.timestampEpochSeconds - a.timestampEpochSeconds).coerceAtLeast(0)
            when (a.eventType) {
                EventType.DRIVE.name -> drive += delta
                EventType.BREAK.name -> breakTime += delta
                EventType.WORK.name -> work += delta
            }
        }
        return Totals(drive, breakTime, work)
    }

    private fun calculateDistanceMeters(points: List<GpsTrackPointEntity>): Long {
        if (points.size < 2) return 0
        val result = FloatArray(1)
        var total = 0.0
        points.zipWithNext().forEach { (a, b) ->
            Location.distanceBetween(a.lat, a.lng, b.lat, b.lng, result)
            total += result[0]
        }
        return total.toLong()
    }

    private fun startOfWeekEpoch(epochSeconds: Long): Long {
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochSecond(epochSeconds).atZone(zone)
        val monday = zdt.toLocalDate().minusDays((zdt.dayOfWeek.value - 1).toLong()).atStartOfDay(zone)
        return monday.toEpochSecond()
    }

    private fun now(): Long = Instant.now().epochSecond
}
