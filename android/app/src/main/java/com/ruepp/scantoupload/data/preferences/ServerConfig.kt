package com.ruepp.scantoupload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ServerConfig(context: Context) {

    private val prefs: SharedPreferences = createPreferences(context)

    private fun createPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME_SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encrypted preferences. Using fallback.", e)
            context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    fun saveServerUrl(url: String) {
        runCatching {
            prefs.edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
        }.onFailure { error ->
            Log.e(TAG, "Failed to save server URL", error)
        }
    }

    fun getServerUrl(): String {
        return runCatching {
            prefs.getString(KEY_SERVER_URL, "") ?: ""
        }.onFailure { error ->
            Log.e(TAG, "Failed to read server URL", error)
        }.getOrDefault("")
    }

    fun saveAppToken(token: String) {
        runCatching {
            prefs.edit().putString(KEY_APP_TOKEN, token).apply()
        }.onFailure { error ->
            Log.e(TAG, "Failed to save app token", error)
        }
    }

    fun getAppToken(): String {
        return runCatching {
            prefs.getString(KEY_APP_TOKEN, "") ?: ""
        }.onFailure { error ->
            Log.e(TAG, "Failed to read app token", error)
        }.getOrDefault("")
    }

    fun isConfigured(): Boolean {
        return runCatching {
            getServerUrl().isNotBlank() && getAppToken().isNotBlank()
        }.onFailure { error ->
            Log.e(TAG, "Failed to check server configuration", error)
        }.getOrDefault(false)
    }

    fun clear() {
        runCatching {
            prefs.edit().remove(KEY_SERVER_URL).remove(KEY_APP_TOKEN).apply()
        }.onFailure { error ->
            Log.e(TAG, "Failed to clear server configuration", error)
        }
    }

    companion object {
        private const val TAG = "ServerConfig"
        private const val PREFS_NAME_SECURE = "server_config_secure"
        private const val PREFS_NAME_FALLBACK = "server_config_fallback"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_APP_TOKEN = "app_token"
    }
}
