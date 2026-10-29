package com.example.smartgpstracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartgpstracker.data.repository.LocationManagerHelper
import com.example.smartgpstracker.ui.navigation.Screen
import com.example.smartgpstracker.ui.screens.DashboardScreen
import com.example.smartgpstracker.ui.screens.HistoryScreen
import com.example.smartgpstracker.ui.screens.MapScreen
import com.example.smartgpstracker.ui.screens.SettingsScreen
import com.example.smartgpstracker.ui.theme.SmartGPSTrackerTheme
import com.example.smartgpstracker.ui.viewmodel.HistoryViewModel
import com.example.smartgpstracker.ui.viewmodel.SettingsViewModel
import com.example.smartgpstracker.ui.viewmodel.TrackerViewModel
import com.example.smartgpstracker.utils.PermissionUtils

class MainActivity : ComponentActivity() {

    private val trackerViewModel: TrackerViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var locationHelper: LocationManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationHelper = LocationManagerHelper(this)

        setContent {
            SmartGPSTrackerTheme {
                MainContent(
                    trackerViewModel = trackerViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel,
                    locationHelper = locationHelper
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtils.hasLocationPermission(this)) {
            locationHelper.startListening()
        }
    }

    override fun onPause() {
        super.onPause()
        // If not actively recording in background service, release foreground listener
        if (trackerViewModel.tripState.value == com.example.smartgpstracker.data.model.TripState.IDLE) {
            locationHelper.stopListening()
        }
    }
}

@Composable
private fun MainContent(
    trackerViewModel: TrackerViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel,
    locationHelper: LocationManagerHelper
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(PermissionUtils.hasLocationPermission(context))
    }
    var isGpsEnabled by remember {
        mutableStateOf(PermissionUtils.isGpsEnabled(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            locationHelper.startListening()
        }
        isGpsEnabled = PermissionUtils.isGpsEnabled(context)
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val screens = listOf(
        Screen.Dashboard,
        Screen.Map,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = trackerViewModel,
                    hasLocationPermission = hasLocationPermission,
                    isGpsEnabled = isGpsEnabled,
                    onRequestPermission = {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                )
            }
            composable(Screen.Map.route) {
                MapScreen(
                    viewModel = trackerViewModel
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    historyViewModel = historyViewModel,
                    trackerViewModel = trackerViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
