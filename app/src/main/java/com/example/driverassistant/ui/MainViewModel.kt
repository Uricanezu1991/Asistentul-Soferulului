package com.example.driverassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.driverassistant.data.local.DriverRepository
import com.example.driverassistant.domain.model.DriverStatus
import com.example.driverassistant.domain.model.EventType
import com.example.driverassistant.domain.model.RouteSummary
import com.example.driverassistant.domain.rules.DrivingRulesEngine
import com.example.driverassistant.export.CsvExporter
import com.example.driverassistant.location.LocationClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DriverRepository,
    private val locationClient: LocationClient
) : ViewModel() {

    private val currentRoute = MutableStateFlow<RouteSummary?>(null)
    private val currentSessionId = MutableStateFlow<Long?>(null)
    private val statusOverride = MutableStateFlow(DriverStatus())
    private val weeklyDriveSeconds = MutableStateFlow(0L)
    private val biWeeklyDriveSeconds = MutableStateFlow(0L)
    private val weeklyDistanceMeters = MutableStateFlow(0L)
    private val extensionsUsedThisWeek = MutableStateFlow(0)
    private val exportPreview = MutableStateFlow("")

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeLatestSession(),
        repository.observeLatestEvents(),
        repository.observeRecentSessions(),
        locationClient.locationState,
        currentRoute,
        weeklyDistanceMeters,
        exportPreview,
        statusOverride,
        weeklyDriveSeconds,
        biWeeklyDriveSeconds,
        extensionsUsedThisWeek
    ) { session, events, history, location, route, weeklyDistance, export, currentStatus, weeklyDrive, biWeeklyDrive, extensions ->
        val calculated = DrivingRulesEngine.calculate(
            session = session,
            events = events,
            weeklyDriveSeconds = weeklyDrive,
            biWeeklyDriveSeconds = biWeeklyDrive,
            extensionsUsedThisWeek = extensions
        )
        MainUiState(
            session = session,
            events = events,
            history = history,
            location = location,
            route = route,
            weeklyDistanceMeters = weeklyDistance,
            exportPreview = export,
            status = calculated.copy(routeVerdict = currentStatus.routeVerdict.ifBlank { calculated.routeVerdict })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            locationClient.start()
        }
        refreshSummaries()
    }

    fun startDay() {
        viewModelScope.launch {
            val loc = locationClient.locationState.value
            val sessionId = repository.startDay(loc?.latitude, loc?.longitude)
            currentSessionId.value = sessionId
            refreshSummaries()
        }
    }

    fun logEvent(type: EventType) {
        viewModelScope.launch {
            val loc = locationClient.locationState.value
            val sessionId = currentSessionId.value ?: repository.getOrCreateOpenSession(loc?.latitude, loc?.longitude)
            currentSessionId.value = sessionId
            repository.logEvent(sessionId, type, loc?.latitude, loc?.longitude)
            refreshSummaries()
        }
    }

    fun stopDay() {
        viewModelScope.launch {
            val loc = locationClient.locationState.value
            val sessionId = currentSessionId.value ?: return@launch
            repository.stopDay(sessionId, loc?.latitude, loc?.longitude)
            refreshSummaries()
        }
    }

    fun setDestination(address: String) {
        viewModelScope.launch {
            val loc = locationClient.locationState.value ?: return@launch
            val route = repository.fetchRouteSummary(
                originLat = loc.latitude,
                originLng = loc.longitude,
                destinationText = address
            )
            currentRoute.value = route
            val current = uiState.value.status
            statusOverride.value = current.copy(routeVerdict = DrivingRulesEngine.evaluateRoute(current, route))
        }
    }

    fun prepareExport() {
        exportPreview.value = CsvExporter.sessionsToCsv(uiState.value.history)
    }

    private fun refreshSummaries() {
        viewModelScope.launch {
            weeklyDriveSeconds.value = repository.currentWeekDriveSeconds()
            biWeeklyDriveSeconds.value = repository.currentBiWeekDriveSeconds()
            weeklyDistanceMeters.value = repository.currentWeekDistanceMeters()
            extensionsUsedThisWeek.value = 0
        }
    }
}

class MainViewModelFactory(
    private val repository: DriverRepository,
    private val locationClient: LocationClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, locationClient) as T
    }
}
