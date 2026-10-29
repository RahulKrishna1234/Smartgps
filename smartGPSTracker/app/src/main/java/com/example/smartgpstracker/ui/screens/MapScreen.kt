package com.example.smartgpstracker.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartgpstracker.data.model.TripState
import com.example.smartgpstracker.ui.theme.AccentAmber
import com.example.smartgpstracker.ui.theme.AccentRed
import com.example.smartgpstracker.ui.viewmodel.TrackerViewModel
import com.example.smartgpstracker.utils.Formatters
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val tripState by viewModel.tripState.collectAsState()
    val distanceMeters by viewModel.distanceMeters.collectAsState()
    val elapsedTimeSeconds by viewModel.elapsedTimeSeconds.collectAsState()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsState()
    val isImperial by viewModel.isImperial.collectAsState()
    val isGpsFixAcquired by viewModel.isGpsFixAcquired.collectAsState()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // OpenStreetMap View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)

                    // Initial default center (world center until GPS fix)
                    val initialPoint = GeoPoint(0.0, 0.0)
                    controller.setCenter(initialPoint)

                    // Route Polyline
                    val line = Polyline().apply {
                        outlinePaint.color = AndroidColor.parseColor("#006C50")
                        outlinePaint.strokeWidth = 14f
                        outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                        outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                    overlayManager.add(line)
                    routePolyline = line

                    // Current Location Marker
                    val marker = Marker(this).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Current Position"
                    }
                    overlayManager.add(marker)
                    currentMarker = marker

                    mapViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lifecycle handling for MapView
        DisposableEffect(Unit) {
            onDispose {
                mapViewRef?.onDetach()
            }
        }

        // Reactively update marker and center when location changes
        LaunchedEffect(currentLocation) {
            val loc = currentLocation ?: return@LaunchedEffect
            val map = mapViewRef ?: return@LaunchedEffect
            val marker = currentMarker ?: return@LaunchedEffect

            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            marker.position = geoPoint

            // Center if trip is recording or first fix
            if (tripState == TripState.RECORDING || map.mapCenter.latitude == 0.0) {
                map.controller.animateTo(geoPoint)
            }
            map.invalidate()
        }

        // Reactively update polyline points
        LaunchedEffect(routePoints) {
            val map = mapViewRef ?: return@LaunchedEffect
            val line = routePolyline ?: return@LaunchedEffect

            val geoPoints = routePoints.map { GeoPoint(it.latitude, it.longitude) }
            line.setPoints(geoPoints)
            map.invalidate()
        }

        // Floating HUD Overlay at the top
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (tripState) {
                                    TripState.RECORDING -> MaterialTheme.colorScheme.primary
                                    TripState.PAUSED -> AccentAmber
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (tripState) {
                                TripState.RECORDING -> "RECORDING"
                                TripState.PAUSED -> "PAUSED"
                                else -> if (isGpsFixAcquired) "GPS READY" else "SEARCHING GPS"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (tripState) {
                                TripState.RECORDING -> MaterialTheme.colorScheme.onPrimary
                                TripState.PAUSED -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Accuracy Indicator
                    Text(
                        text = currentLocation?.let {
                            Formatters.formatAccuracy(it.accuracy, isImperial)
                        } ?: "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SPEED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isGpsFixAcquired && currentLocation != null) {
                                Formatters.formatSpeed(currentSpeedKmh, isImperial)
                            } else {
                                "--"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = Formatters.formatDistance(distanceMeters, isImperial),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = Formatters.formatDuration(elapsedTimeSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Map Controls on the bottom right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zoom In Button
            SmallFloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomIn()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out Button
            SmallFloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.zoomOut()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            // Center on Current Location
            FloatingActionButton(
                onClick = {
                    currentLocation?.let { loc ->
                        mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }
    }
}
