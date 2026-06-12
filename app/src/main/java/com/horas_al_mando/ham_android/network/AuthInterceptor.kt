package com.horas_al_mando.ham_android.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        val path = chain.request().url.encodedPath
        if (!path.contains("/auth/")) {
            sessionManager.getAuthToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
