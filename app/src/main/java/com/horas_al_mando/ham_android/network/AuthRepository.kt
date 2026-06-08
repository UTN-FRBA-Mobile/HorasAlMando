package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.*
import retrofit2.Response

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) {
    suspend fun register(request: RegisterRequest): Response<Unit> {
        return authApi.register(request)
    }

    suspend fun verifyRegistration(request: VerifyRegistrationRequest): Response<AuthResponse> {
        val response = authApi.verifyRegistration(request)
        if (response.isSuccessful) {
            response.body()?.let {
                if (it.token != null && it.userId != null) {
                    sessionManager.saveAuthToken(it.token)
                    sessionManager.saveUserId(it.userId)
                }
            }
        }
        return response
    }

    suspend fun login(request: LoginRequest): Response<AuthResponse> {
        val response = authApi.login(request)
        if (response.isSuccessful) {
            response.body()?.let {
                if (it.token != null && it.userId != null) {
                    sessionManager.saveAuthToken(it.token)
                    sessionManager.saveUserId(it.userId)
                }
            }
        }
        return response
    }

    suspend fun logout(): Response<Unit> {
        val response = authApi.logout()
        sessionManager.clearSession()
        return response
    }

    suspend fun getUserProfile(id: String): Response<UserProfile> {
        return authApi.getUserProfile(id)
    }

    fun isLoggedIn(): Boolean = sessionManager.hasToken()
    fun getUserId(): String? = sessionManager.getUserId()
}
