package com.horas_al_mando.ham_android.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(private val sessionManager: SessionManager) : CookieJar {

    private val trackedCookies = setOf(
        SessionManager.REFRESH_TOKEN_ID_COOKIE,
        SessionManager.REFRESH_TOKEN_COOKIE
    )

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            if (cookie.name !in trackedCookies) return@forEach
            val expired = cookie.expiresAt < System.currentTimeMillis()
            if (cookie.value.isEmpty() || expired) {
                sessionManager.removeRefreshCookie(cookie.name)
            } else {
                sessionManager.saveRefreshCookie(cookie.name, cookie.value)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.encodedPath.contains("/auth/")) return emptyList()

        return trackedCookies.mapNotNull { name ->
            sessionManager.getRefreshCookie(name)?.let { value ->
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(url.host)
                    .path("/")
                    .build()
            }
        }
    }
}
