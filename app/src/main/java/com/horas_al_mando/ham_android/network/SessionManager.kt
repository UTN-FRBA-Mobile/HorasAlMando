package com.horas_al_mando.ham_android.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CLIENT_FLIGHT_ID = "client_flight_id"

        const val REFRESH_TOKEN_ID_COOKIE = "ham_refresh_token_id"
        const val REFRESH_TOKEN_COOKIE = "ham_refresh_token"
        private const val COOKIE_PREFIX = "cookie_"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveRefreshCookie(name: String, value: String) {
        prefs.edit().putString(COOKIE_PREFIX + name, value).apply()
    }

    fun getRefreshCookie(name: String): String? {
        return prefs.getString(COOKIE_PREFIX + name, null)
    }

    fun removeRefreshCookie(name: String) {
        prefs.edit().remove(COOKIE_PREFIX + name).apply()
    }

    fun hasRefreshCookies(): Boolean {
        return getRefreshCookie(REFRESH_TOKEN_ID_COOKIE) != null &&
            getRefreshCookie(REFRESH_TOKEN_COOKIE) != null
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveClientFlightId(id: String) {
        prefs.edit().putString(KEY_CLIENT_FLIGHT_ID, id).apply()
    }

    fun getClientFlightId(): String? {
        return prefs.getString(KEY_CLIENT_FLIGHT_ID, null)
    }

    fun clearClientFlightId() {
        prefs.edit().remove(KEY_CLIENT_FLIGHT_ID).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun hasToken(): Boolean {
        return getAuthToken() != null
    }
}
