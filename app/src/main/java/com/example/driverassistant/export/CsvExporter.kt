package com.example.driverassistant.export

import com.example.driverassistant.domain.model.SessionHistoryItem

object CsvExporter {
    fun sessionsToCsv(items: List<SessionHistoryItem>): String {
        val header = "session_id,start_epoch,end_epoch,drive_seconds,break_seconds,work_seconds,distance_meters,status"
        val rows = items.map {
            listOf(
                it.id,
                it.startEpochSeconds,
                it.endEpochSeconds ?: "",
                it.driveSeconds,
                it.breakSeconds,
                it.workSeconds,
                it.distanceMeters,
                it.status
            ).joinToString(",")
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}
