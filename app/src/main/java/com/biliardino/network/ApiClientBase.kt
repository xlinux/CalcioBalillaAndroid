package com.biliardino.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiClientBase {
    //private const val BASE_URL = "https://wp1twi5jzp7iiox98gima8cp.212.227.188.124.sslip.io/"
    private const val BASE_URL = "http://192.168.1.40:8080/"

    var authToken: String? = null
    var onAuthFailure: (() -> Unit)? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
        isLenient = true
    }

    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        authToken?.let {
            request.header("Authorization", "Bearer $it")
        }
        chain.proceed(request.build())
    }

    private lateinit var retrofit: Retrofit

    fun init(context: Context) {
        val sessionManager = SessionManager(context)
        val authApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(OkHttpClient.Builder().addInterceptor(logger).build())
            .build()
            .create(AuthApi::class.java)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .authenticator(TokenAuthenticator(sessionManager, authApi))
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    fun <T> service(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    val auth: AuthApi by lazy { service(AuthApi::class.java) }
    val userSettings: UserSettingsApi by lazy { service(UserSettingsApi::class.java) }
    val leagues: LeagueApi by lazy { service(LeagueApi::class.java) }
    val competitions: CompetitionApi by lazy { service(CompetitionApi::class.java) }
    val matches: MatchApi by lazy { service(MatchApi::class.java) }
    val sports: SportApi by lazy { service(SportApi::class.java) }
    val profile: ProfileApi by lazy { service(ProfileApi::class.java) }
}
