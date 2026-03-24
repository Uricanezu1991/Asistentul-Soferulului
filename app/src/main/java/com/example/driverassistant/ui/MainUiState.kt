package com.example.driverassistant.ui

import com.example.driverassistant.data.local.DailySessionEntity
import com.example.driverassistant.data.local.EventLogEntity
import com.example.driverassistant.domain.model.DriverStatus
import com.example.driverassistant.domain.model.RouteSummary
import com.example.driverassistant.domain.model.SessionHistoryItem
import com.example.driverassistant.location.LocationPoint

data class MainUiState(
    val session: DailySessionEntity? = null,
    val events: List<EventLogEntity> = emptyList(),
    val history: List<SessionHistoryItem> = emptyList(),
    val location: LocationPoint? = null,
    val route: RouteSummary? = null,
    val weeklyDistanceMeters: Long = 0,
    val status: DriverStatus = DriverStatus(),
    val exportPreview: String = ""
)
