package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.model.FlightTrackSummary
import com.horas_al_mando.ham_android.network.FlightTrackRepository
import com.horas_al_mando.ham_android.ui.components.FlightHistoryCard
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    repository  : FlightTrackRepository,
    onOpenReplay: (Long) -> Unit,
) {
    var tracks    by remember { mutableStateOf<List<FlightTrackSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            repository.getTracks().onSuccess { tracks = it }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text  = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(4.dp))
        }
        if (tracks.isEmpty()) {
            item {
                Text(
                    text  = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(tracks) { track ->
                FlightHistoryCard(
                    track   = track,
                    onClick = { onOpenReplay(track.id) },
                )
            }
        }
    }
}
