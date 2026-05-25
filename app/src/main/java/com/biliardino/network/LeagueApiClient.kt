package com.biliardino.network

import com.biliardino.model.LeagueResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface LeagueApiClient {
    @GET("public/leghe")
    suspend fun publicLeagues(): List<LeagueResponse>

    @GET("leghe/mie")
    suspend fun myLeagues(@Header("Authorization") bearer: String): List<LeagueResponse>

    companion object {
        val instance: LeagueApiClient by lazy { ApiClientBase.service() }
    }
}
