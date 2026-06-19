package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/verify-registration")
    suspend fun verifyRegistration(@Body request: VerifyRegistrationRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    fun refresh(): retrofit2.Call<RefreshResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") id: String): Response<UserProfile>
}
