package com.notediscovery.app.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notediscovery.app.data.api.NoteDiscoveryClient
import com.notediscovery.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String = "",
    val apiKey: String = "",
    val testResult: String? = null,
    val isTesting: Boolean = false
)

class SettingsViewModel(
    private val repository: NotesRepository,
    context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val savedUrl = prefs.getString("server_url", null)
        val savedKey = prefs.getString("api_key", null)
        if (savedUrl != null && savedKey != null) {
            val fixedUrl = ensureHttpPrefix(savedUrl)
            _uiState.value = SettingsUiState(serverUrl = fixedUrl, apiKey = savedKey)
            repository.updateConfig(fixedUrl, savedKey)
        }
    }

    fun updateUrl(url: String) { _uiState.value = _uiState.value.copy(serverUrl = url) }
    fun updateKey(key: String) { _uiState.value = _uiState.value.copy(apiKey = key) }

    private fun ensureHttpPrefix(url: String): String {
        if (url.isBlank()) return url
        return if (url.startsWith("http://") || url.startsWith("https://")) url
        else "http://$url"
    }

    fun save() {
        val state = _uiState.value
        val fixedUrl = ensureHttpPrefix(state.serverUrl)
        _uiState.value = state.copy(serverUrl = fixedUrl)
        prefs.edit()
            .putString("server_url", fixedUrl)
            .putString("api_key", state.apiKey)
            .apply()
        repository.updateConfig(fixedUrl, state.apiKey)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            repository.testConnection().fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isTesting = false, testResult = "✅ $msg"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isTesting = false, testResult = "❌ ${e.message ?: e.toString()}"
                    )
                }
            )
        }
    }
}
