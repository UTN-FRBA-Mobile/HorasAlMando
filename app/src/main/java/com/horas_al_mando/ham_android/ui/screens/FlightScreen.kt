package com.horas_al_mando.ham_android.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.ui.CLEAN_MAP_STYLE_JSON
import com.google.gson.Gson
import com.horas_al_mando.ham_android.db.AppDatabase
import com.horas_al_mando.ham_android.db.InProgressFlightPointEntity
import com.horas_al_mando.ham_android.db.PendingFlightEntity
import com.horas_al_mando.ham_android.hardware.CompassHelper
import com.horas_al_mando.ham_android.model.FlightPoint
import com.horas_al_mando.ham_android.model.FlightSyncPayload
import com.horas_al_mando.ham_android.network.ApiClient
import com.horas_al_mando.ham_android.network.FlightApiService
import com.horas_al_mando.ham_android.network.SocialRadarRepository
import com.horas_al_mando.ham_android.service.ActiveCircuitRepository
import com.horas_al_mando.ham_android.service.FlightRepository
import com.horas_al_mando.ham_android.service.FlightTrackingService
import com.horas_al_mando.ham_android.service.RunUploadState
import com.horas_al_mando.ham_android.ui.PlaneMarker
import com.horas_al_mando.ham_android.ui.components.StatsGrid
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant

private val START_LATLNG = LatLng(-34.6037, -58.3816)
private val gson = Gson()

@Composable
fun FlightScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val flightPoints by FlightRepository.currentFlightPath.collectAsState()
    val isTracking by FlightRepository.isTracking.collectAsState()
    val elapsedSeconds by FlightRepository.elapsedSeconds.collectAsState(0L)
    val activePilots by SocialRadarRepository.activePilots.collectAsState()

    val activeCircuit by ActiveCircuitRepository.activeCircuit.collectAsState()
    val runInProgress by ActiveCircuitRepository.runInProgress.collectAsState()
    val nextWaypointIndex by ActiveCircuitRepository.nextWaypointIndex.collectAsState()
    val bearingToNext by ActiveCircuitRepository.bearingToNext.collectAsState()
    val uploadState by ActiveCircuitRepository.uploadState.collectAsState()

    var azimuth by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val compass = CompassHelper(context) { azimuth = it }
        compass.start()
        onDispose { compass.stop() }
    }

    var showDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadDone by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var pendingPayload by remember { mutableStateOf<FlightSyncPayload?>(null) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var showInterruptedDialog by remember { mutableStateOf(false) }
    var interruptedPoints by remember { mutableStateOf<List<InProgressFlightPointEntity>>(emptyList()) }

    val serverErrorMsg = stringResource(R.string.flight_server_error)
    val connectionErrorMsg = stringResource(R.string.flight_connection_error)
    val pilotSnippetFormat = stringResource(R.string.flight_pilot_snippet)
    val myPositionLabel = stringResource(R.string.flight_my_position)

    LaunchedEffect(Unit) {
        val pendingDao = AppDatabase.getInstance(context).pendingFlightDao()
        val inProgressDao = AppDatabase.getInstance(context).inProgressFlightDao()

        val pending = pendingDao.get()
        if (pending != null) {
            pendingPayload = gson.fromJson(pending.payloadJson, FlightSyncPayload::class.java)
            showPendingDialog = true
            return@LaunchedEffect
        }

        if (!isTracking) {
            val points = inProgressDao.getAll()
            if (points.isNotEmpty()) {
                interruptedPoints = points
                showInterruptedDialog = true
            }
        }
    }

    val doFinish: suspend (FlightSyncPayload) -> Unit = { payload ->
        val pendingDao = AppDatabase.getInstance(context).pendingFlightDao()
        val inProgressDao = AppDatabase.getInstance(context).inProgressFlightDao()
        pendingDao.save(PendingFlightEntity(payloadJson = gson.toJson(payload)))
        isUploading = true
        try {
            val response = FlightApiService.api.finishFlight(payload)
            if (response.isSuccessful) {
                pendingDao.delete()
                inProgressDao.deleteAll()
                FlightRepository.clearFlight()
                ApiClient.getSessionManager().clearClientFlightId()
                uploadDone = true
            } else {
                syncError = serverErrorMsg.format(response.code())
            }
        } catch (e: Exception) {
            syncError = connectionErrorMsg.format(e.localizedMessage ?: "desconocido")
        } finally {
            isUploading = false
        }
    }

    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            val intent = Intent(context, FlightTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    val currentPoint = flightPoints.lastOrNull()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(START_LATLNG, 13f)
    }

    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = true) }
    val density = LocalDensity.current
    var statsCardHeightPx by remember { mutableIntStateOf(0) }
    val mapProperties = remember(isTracking) {
        MapProperties(
            isMyLocationEnabled = isTracking,
            mapStyleOptions = MapStyleOptions(CLEAN_MAP_STYLE_JSON),
        )
    }
    val polylinePoints = remember(flightPoints.size) { flightPoints.map { LatLng(it.lat, it.lng) } }
    val ownMarkerState = remember { MarkerState(position = START_LATLNG) }

    val circuitPoints = remember(activeCircuit) {
        activeCircuit?.waypoints?.sortedBy { it.order }?.map { LatLng(it.lat, it.lng) } ?: emptyList()
    }

    var hasCenteredInitially by remember { mutableStateOf(false) }

    LaunchedEffect(isTracking) {
        if (!isTracking) hasCenteredInitially = false
    }

    LaunchedEffect(currentPoint) {
        currentPoint?.let {
            if (it.lat != 0.0 && it.lng != 0.0) {
                val pos = LatLng(it.lat, it.lng)
                ownMarkerState.position = pos
                if (isTracking) {
                    if (!hasCenteredInitially) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 15f)
                        hasCenteredInitially = true
                    } else {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLng(pos))
                    }
                }
            }
        }
    }

    if (showInterruptedDialog && interruptedPoints.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.flight_interrupted_title)) },
            text = { Text(stringResource(R.string.flight_interrupted_message, interruptedPoints.size)) },
            confirmButton = {
                TextButton(onClick = {
                    showInterruptedDialog = false
                    showDialog = true
                    val points = interruptedPoints.map {
                        FlightPoint(
                            lat = it.lat, lng = it.lng, timestamp = it.timestamp,
                            altitude = it.altitude, speed = it.speed, heading = it.heading,
                            verticalSpeed = it.verticalSpeed, pressure = it.pressure
                        )
                    }
                    val recoveredId = ApiClient.getSessionManager().getClientFlightId()
                        ?: java.util.UUID.randomUUID().toString()
                    val payload = FlightSyncPayload(
                        clientFlightId = recoveredId,
                        startTime = points.first().timestamp,
                        endTime = Instant.now().toString(),
                        path = points
                    )
                    scope.launch { doFinish(payload) }
                }) { Text(stringResource(R.string.flight_pending_upload)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInterruptedDialog = false
                    interruptedPoints = emptyList()
                    FlightRepository.clearFlight()
                    ApiClient.getSessionManager().clearClientFlightId()
                    scope.launch {
                        AppDatabase.getInstance(context).inProgressFlightDao().deleteAll()
                    }
                }) { Text(stringResource(R.string.flight_pending_discard)) }
            }
        )
    }

    if (showPendingDialog && pendingPayload != null) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.flight_pending_title)) },
            text = { Text(stringResource(R.string.flight_pending_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPendingDialog = false
                    showDialog = true
                    val payload = pendingPayload!!
                    scope.launch { doFinish(payload) }
                }) { Text(stringResource(R.string.flight_pending_upload)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPendingDialog = false
                    pendingPayload = null
                    FlightRepository.clearFlight()
                    ApiClient.getSessionManager().clearClientFlightId()
                    scope.launch { AppDatabase.getInstance(context).pendingFlightDao().delete() }
                }) { Text(stringResource(R.string.flight_pending_discard)) }
            }
        )
    }

    if (showDialog) {
        val durationMin = elapsedSeconds / 60
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    when {
                        uploadDone -> stringResource(R.string.flight_sync_success_title)
                        syncError != null -> stringResource(R.string.flight_sync_error_title)
                        else -> stringResource(R.string.flight_finished_title)
                    }
                )
            },
            text = {
                when {
                    isUploading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.flight_uploading_message))
                    }
                    uploadDone -> Text(stringResource(R.string.flight_sync_success_message))
                    syncError != null -> Text(syncError!!)
                    else -> Text(stringResource(R.string.flight_sync_summary, durationMin.toInt(), flightPoints.size))
                }
            },
            confirmButton = {
                if (!isUploading) {
                    Row {
                        if (syncError != null) {
                            TextButton(onClick = {
                                syncError = null
                                scope.launch {
                                    val dao = AppDatabase.getInstance(context).pendingFlightDao()
                                    val pending = dao.get()
                                    if (pending != null) {
                                        val payload = gson.fromJson(pending.payloadJson, FlightSyncPayload::class.java)
                                        doFinish(payload)
                                    }
                                }
                            }) { Text(stringResource(R.string.flight_retry_button)) }
                        }
                        TextButton(onClick = {
                            showDialog = false
                            uploadDone = false
                            syncError = null
                            FlightRepository.setTracking(false)
                        }) { Text(stringResource(R.string.flight_close_button)) }
                    }
                }
            },
        )
    }

    uploadState?.let { state ->
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.flight_circuit_finished_title)) },
            text = {
                when (state) {
                    is RunUploadState.Uploading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.flight_circuit_finished_uploading))
                    }
                    is RunUploadState.Success -> Text(
                        stringResource(
                            R.string.flight_circuit_finished_success,
                            state.run.rank,
                            formatDuration(state.run.totalDurationSeconds),
                        )
                    )
                    is RunUploadState.Error -> Text(
                        stringResource(R.string.flight_circuit_finished_error, state.message)
                    )
                }
            },
            confirmButton = {
                if (state !is RunUploadState.Uploading) {
                    TextButton(onClick = { ActiveCircuitRepository.acknowledgeResult() }) {
                        Text(stringResource(R.string.flight_circuit_finished_close))
                    }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties,
            contentPadding = PaddingValues(bottom = with(density) { statsCardHeightPx.toDp() }),
        ) {
            if (polylinePoints.size > 1) {
                Polyline(
                    points = polylinePoints,
                    color = Primary,
                    width = 5f,
                )
            }

            if (circuitPoints.isNotEmpty()) {
                if (circuitPoints.size > 1) {
                    Polyline(points = circuitPoints, color = Secondary, width = 5f)
                }
                activeCircuit?.waypoints?.sortedBy { it.order }?.forEach { wp ->
                    Marker(
                        state = MarkerState(position = LatLng(wp.lat, wp.lng)),
                        title = "#${wp.order}",
                    )
                }
            }

            if (currentPoint != null) {
                PlaneMarker(
                    markerState = ownMarkerState,
                    headingDegrees = (currentPoint.heading ?: 0.0).toFloat(),
                    tint = Primary,
                    title = myPositionLabel,
                )
            }

            activePilots.forEach { pilot ->
                if (pilot.pilotId != FlightRepository.getPilotId()) {
                    val pilotMarkerState = remember(pilot.pilotId) {
                        MarkerState(position = LatLng(pilot.lat, pilot.lng))
                    }
                    LaunchedEffect(pilot.lat, pilot.lng) {
                        pilotMarkerState.position = LatLng(pilot.lat, pilot.lng)
                    }
                    PlaneMarker(
                        markerState = pilotMarkerState,
                        headingDegrees = pilot.heading.toFloat(),
                        tint = GradientBlue,
                        title = pilot.name,
                        snippet = pilotSnippetFormat.format(pilot.altitude, pilot.speed),
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { statsCardHeightPx = it.size.height }
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (activeCircuit != null) {
                    CircuitRunPanel(
                        circuitName = activeCircuit!!.name,
                        timer = formatElapsed(elapsedSeconds),
                        nextWaypointNumber = circuitPoints.indices.contains(nextWaypointIndex).let {
                            if (it && runInProgress) nextWaypointIndex else -1
                        },
                        nextWaypointLat = activeCircuit!!.waypoints.getOrNull(nextWaypointIndex)?.lat,
                        nextWaypointLng = activeCircuit!!.waypoints.getOrNull(nextWaypointIndex)?.lng,
                        arrowRotation = ((bearingToNext ?: 0f) - azimuth),
                        showProgress = runInProgress,
                        onCancel = { ActiveCircuitRepository.disarm() },
                    )
                    HorizontalDivider(color = Outline)
                }

                StatsGrid(
                    altitude = currentPoint?.altitude?.let { "%.0f".format(it) } ?: "--",
                    speed = currentPoint?.speed?.let { "%.0f".format(it) } ?: "--",
                    heading = currentPoint?.heading?.let { "%.0f".format(it) } ?: "--",
                    elapsed = formatElapsed(elapsedSeconds),
                )

                if (!isTracking) {
                    Button(
                        onClick = {
                            val allGranted = permissionsToRequest.all {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (allGranted) {
                                val intent = Intent(context, FlightTrackingService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                launcher.launch(permissionsToRequest.toTypedArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.flight_start_button), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            val payload = FlightRepository.getSyncPayload()
                            context.stopService(Intent(context, FlightTrackingService::class.java))
                            showDialog = true
                            scope.launch { doFinish(payload) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Destructive),
                    ) {
                        Text(stringResource(R.string.flight_end_button), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircuitRunPanel(
    circuitName: String,
    timer: String,
    nextWaypointNumber: Int,
    nextWaypointLat: Double?,
    nextWaypointLng: Double?,
    arrowRotation: Float,
    showProgress: Boolean,
    onCancel: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(circuitName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.flight_circuit_timer, timer))
            if (showProgress && nextWaypointNumber >= 0) {
                Text(stringResource(R.string.flight_circuit_next_waypoint, nextWaypointNumber))
                if (nextWaypointLat != null && nextWaypointLng != null) {
                    Text(
                        stringResource(R.string.flight_circuit_coords, nextWaypointLat, nextWaypointLng),
                        style = MaterialTheme.typography.bodySmall,
                        color = Secondary,
                    )
                }
            }
            TextButton(onClick = onCancel, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.flight_circuit_cancel_run), color = Destructive)
            }
        }
        if (showProgress) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp).rotate(arrowRotation),
            )
        }
    }
}

private fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
