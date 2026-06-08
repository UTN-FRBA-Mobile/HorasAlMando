package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.horas_al_mando.ham_android.ui.CLEAN_MAP_STYLE_JSON
import com.horas_al_mando.ham_android.model.FlightPoint
import com.horas_al_mando.ham_android.ui.components.StatsGrid
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.*

private val START_LATLNG          = LatLng(-34.6037, -58.3816)
private const val INITIAL_HEADING = 45.0
private const val CRUISE_ALT_M    = 1500.0
private const val CRUISE_SPEED_KT = 105.0
private const val CLIMB_DURATION  = 240.0

@Composable
fun FlightScreen() {
    val scope          = rememberCoroutineScope()
    val flightPoints   = remember { mutableStateListOf<FlightPoint>() }
    var isFlying       by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showDialog     by remember { mutableStateOf(false) }
    var isUploading    by remember { mutableStateOf(false) }
    var uploadDone     by remember { mutableStateOf(false) }

    LaunchedEffect(isFlying) {
        if (isFlying) {
            while (isFlying) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    LaunchedEffect(isFlying) {
        if (isFlying) {
            while (isFlying) {
                val i    = flightPoints.size
                val t    = i * 2.0
                val prev = flightPoints.lastOrNull()

                val (altitude, vs) = if (t < CLIMB_DURATION) {
                    val alt  = 300.0 + (CRUISE_ALT_M - 300.0) * (t / CLIMB_DURATION)
                    val rate = (CRUISE_ALT_M - 300.0) / (CLIMB_DURATION / 60.0)
                    Pair(alt, rate)
                } else {
                    Pair(CRUISE_ALT_M + sin(t * 0.05) * 8.0, sin(t * 0.08) * 5.0)
                }

                val speed = if (t < CLIMB_DURATION)
                    CRUISE_SPEED_KT * (0.85 + 0.15 * t / CLIMB_DURATION)
                else
                    CRUISE_SPEED_KT + sin(t * 0.06) * 3.0

                val prevHdg = prev?.heading ?: INITIAL_HEADING
                val newHdg  = when (i) {
                    80   -> (prevHdg + 15.0) % 360.0
                    else -> (prevHdg + (Math.random() - 0.5) * 0.6 + 360.0) % 360.0
                }

                val speedMs = speed * 0.5144
                val distM   = speedMs * 2.0
                val hdgRad  = Math.toRadians(newHdg)
                val prevLat = prev?.lat ?: START_LATLNG.latitude
                val prevLng = prev?.lng ?: START_LATLNG.longitude
                val newLat  = prevLat + (distM * cos(hdgRad)) / 111320.0
                val newLng  = prevLng + (distM * sin(hdgRad)) / (111320.0 * cos(Math.toRadians(prevLat)))
                val pressure = 1013.25 * (1.0 - altitude / 44330.0).pow(5.2561)

                flightPoints.add(FlightPoint(
                    lat           = newLat,
                    lng           = newLng,
                    timestamp     = Instant.now().toString(),
                    altitude      = altitude,
                    speed         = speed,
                    heading       = newHdg,
                    verticalSpeed = vs,
                    pressure      = pressure,
                ))
                delay(2000)
            }
        }
    }

    val currentPoint = flightPoints.lastOrNull()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(START_LATLNG, 13f)
    }

    LaunchedEffect(currentPoint) {
        if (currentPoint != null && isFlying) {
            cameraPositionState.position =
                CameraPosition.fromLatLngZoom(LatLng(currentPoint.lat, currentPoint.lng), 13f)
        }
    }

    val mapUiSettings  = remember { MapUiSettings(zoomControlsEnabled = false) }
    val mapProperties  = remember {
        MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions     = MapStyleOptions(CLEAN_MAP_STYLE_JSON),
        )
    }
    val polylinePoints = remember(flightPoints.size) { flightPoints.map { LatLng(it.lat, it.lng) } }
    val markerState    = remember { MarkerState(position = START_LATLNG) }
    LaunchedEffect(currentPoint) {
        currentPoint?.let { markerState.position = LatLng(it.lat, it.lng) }
    }

    if (showDialog) {
        val durationMin = elapsedSeconds / 60
        AlertDialog(
            onDismissRequest = {},
            shape            = RoundedCornerShape(20.dp),
            title            = { Text("Vuelo finalizado") },
            text             = {
                when {
                    isUploading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Subiendo datos del vuelo…")
                    }
                    uploadDone  -> Text("¡Vuelo sincronizado correctamente!")
                    else        -> Text("Vuelo de $durationMin minutos, ${flightPoints.size} puntos registrados.")
                }
            },
            confirmButton = {
                if (!isUploading) {
                    TextButton(onClick = {
                        showDialog     = false
                        uploadDone     = false
                        flightPoints.clear()
                        elapsedSeconds = 0
                    }) { Text("Cerrar") }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier            = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings          = mapUiSettings,
            properties          = mapProperties,
        ) {
            if (polylinePoints.size > 1) {
                Polyline(
                    points = polylinePoints,
                    color  = Primary,
                    width  = 5f,
                )
            }
            if (currentPoint != null) {
                Marker(
                    state = markerState,
                    title = "Posición actual",
                )
            }
        }

        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Surface),
            border    = BorderStroke(1.dp, Outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatsGrid(
                    altitude = currentPoint?.altitude?.let { "%.0f".format(it) } ?: "--",
                    speed    = currentPoint?.speed?.let    { "%.0f".format(it) } ?: "--",
                    heading  = currentPoint?.heading?.let  { "%.0f".format(it) } ?: "--",
                    elapsed  = formatElapsed(elapsedSeconds),
                )

                if (!isFlying) {
                    Button(
                        onClick        = { isFlying = true },
                        modifier       = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text("Iniciar Vuelo", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            isFlying   = false
                            showDialog = true
                            scope.launch {
                                isUploading = true
                                delay(2000)
                                isUploading = false
                                uploadDone  = true
                            }
                        },
                        modifier       = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        colors         = ButtonDefaults.buttonColors(containerColor = Destructive),
                    ) {
                        Text("Finalizar Vuelo", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
