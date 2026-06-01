package com.biliardino.network

import com.biliardino.model.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Se abbiamo già provato a fare l'autenticazione per questo request e abbiamo fallito (response.priorResponse != null),
        // allora non riproviamo per evitare loop infiniti.
        if (response.priorResponse != null) {
            return null
        }

        return runBlocking {
            val refreshToken = sessionManager.refreshToken.first()
            if (refreshToken == null) {
                ApiClientBase.onAuthFailure?.invoke()
                return@runBlocking null
            }

            try {
                val refreshResponse = authApi.refresh(RefreshRequest(refreshToken))
                val newJwt = refreshResponse.token ?: refreshResponse.jwt
                val newRefreshToken = refreshResponse.refreshToken ?: newJwt

                if (newJwt != null) {
                    sessionManager.saveTokens(newJwt, newRefreshToken ?: newJwt)
                    ApiClientBase.authToken = newJwt

                    // Ripeti la richiesta originale con il nuovo token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newJwt")
                        .build()
                } else {
                    ApiClientBase.onAuthFailure?.invoke()
                    null
                }
            } catch (e: Exception) {
                ApiClientBase.onAuthFailure?.invoke()
                null
            }
        }
    }
}
