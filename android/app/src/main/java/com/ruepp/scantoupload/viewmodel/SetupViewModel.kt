package com.ruepp.scantoupload.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruepp.scantoupload.data.api.ApiClient
import com.ruepp.scantoupload.data.preferences.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

        if (state.serverUrl.isBlank()) {
            _uiState.value = state.copy(error = "Server URL is required")
            return
        }
        if (state.appToken.isBlank()) {
            _uiState.value = state.copy(error = "App token is required")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        // Save before verifying so ApiClient picks up the values
        serverConfig.saveServerUrl(state.serverUrl)
        serverConfig.saveAppToken(state.appToken)

        val apiClient = ApiClient(serverConfig)

        viewModelScope.launch(Dispatchers.IO) {
            val result = apiClient.checkStatus()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        setupSuccess = true
                    )
                },
                onFailure = { error ->
                    // Clear saved config on failure so isConfigured() returns false
                    serverConfig.clear()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Connection failed"
                    )
                }
            )
        }
    }
}
