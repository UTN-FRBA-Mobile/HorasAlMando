package com.horas_al_mando.ham_android.network

import com.horas_al_mando.ham_android.model.FlightSyncPayload
import com.horas_al_mando.ham_android.model.FlightTrackDetail
import com.horas_al_mando.ham_android.model.FlightTrackSummary
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FlightApi {
    @POST("flight-tracks/finish")
    suspend fun finishFlight(@Body payload: FlightSyncPayload): Response<Unit>

    @GET("flight-tracks")
    suspend fun getFlightTracks(): Response<List<FlightTrackSummary>>

    @GET("flight-tracks/{id}")
    suspend fun getFlightTrackDetail(@Path("id") id: Long): Response<FlightTrackDetail>
}


object FlightApiService {
    val api: FlightApi by lazy {
        ApiClient.createService(FlightApi::class.java)
    }
}
