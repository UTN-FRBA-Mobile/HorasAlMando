package com.horas_al_mando.ham_android.model

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String
)

data class VerifyRegistrationRequest(
    val username: String,
    val password: String,
    val code: String,
    val deviceName: String,
    val deviceType: String = "MOBILE"
)

data class LoginRequest(
    val username: String,
    val password: String,
    val deviceName: String,
    val deviceType: String = "MOBILE"
)

data class AuthResponse(
    val token: String?,
    val userId: String?,
    val active: Boolean?
)

data class RefreshResponse(
    val token: String?
)

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val license: String?,
    val totalHours: String?
)
