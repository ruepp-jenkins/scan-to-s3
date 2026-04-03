package com.ruepp.scantoupload.data.preferences

import android.content.Context

class ServerConfig(context: Context) {

    private val prefs = context.getSharedPreferences("server_config", Context.MODE_PRIVATE)

    fun saveServerUrl(url: String) {
        prefs.edit().putString(KEY_SERVER_URL, url.trimEnd('/')).apply()
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, "") ?: ""
    }

    fun hasServerUrl(): Boolean {
        return getServerUrl().isNotBlank()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
    }
}
