package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.hardware.getLastLocation
import com.horas_al_mando.ham_android.model.CircuitSummary
import com.horas_al_mando.ham_android.network.CircuitRepository
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CircuitsScreen(
    repository: CircuitRepository,
    onOpenDetail: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var circuits by remember { mutableStateOf<List<CircuitSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var mineOnly by remember { mutableStateOf(false) }
    var rangeKm by remember { mutableFloatStateOf(200f) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }

    fun reload() {
        isLoading = true
        loadError = false
        scope.launch {
            repository.getCircuits(userLat, userLng, rangeKm.toDouble(), if (mineOnly) true else null)
                .onSuccess { circuits = it }
                .onFailure { loadError = true }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        getLastLocation(context) { lat, lng ->
            userLat = lat
            userLng = lng
            reload()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                containerColor = Primary,
                contentColor = OnPrimary,
            ) { Text(stringResource(R.string.circuits_create_button)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = stringResource(R.string.circuits_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !mineOnly,
                        onClick = { mineOnly = false; reload() },
                        label = { Text(stringResource(R.string.circuits_filter_all)) },
                    )
                    FilterChip(
                        selected = mineOnly,
                        onClick = { mineOnly = true; reload() },
                        label = { Text(stringResource(R.string.circuits_filter_mine)) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.circuits_filter_range, rangeKm.toInt()))
                Slider(
                    value = rangeKm,
                    onValueChange = { rangeKm = it },
                    onValueChangeFinished = { reload() },
                    valueRange = 1f..200f,
                    colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
                )
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                loadError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.circuits_load_error))
                }
                circuits.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.circuits_empty))
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(circuits) { circuit ->
                        CircuitCard(circuit = circuit, onClick = { onOpenDetail(circuit.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CircuitCard(circuit: CircuitSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(circuit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.circuits_card_creator, circuit.creatorName),
                style = MaterialTheme.typography.bodySmall,
                color = Secondary,
            )
            Text(stringResource(R.string.circuits_card_distance_total, circuit.totalDistanceKm))
            Text(stringResource(R.string.circuits_card_waypoints, circuit.waypointCount), color = Secondary)
            circuit.distanceFromUserKm?.let {
                Text(
                    stringResource(R.string.circuits_card_distance_from_user, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                )
            }
        }
    }
}
