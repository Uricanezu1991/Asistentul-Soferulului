package com.example.driverassistant.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class EventType { START_DAY, DRIVE, BREAK, WORK, STOP_DAY }

data class RouteSummary(
    val distanceMeters: Int = 0,
    val durationSeconds: Int = 0,
    val destinationLabel: String = ""
)

data class DriverStatus(
    val driveTodaySeconds: Long = 0,
    val continuousDriveSeconds: Long = 0,
    val breakTodaySeconds: Long = 0,
    val workTodaySeconds: Long = 0,
    val remainingUntilBreakSeconds: Long = 4.5.hours,
    val remainingDailyDriveSeconds: Long = 9.hours,
    val weeklyDriveSeconds: Long = 0,
    val biWeeklyDriveSeconds: Long = 0,
    val remainingWeeklyDriveSeconds: Long = 56.hours,
    val remainingBiWeeklyDriveSeconds: Long = 90.hours,
    val extensionsUsedThisWeek: Int = 0,
    val breaksSummary: String = "Nicio pauza relevanta inca",
    val routeVerdict: String = "Seteaza destinatia"
)

data class SessionHistoryItem(
    val id: Long,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long?,
    val driveSeconds: Long,
    val breakSeconds: Long,
    val workSeconds: Long,
    val distanceMeters: Long,
    val status: String
)

val Int.hours: Long get() = this * 3600L

fun Long.toHourMinute(): String {
    val d = Duration.ofSeconds(this.coerceAtLeast(0))
    val h = d.toHours()
    val m = d.minusHours(h).toMinutes()
    return "%02dh %02dm".format(h, m)
}

fun Long.toKmString(): String = "%.1f km".format(this / 1000.0)

fun Long.toDateTimeString(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    return formatter.format(Instant.ofEpochSecond(this).atZone(zoneId))
}
