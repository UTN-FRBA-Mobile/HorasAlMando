package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.model.FlightTrackDetail
import com.horas_al_mando.ham_android.network.FlightTrackRepository
import com.horas_al_mando.ham_android.ui.CLEAN_MAP_STYLE_JSON
import com.horas_al_mando.ham_android.ui.components.StatsGrid
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

@Composable
fun ReplayScreen(
    flightId  : Long,
    repository: FlightTrackRepository,
    onBack    : () -> Unit,
) {
    var detail    by remember { mutableStateOf<FlightTrackDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(flightId) {
        scope.launch {
            repository.getTrackDetail(flightId).onSuccess { detail = it }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.replay_not_found))
        }
        return
    }

    val track = detail!!
    val path = track.actualPath
    val defaultFlightName = stringResource(R.string.flight_default_name)

    var sliderPos by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    val maxIndex  = (path.size - 1).coerceAtLeast(0)
    val index     = sliderPos.toInt().coerceIn(0, maxIndex)
    val point     = path.getOrNull(index)

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && sliderPos < maxIndex) {
                delay(1000)
                sliderPos = (sliderPos + 1f).coerceAtMost(maxIndex.toFloat())
            }
            isPlaying = false
        }
    }

    val startInstant = remember { Instant.parse(path.first().timestamp) }
    val elapsed = point?.let {
        val secs = ChronoUnit.SECONDS.between(startInstant, Instant.parse(it.timestamp))
        val h = secs / 3600; val m = (secs % 3600) / 60; val s = secs % 60
        "%02d:%02d:%02d".format(h, m, s)
    } ?: "--"

    val label          = "${track.locationName ?: defaultFlightName} – ${dateFormatter.format(Instant.parse(track.startTime))}"
    val initialCenter  = LatLng(path.first().lat, path.first().lng)
    val mapUiSettings  = remember { MapUiSettings(zoomControlsEnabled = false) }
    val mapProperties  = remember { MapProperties(mapStyleOptions = MapStyleOptions(CLEAN_MAP_STYLE_JSON)) }
    val polylinePoints = remember { path.map { LatLng(it.lat, it.lng) } }
    val markerState    = remember { MarkerState(position = initialCenter) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 13f)
    }

    LaunchedEffect(index) {
        point?.let {
            val pos = LatLng(it.lat, it.lng)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 13f)
            markerState.position = pos
        }
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
                    color  = Secondary,
                    width  = 4f,
                )
            }
            if (point != null) {
                Marker(
                    state = markerState,
                    title = elapsed,
                )
            }
        }

        Button(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) { Text(stringResource(R.string.replay_back_button)) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                StatsGrid(
                    altitude = point?.altitude?.let { "%.0f".format(it) } ?: "--",
                    speed    = point?.speed?.let    { "%.0f".format(it) } ?: "--",
                    heading  = point?.heading?.let  { "%.0f".format(it) } ?: "--",
                    elapsed  = elapsed,
                )
                Slider(
                    value         = sliderPos,
                    onValueChange = { sliderPos = it; isPlaying = false },
                    valueRange    = 0f..maxIndex.toFloat(),
                    steps         = (path.size - 2).coerceAtLeast(0),
                    colors        = SliderDefaults.colors(
                        thumbColor       = Primary,
                        activeTrackColor = Primary,
                    ),
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick  = {
                            sliderPos = 0f
                            isPlaying = false
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.replay_reset_button)) }

                    Button(
                        onClick  = {
                            if (sliderPos >= maxIndex) sliderPos = 0f
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) { Text(stringResource(if (isPlaying) R.string.replay_pause_button else R.string.replay_play_button)) }
                }
            }
        }
    }
}
