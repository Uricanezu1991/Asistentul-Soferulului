package com.example.driverassistant.domain.rules

import com.example.driverassistant.data.local.DailySessionEntity
import com.example.driverassistant.data.local.EventLogEntity
import com.example.driverassistant.domain.model.DriverStatus
import com.example.driverassistant.domain.model.EventType
import com.example.driverassistant.domain.model.RouteSummary
import com.example.driverassistant.domain.model.hours

object DrivingRulesEngine {
    private const val MAX_CONTINUOUS_DRIVE = 4.5 * 3600
    private const val MAX_DAILY_DRIVE = 9 * 3600
    private const val MAX_WEEKLY_DRIVE = 56 * 3600
    private const val MAX_BIWEEKLY_DRIVE = 90 * 3600

    fun calculate(
        session: DailySessionEntity?,
        events: List<EventLogEntity>,
        weeklyDriveSeconds: Long = 0,
        biWeeklyDriveSeconds: Long = 0,
        extensionsUsedThisWeek: Int = 0
    ): DriverStatus {
        if (session == null || events.isEmpty()) {
            return DriverStatus(
                weeklyDriveSeconds = weeklyDriveSeconds,
                biWeeklyDriveSeconds = biWeeklyDriveSeconds,
                remainingWeeklyDriveSeconds = (MAX_WEEKLY_DRIVE - weeklyDriveSeconds).coerceAtLeast(0),
                remainingBiWeeklyDriveSeconds = (MAX_BIWEEKLY_DRIVE - biWeeklyDriveSeconds).coerceAtLeast(0),
                extensionsUsedThisWeek = extensionsUsedThisWeek
            )
        }

        var drive = 0L
        var continuousDrive = 0L
        var breakTime = 0L
        var work = 0L
        var firstBreakChunk = 0L
        var secondBreakChunk = 0L

        val ordered = events.sortedBy { it.timestampEpochSeconds }
        ordered.zipWithNext().forEach { (a, b) ->
            val delta = (b.timestampEpochSeconds - a.timestampEpochSeconds).coerceAtLeast(0)
            when (a.eventType) {
                EventType.DRIVE.name -> {
                    drive += delta
                    continuousDrive += delta
                }
                EventType.BREAK.name -> {
                    breakTime += delta
                    if (firstBreakChunk < 15 * 60 && delta >= 15 * 60L) {
                        firstBreakChunk = delta
                    } else if (firstBreakChunk >= 15 * 60 && secondBreakChunk < 30 * 60 && delta >= 30 * 60L) {
                        secondBreakChunk = delta
                    }
                    if (delta >= 45 * 60L || (firstBreakChunk >= 15 * 60 && secondBreakChunk >= 30 * 60)) {
                        continuousDrive = 0L
                    }
                }
                EventType.WORK.name -> work += delta
            }
        }

        val dailyLimit = if (extensionsUsedThisWeek < 2) 10.hours else MAX_DAILY_DRIVE.toLong()
        val remainingBreak = (MAX_CONTINUOUS_DRIVE - continuousDrive).toLong().coerceAtLeast(0)
        val remainingDaily = (dailyLimit - drive).coerceAtLeast(0)
        val breaksSummary = when {
            breakTime >= 45 * 60L -> "Pauza de 45 min este acoperita"
            firstBreakChunk >= 15 * 60 && secondBreakChunk >= 30 * 60 -> "Pauza fractionata 15 + 30 este acoperita"
            firstBreakChunk >= 15 * 60 -> "Ai prima parte de 15 min, mai lipsesc 30 min"
            else -> "Pauza de 45 min nu este completa"
        }

        return DriverStatus(
            driveTodaySeconds = drive,
            continuousDriveSeconds = continuousDrive,
            breakTodaySeconds = breakTime,
            workTodaySeconds = work,
            remainingUntilBreakSeconds = remainingBreak,
            remainingDailyDriveSeconds = remainingDaily,
            weeklyDriveSeconds = weeklyDriveSeconds,
            biWeeklyDriveSeconds = biWeeklyDriveSeconds,
            remainingWeeklyDriveSeconds = (MAX_WEEKLY_DRIVE - weeklyDriveSeconds).coerceAtLeast(0),
            remainingBiWeeklyDriveSeconds = (MAX_BIWEEKLY_DRIVE - biWeeklyDriveSeconds).coerceAtLeast(0),
            extensionsUsedThisWeek = extensionsUsedThisWeek,
            breaksSummary = breaksSummary,
            routeVerdict = "Seteaza destinatia"
        )
    }

    fun evaluateRoute(status: DriverStatus, route: RouteSummary): String {
        return when {
            route.durationSeconds <= status.remainingUntilBreakSeconds &&
                route.durationSeconds <= status.remainingDailyDriveSeconds &&
                route.durationSeconds <= status.remainingWeeklyDriveSeconds &&
                route.durationSeconds <= status.remainingBiWeeklyDriveSeconds -> "Ajungi legal fara pauza"
            route.durationSeconds > status.remainingDailyDriveSeconds -> "Nu ajungi legal azi fara oprire"
            route.durationSeconds > status.remainingUntilBreakSeconds -> "Ai nevoie de pauza inainte de destinatie"
            route.durationSeconds > status.remainingWeeklyDriveSeconds -> "Depasesti limita saptamanala"
            route.durationSeconds > status.remainingBiWeeklyDriveSeconds -> "Depasesti limita pe 2 saptamani"
            else -> "Verifica manual traseul"
        }
    }
}
