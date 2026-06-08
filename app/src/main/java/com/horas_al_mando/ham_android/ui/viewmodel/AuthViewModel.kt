package com.horas_al_mando.ham_android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horas_al_mando.ham_android.model.*
import com.horas_al_mando.ham_android.network.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
    data class NeedsVerification(val username: String, val password: String) : AuthState()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful) {
                    _state.value = AuthState.Success
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Credenciales inválidas."
                        else -> "Error en el servidor: ${response.code()}"
                    }
                    _state.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful) {
                    _state.value = AuthState.NeedsVerification(request.username, request.password)
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "El usuario o email ya existe."
                        else -> "Error al registrarse: ${response.code()}"
                    }
                    _state.value = AuthState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun verify(request: VerifyRegistrationRequest) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val response = repository.verifyRegistration(request)
                if (response.isSuccessful) {
                    _state.value = AuthState.Success
                } else {
                    _state.value = AuthState.Error("Código inválido o expirado.")
                }
            } catch (e: Exception) {
                _state.value = AuthState.Error("Error de red: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}
