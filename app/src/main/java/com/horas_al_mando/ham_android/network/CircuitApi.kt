package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.CircuitDetail
import com.horas_al_mando.ham_android.model.CircuitRun
import com.horas_al_mando.ham_android.model.CircuitSummary
import com.horas_al_mando.ham_android.model.CreateCircuitRequest
import com.horas_al_mando.ham_android.model.CreateCircuitRunRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CircuitApi {
    @GET("circuits")
    suspend fun getCircuits(
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("rangeKm") rangeKm: Double?,
        @Query("mine") mine: Boolean?,
    ): Response<List<CircuitSummary>>

    @GET("circuits/{id}")
    suspend fun getCircuit(@Path("id") id: Long): Response<CircuitDetail>

    @POST("circuits")
    suspend fun createCircuit(@Body body: CreateCircuitRequest): Response<CircuitDetail>

    @DELETE("circuits/{id}")
    suspend fun deleteCircuit(@Path("id") id: Long): Response<Unit>

    @GET("circuits/{id}/runs")
    suspend fun getRuns(@Path("id") id: Long): Response<List<CircuitRun>>

    @POST("circuits/{id}/runs")
    suspend fun createRun(
        @Path("id") id: Long,
        @Body body: CreateCircuitRunRequest,
    ): Response<CircuitRun>
}

object CircuitApiService {
    val api: CircuitApi by lazy {
        ApiClient.createService(CircuitApi::class.java)
    }
}
