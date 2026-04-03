package com.ruepp.scantoupload.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruepp.scantoupload.data.api.ApiClient
import com.ruepp.scantoupload.data.preferences.ServerConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class SetupUiState(
    val serverUrl: String = "",
    val appToken: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val setupSuccess: Boolean = false
)

class SetupViewModel(
    private val serverConfig: ServerConfig
) : ViewModel() {

    companion object {
        private const val TAG = "SetupViewModel"
    }

    private val _uiState = MutableStateFlow(
        SetupUiState(
            serverUrl = serverConfig.getServerUrl(),
            appToken = serverConfig.getAppToken()
        )
    )
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, error = null)
    }

    fun updateAppToken(token: String) {
        _uiState.value = _uiState.value.copy(appToken = token, error = null)
    }

    fun connect() {
        val state = _uiState.value
        if (state.isLoading) return

        val normalizedServerUrl = state.serverUrl.trim()
        val normalizedAppToken = state.appToken.trim()

        if (normalizedServerUrl.isBlank()) {
            _uiState.value = state.copy(error = "Server URL is required")
            return
        }
        val parsedUrl = normalizedServerUrl.toHttpUrlOrNull()
        if (parsedUrl == null) {
            _uiState.value = state.copy(
                error = "Invalid server URL. Include http:// or https://"
            )
            return
        }
        if (normalizedAppToken.isBlank()) {
            _uiState.value = state.copy(error = "App token is required")
            return
        }

        val formattedServerUrl = parsedUrl.toString().trimEnd('/')
        _uiState.value = state.copy(
            serverUrl = formattedServerUrl,
            appToken = normalizedAppToken,
            isLoading = true,
            error = null
        )

        val apiClient = runCatching {
            serverConfig.saveServerUrl(formattedServerUrl)
            serverConfig.saveAppToken(normalizedAppToken)
            ApiClient(serverConfig)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to prepare connection", error)
            safeClearConfig()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Connection setup failed: ${userMessage(error)}"
            )
            return
        }

        val connectExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Unhandled exception in connect coroutine", throwable)
            safeClearConfig()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Connection failed: ${userMessage(throwable)}"
            )
        }

        viewModelScope.launch(Dispatchers.IO + connectExceptionHandler) {
            try {
                val result = apiClient.checkStatus()
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            setupSuccess = true
                        )
                    },
                    onFailure = { error ->
                        safeClearConfig()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Connection failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected connection crash", e)
                safeClearConfig()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connection failed: ${userMessage(e)}"
                )
            }
        }
    }

    private fun safeClearConfig() {
        runCatching { serverConfig.clear() }
            .onFailure { error -> Log.e(TAG, "Failed to clear invalid config", error) }
    }

    private fun userMessage(error: Throwable): String {
        return error.message?.takeIf { it.isNotBlank() } ?: "Unknown error"
    }
}
