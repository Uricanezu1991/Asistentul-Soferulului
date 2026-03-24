package com.example.driverassistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.driverassistant.data.local.AppDatabase
import com.example.driverassistant.data.local.DriverRepository
import com.example.driverassistant.location.LocationClient
import com.example.driverassistant.ui.DriverAssistantApp
import com.example.driverassistant.ui.MainViewModel
import com.example.driverassistant.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val permissions = buildList {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = {}
            )
            val requested = remember { mutableStateOf(false) }

            val database = remember { AppDatabase.getInstance(context) }
            val repository = remember {
                DriverRepository(
                    eventDao = database.eventDao(),
                    sessionDao = database.dailySessionDao(),
                    gpsDao = database.gpsTrackDao(),
                    destinationDao = database.destinationDao(),
                    routeApi = RetrofitProvider.routeApi
                )
            }
            val locationClient = remember { LocationClient(context) }
            val factory = remember { MainViewModelFactory(repository, locationClient) }
            val vm: MainViewModel = viewModel(factory = factory)

            LaunchedEffect(Unit) {
                if (!requested.value) {
                    requested.value = true
                    launcher.launch(permissions.toTypedArray())
                }
            }

            DriverAssistantApp(vm)
        }
    }
}
