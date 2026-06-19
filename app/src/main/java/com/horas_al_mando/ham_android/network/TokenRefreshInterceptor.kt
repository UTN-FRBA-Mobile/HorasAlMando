package com.horas_al_mando.ham_android.network

import okhttp3.Interceptor
import okhttp3.Response

class TokenRefreshInterceptor(
    private val sessionManager: SessionManager,
    private val refreshApiProvider: () -> AuthApi
) : Interceptor {

    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.url.encodedPath.contains("/auth/")) return response
        if (response.code != 401 && response.code != 403) return response
        if (!sessionManager.hasRefreshCookies()) return response

        val recovered = synchronized(lock) {
            val current = sessionManager.getAuthToken()
            val requestToken = request.header("Authorization")?.removePrefix("Bearer ")
            if (current != null && current != requestToken) {
                true
            } else {
                refreshToken()
            }
        }

        if (!recovered) {
            sessionManager.clearSession()
            SessionEvents.notifyExpired()
            return response
        }

        val newToken = sessionManager.getAuthToken() ?: return response
        response.close()

        val retried = request.newBuilder()
            .removeHeader("Authorization")
            .addHeader("Authorization", "Bearer $newToken")
            .build()

        return chain.proceed(retried)
    }

    private fun refreshToken(): Boolean {
        return try {
            val resp = refreshApiProvider().refresh().execute()
            val newToken = resp.body()?.token
            if (resp.isSuccessful && !newToken.isNullOrBlank()) {
                sessionManager.saveAuthToken(newToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
