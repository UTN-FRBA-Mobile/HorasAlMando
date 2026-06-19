package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.model.CircuitDetail
import com.horas_al_mando.ham_android.model.CircuitRun
import com.horas_al_mando.ham_android.network.CircuitRepository
import com.horas_al_mando.ham_android.service.ActiveCircuitRepository
import com.horas_al_mando.ham_android.ui.CLEAN_MAP_STYLE_JSON
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CircuitDetailScreen(
    circuitId: Long,
    repository: CircuitRepository,
    onBack: () -> Unit,
    onRecorrer: () -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<CircuitDetail?>(null) }
    var runs by remember { mutableStateOf<List<CircuitRun>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var runsExpanded by remember { mutableStateOf(false) }
    var showRunConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(circuitId) {
        scope.launch {
            repository.getCircuit(circuitId).onSuccess { detail = it }
            repository.getRuns(circuitId).onSuccess { runs = it }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val circuit = detail
    if (circuit == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.circuit_detail_not_found))
        }
        return
    }

    val points = remember(circuit) { circuit.waypoints.sortedBy { it.order }.map { LatLng(it.lat, it.lng) } }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(points.firstOrNull() ?: LatLng(-34.6037, -58.3816), 12f)
    }
    LaunchedEffect(points) {
        if (points.size >= 2) {
            val builder = LatLngBounds.builder()
            points.forEach { builder.include(it) }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
        }
    }

    if (showRunConfirm) {
        AlertDialog(
            onDismissRequest = { showRunConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.circuit_detail_run_confirm_title)) },
            text = { Text(stringResource(R.string.circuit_detail_run_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRunConfirm = false
                    ActiveCircuitRepository.arm(circuit)
                    onRecorrer()
                }) { Text(stringResource(R.string.circuit_common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRunConfirm = false }) {
                    Text(stringResource(R.string.circuit_common_cancel))
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(stringResource(R.string.circuit_detail_delete_title)) },
            text = { Text(stringResource(R.string.circuit_detail_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        repository.deleteCircuit(circuit.id).onSuccess { onDeleted() }
                    }
                }) { Text(stringResource(R.string.circuit_detail_delete_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.circuit_common_cancel))
                }
            },
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    properties = MapProperties(mapStyleOptions = MapStyleOptions(CLEAN_MAP_STYLE_JSON)),
                ) {
                    if (points.size > 1) {
                        Polyline(points = points, color = Primary, width = 6f)
                    }
                    circuit.waypoints.sortedBy { it.order }.forEach { wp ->
                        Marker(
                            state = MarkerState(position = LatLng(wp.lat, wp.lng)),
                            title = "#${wp.order}",
                        )
                    }
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(R.string.circuit_detail_back)) }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(circuit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.circuit_detail_creator, circuit.creatorName), color = Secondary)
                Text(stringResource(R.string.circuit_detail_total_distance, circuit.totalDistanceKm))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { showRunConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                ) { Text(stringResource(R.string.circuit_detail_run_button)) }
                if (circuit.mine) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.circuit_detail_delete_button), color = Destructive) }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Outline),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.circuit_detail_runs_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { runsExpanded = !runsExpanded }) {
                            Icon(
                                if (runsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                            )
                        }
                    }
                    if (runsExpanded) {
                        if (runs.isEmpty()) {
                            Text(
                                stringResource(R.string.circuit_detail_runs_empty),
                                color = Secondary,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            runs.forEach { run ->
                                Text(
                                    stringResource(
                                        R.string.circuit_detail_run_row,
                                        run.rank,
                                        run.pilotName,
                                        formatDuration(run.totalDurationSeconds),
                                    ),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                )
                                HorizontalDivider(color = Outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
