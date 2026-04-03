package com.ruepp.scantoupload.data.preferences

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ServerConfig(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "server_config_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, "") ?: ""
    }

    fun saveAppToken(token: String) {
        prefs.edit().putString(KEY_APP_TOKEN, token).apply()
    }

    fun getAppToken(): String {
        return prefs.getString(KEY_APP_TOKEN, "") ?: ""
    }

    fun isConfigured(): Boolean {
        return getServerUrl().isNotBlank() && getAppToken().isNotBlank()
    }

    fun clear() {
        prefs.edit().remove(KEY_SERVER_URL).remove(KEY_APP_TOKEN).apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_APP_TOKEN = "app_token"
    }
}
