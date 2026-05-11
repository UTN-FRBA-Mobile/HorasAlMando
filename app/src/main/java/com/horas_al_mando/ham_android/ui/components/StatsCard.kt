package com.horas_al_mando.ham_android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horas_al_mando.ham_android.ui.theme.Secondary

@Composable
fun StatsCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = Secondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text       = value,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 18.sp,
            color      = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun StatsGrid(
    altitude: String,
    speed   : String,
    heading : String,
    elapsed : String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            StatsCell("Altitud (ft)",    altitude, Modifier.weight(1f))
            StatsCell("Velocidad (kt)",  speed,    Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            StatsCell("Rumbo (°)", heading, Modifier.weight(1f))
            StatsCell("Tiempo",    elapsed, Modifier.weight(1f))
        }
    }
}
