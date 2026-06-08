package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.model.MockData
import com.horas_al_mando.ham_android.ui.theme.*

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = CardShape,
            colors    = CardDefaults.cardColors(containerColor = Surface),
            border    = BorderStroke(1.dp, Outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ProfileRow(label = "Nombre",                value = MockData.PILOT_NAME)
                HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow(label = "Licencia",              value = MockData.PILOT_LICENSE)
                HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 8.dp))
                ProfileRow(label = "Horas totales de vuelo", value = MockData.PILOT_TOTAL_HOURS)
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick        = onLogout,
            modifier       = Modifier.fillMaxWidth(),
            border         = BorderStroke(1.dp, Outline),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold, color = Destructive)
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Secondary)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
