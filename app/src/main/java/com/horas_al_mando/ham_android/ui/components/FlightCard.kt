package com.horas_al_mando.ham_android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.model.MockData
import com.horas_al_mando.ham_android.ui.theme.CardShape
import com.horas_al_mando.ham_android.ui.theme.Outline
import com.horas_al_mando.ham_android.ui.theme.Secondary

@Composable
fun FlightHistoryCard(
    flight  : MockData.StoredFlight,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text       = flight.label,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text  = "Duración: ${flight.duration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                )
                Text(
                    text  = "Distancia: ${flight.distance}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary,
                )
            }
        }
    }
}
