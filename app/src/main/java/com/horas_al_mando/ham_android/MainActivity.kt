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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.horas_al_mando.ham_android.ui.components.HamBottomBar
import com.horas_al_mando.ham_android.ui.screens.*
import com.horas_al_mando.ham_android.ui.theme.HamTheme

import androidx.compose.runtime.collectAsState
import com.horas_al_mando.ham_android.service.FlightRepository
import com.horas_al_mando.ham_android.network.*
import com.horas_al_mando.ham_android.network.FlightTrackRepository
import com.horas_al_mando.ham_android.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ApiClient.init(this)
        val authApi = ApiClient.createService(AuthApi::class.java)
        authRepository = AuthRepository(authApi, ApiClient.getSessionManager())
        authViewModel = AuthViewModel(authRepository)

        setContent {
            HamTheme {
                HamApp(authRepository, authViewModel)
            }
        }
    }
}

@Composable
fun HamApp(authRepository: AuthRepository, authViewModel: AuthViewModel) {
    val rootNav = rememberNavController()
    val startDestination = if (authRepository.isLoggedIn()) {
        authRepository.getUserId()?.toLongOrNull()?.let { FlightRepository.setPilotId(it) }
        "main"
    } else "login"

    NavHost(navController = rootNav, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    authRepository.getUserId()?.toLongOrNull()?.let { FlightRepository.setPilotId(it) }
                    rootNav.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { rootNav.navigate("register") },
                onNavigateToVerify = { username, password ->
                    rootNav.navigate("verify/$username/$password")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = { rootNav.popBackStack() },
                onNavigateToVerify = { username, password ->
                    rootNav.navigate("verify/$username/$password")
                }
            )
        }
        composable("verify/{username}/{password}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            VerificationScreen(
                viewModel = authViewModel,
                username = username,
                password = password,
                onVerificationSuccess = {
                    authRepository.getUserId()?.toLongOrNull()?.let { FlightRepository.setPilotId(it) }
                    rootNav.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            val flightApi = ApiClient.createService(com.horas_al_mando.ham_android.network.FlightApi::class.java)
            MainScreen(
                repository           = authRepository,
                flightTrackRepository = FlightTrackRepository(flightApi),
                onLogout = {
                    rootNav.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                    authViewModel.resetState()
                }
            )
        }
    }
}

@Composable
fun MainScreen(
    repository           : AuthRepository,
    flightTrackRepository: FlightTrackRepository,
    onLogout             : () -> Unit,
) {
    val innerNav    = rememberNavController()
    val backStack   by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val isTracking by FlightRepository.isTracking.collectAsState()
    val circuitRepository = remember { CircuitRepository() }
    val mainRoutes = setOf("flight", "circuits", "history", "profile")
    val showBottomBar = currentRoute in mainRoutes && !isTracking

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
            composable("circuits") {
                CircuitsScreen(
                    repository   = circuitRepository,
                    onOpenDetail = { id -> innerNav.navigate("circuitDetail/$id") },
                    onCreate     = { innerNav.navigate("createCircuit") },
                )
            }
            composable("circuitDetail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                CircuitDetailScreen(
                    circuitId  = id,
                    repository = circuitRepository,
                    onBack     = { innerNav.popBackStack() },
                    onRecorrer = {
                        innerNav.navigate("flight") {
                            popUpTo("circuits")
                            launchSingleTop = true
                        }
                    },
                    onDeleted  = { innerNav.popBackStack() },
                )
            }
            composable("createCircuit") {
                CreateCircuitScreen(
                    repository = circuitRepository,
                    onBack     = { innerNav.popBackStack() },
                    onCreated  = { innerNav.popBackStack() },
                )
            }
            composable("history") {
                HistoryScreen(
                    repository   = flightTrackRepository,
                    onOpenReplay = { id -> innerNav.navigate("replay/$id") },
                )
            }
            composable("profile") {
                ProfileScreen(repository = repository, onLogout = onLogout)
            }
            composable("replay/{flightId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("flightId")?.toLongOrNull() ?: 0L
                ReplayScreen(
                    flightId   = id,
                    repository = flightTrackRepository,
                    onBack     = { innerNav.popBackStack() },
                )
            }
        }
    }
}
