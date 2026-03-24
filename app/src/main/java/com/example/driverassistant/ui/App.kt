package com.example.driverassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.driverassistant.domain.model.EventType
import com.example.driverassistant.domain.model.toDateTimeString
import com.example.driverassistant.domain.model.toHourMinute
import com.example.driverassistant.domain.model.toKmString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverAssistantApp(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val destination = remember { mutableStateOf("") }
    val selectedTab = remember { mutableIntStateOf(0) }

    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(selectedTabIndex = selectedTab.intValue) {
                    Tab(selected = selectedTab.intValue == 0, onClick = { selectedTab.intValue = 0 }, text = { Text("Dashboard") })
                    Tab(selected = selectedTab.intValue == 1, onClick = { selectedTab.intValue = 1 }, text = { Text("Istoric") })
                }
                when (selectedTab.intValue) {
                    0 -> DashboardTab(state, destination.value, onDestinationChange = { destination.value = it }, vm = vm)
                    1 -> HistoryTab(state, onPrepareExport = vm::prepareExport)
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(state: MainUiState, destination: String, onDestinationChange: (String) -> Unit, vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Asistent Transport", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            StatusCard(
                title = "Locatie",
                value = state.location?.let { "${it.latitude}, ${it.longitude}" } ?: "Locatia nu este disponibila"
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatusCard("Condus azi", state.status.driveTodaySeconds.toHourMinute(), Modifier.weight(1f))
                StatusCard("Pana la pauza", state.status.remainingUntilBreakSeconds.toHourMinute(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatusCard("Pana la limita zilnica", state.status.remainingDailyDriveSeconds.toHourMinute(), Modifier.weight(1f))
                StatusCard("Verdict", state.status.routeVerdict, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatusCard("Saptamana", state.status.weeklyDriveSeconds.toHourMinute(), Modifier.weight(1f))
                StatusCard("2 saptamani", state.status.biWeeklyDriveSeconds.toHourMinute(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatusCard("Km saptamana", state.weeklyDistanceMeters.toKmString(), Modifier.weight(1f))
                StatusCard("Pauze", state.status.breaksSummary, Modifier.weight(1f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Destinatie") }
                )
                Button(onClick = { vm.setDestination(destination) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Calculeaza ruta")
                }
            }
        }
        item {
            state.route?.let {
                StatusCard("Ruta", "${it.destinationLabel}\nDistanta: ${it.distanceMeters / 1000.0} km\nDurata: ${it.durationSeconds.toLong().toHourMinute()}")
            }
        }
        item {
            ControlButtons(vm)
        }
        item {
            Text("Evenimente recente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(state.events.take(10)) { event ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(event.eventType, fontWeight = FontWeight.Bold)
                    Text("Timp: ${event.timestampEpochSeconds.toDateTimeString()}")
                    Text("Coordonate: ${event.lat ?: "-"}, ${event.lng ?: "-"}")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HistoryTab(state: MainUiState, onPrepareExport: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Istoric sesiuni", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Button(onClick = onPrepareExport, modifier = Modifier.fillMaxWidth()) {
                Text("Genereaza preview CSV")
            }
        }
        if (state.exportPreview.isNotBlank()) {
            item {
                StatusCard("CSV preview", state.exportPreview)
            }
        }
        items(state.history) { session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sesiune #${session.id}", fontWeight = FontWeight.Bold)
                    Text("Start: ${session.startEpochSeconds.toDateTimeString()}")
                    Text("Final: ${session.endEpochSeconds?.toDateTimeString() ?: "In curs"}")
                    Text("Condus: ${session.driveSeconds.toHourMinute()}")
                    Text("Pauza: ${session.breakSeconds.toHourMinute()}")
                    Text("Lucru: ${session.workSeconds.toHourMinute()}")
                    Text("Distanta: ${session.distanceMeters.toKmString()}")
                    Text("Status: ${session.status}")
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ControlButtons(vm: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.startDay() }, modifier = Modifier.fillMaxWidth()) { Text("Start zi") }
        Button(onClick = { vm.logEvent(EventType.DRIVE) }, modifier = Modifier.fillMaxWidth()) { Text("Condus") }
        Button(onClick = { vm.logEvent(EventType.BREAK) }, modifier = Modifier.fillMaxWidth()) { Text("Pauza") }
        Button(onClick = { vm.logEvent(EventType.WORK) }, modifier = Modifier.fillMaxWidth()) { Text("Lucru") }
        Button(onClick = { vm.stopDay() }, modifier = Modifier.fillMaxWidth()) { Text("Stop zi") }
    }
}

@Composable
private fun StatusCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
