package com.horas_al_mando.ham_android.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    object Flight  : BottomTab("flight",  "Vuelo",     Icons.Default.Flight)
    object History : BottomTab("history", "Historial", Icons.Default.History)
    object Profile : BottomTab("profile", "Perfil",    Icons.Default.Person)
}

private val tabs = listOf(BottomTab.Flight, BottomTab.History, BottomTab.Profile)

@Composable
fun HamBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick  = { onNavigate(tab.route) },
                icon     = { Icon(tab.icon, contentDescription = tab.label) },
                label    = { Text(tab.label) },
                colors   = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}
