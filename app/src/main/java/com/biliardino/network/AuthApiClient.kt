package com.biliardino.network

import com.biliardino.model.AuthResponse
import com.biliardino.model.LoginRequest
import com.biliardino.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiClient {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    companion object {
        val instance: AuthApiClient by lazy { ApiClientBase.service() }
    }
}
