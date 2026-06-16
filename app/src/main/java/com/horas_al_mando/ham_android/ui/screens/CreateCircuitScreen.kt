package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.hardware.getLastLocation
import com.horas_al_mando.ham_android.model.Waypoint
import com.horas_al_mando.ham_android.network.CircuitRepository
import com.horas_al_mando.ham_android.ui.CLEAN_MAP_STYLE_JSON
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CreateCircuitScreen(
    repository: CircuitRepository,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val waypoints = remember { mutableStateListOf<Waypoint>() }
    var naming by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-34.6037, -58.3816), 14f)
    }

    LaunchedEffect(Unit) {
        getLastLocation(context) { lat, lng ->
            if (lat != null && lng != null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
            }
        }
    }

    fun addWaypoint() {
        val center = cameraPositionState.position.target
        waypoints.add(Waypoint(order = waypoints.size, lat = center.latitude, lng = center.longitude))
    }

    fun finishWaypoints() {
        addWaypoint()
        naming = true
        if (waypoints.size >= 2) {
            val builder = LatLngBounds.builder()
            waypoints.forEach { builder.include(LatLng(it.lat, it.lng)) }
            scope.launch {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
            }
        }
    }

    val markerPoints = waypoints.map { LatLng(it.lat, it.lng) }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            properties = MapProperties(mapStyleOptions = MapStyleOptions(CLEAN_MAP_STYLE_JSON)),
        ) {
            if (markerPoints.size > 1) {
                Polyline(points = markerPoints, color = Primary, width = 6f)
            }
            waypoints.forEach { wp ->
                Marker(
                    state = MarkerState(position = LatLng(wp.lat, wp.lng)),
                    title = "#${wp.order}",
                )
            }
        }

        if (!naming) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Destructive,
                modifier = Modifier.align(Alignment.Center).size(48.dp),
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) { Text(stringResource(R.string.create_circuit_cancel)) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.create_circuit_count, waypoints.size))

                if (!naming) {
                    Text(stringResource(R.string.create_circuit_hint), color = Secondary, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { addWaypoint() },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.create_circuit_add_waypoint)) }
                        Button(
                            onClick = { finishWaypoints() },
                            modifier = Modifier.weight(1f),
                            enabled = waypoints.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) { Text(stringResource(R.string.create_circuit_final_waypoint)) }
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.create_circuit_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (saveError) {
                        Text(stringResource(R.string.create_circuit_error), color = Destructive)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                naming = false
                                if (waypoints.isNotEmpty()) waypoints.removeAt(waypoints.size - 1)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving,
                        ) { Text(stringResource(R.string.create_circuit_cancel)) }
                        Button(
                            onClick = {
                                isSaving = true
                                saveError = false
                                scope.launch {
                                    repository.createCircuit(name.trim(), waypoints.toList())
                                        .onSuccess { onCreated() }
                                        .onFailure { saveError = true; isSaving = false }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving && name.isNotBlank() && waypoints.size >= 2,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnPrimary)
                            } else {
                                Text(stringResource(R.string.create_circuit_confirm))
                            }
                        }
                    }
                }
            }
        }
    }
}
