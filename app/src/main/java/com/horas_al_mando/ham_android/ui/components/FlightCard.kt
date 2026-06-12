package com.horas_al_mando.ham_android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.model.FlightTrackSummary
import com.horas_al_mando.ham_android.ui.theme.CardShape
import com.horas_al_mando.ham_android.ui.theme.Outline
import com.horas_al_mando.ham_android.ui.theme.Secondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

@Composable
fun FlightHistoryCard(
    track   : FlightTrackSummary,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = "${track.locationName ?: stringResource(R.string.flight_default_name)} – ${dateFormatter.format(Instant.parse(track.startTime))}"

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = CardShape,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, Outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = stringResource(R.string.flight_duration, track.totalDurationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = Secondary,
            )
        }
    }
}
