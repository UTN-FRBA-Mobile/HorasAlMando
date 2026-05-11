package com.horas_al_mando.ham_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.horas_al_mando.ham_android.ui.components.HamBottomBar
import com.horas_al_mando.ham_android.ui.screens.*
import com.horas_al_mando.ham_android.ui.theme.HamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HamTheme {
                HamApp()
            }
        }
    }
}

@Composable
fun HamApp() {
    val rootNav = rememberNavController()

    NavHost(navController = rootNav, startDestination = "login") {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                rootNav.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainScreen(onLogout = {
                rootNav.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            })
        }
    }
}

@Composable
fun MainScreen(onLogout: () -> Unit) {
    val innerNav    = rememberNavController()
    val backStack   by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute?.startsWith("replay") == false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HamBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        innerNav.navigate(route) {
                            popUpTo(innerNav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = innerNav,
            startDestination = "flight",
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable("flight") {
                FlightScreen()
            }
            composable("history") {
                HistoryScreen(onOpenReplay = { id -> innerNav.navigate("replay/$id") })
            }
            composable("profile") {
                ProfileScreen(onLogout = onLogout)
            }
            composable("replay/{flightId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("flightId")?.toIntOrNull() ?: 0
                ReplayScreen(
                    flightId = id,
                    onBack   = { innerNav.popBackStack() },
                )
            }
        }
    }
}
