package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.model.MockData
import com.horas_al_mando.ham_android.ui.components.FlightHistoryCard

@Composable
fun HistoryScreen(onOpenReplay: (Int) -> Unit) {
    LazyColumn(
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text  = "Historial de Vuelos",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(4.dp))
        }
        items(MockData.mockFlights) { flight ->
            FlightHistoryCard(
                flight  = flight,
                onClick = { onOpenReplay(flight.id) },
            )
        }
    }
}
