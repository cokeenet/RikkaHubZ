package me.rerere.rikkahub.ui.pages.setting.hermes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.hermes.HermesBridgeClient
import me.rerere.rikkahub.hermes.HermesBridgeConfig
import me.rerere.rikkahub.hermes.HermesBridgePreferences

class SettingHermesViewModel(
    private val preferences: HermesBridgePreferences,
    private val client: HermesBridgeClient,
) : ViewModel() {
    val config: StateFlow<HermesBridgeConfig> = preferences.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HermesBridgeConfig(),
    )

    var probeState = kotlinx.coroutines.flow.MutableStateFlow<HermesProbeUiState>(HermesProbeUiState.Idle)
        private set

    fun setBaseUrl(value: String) {
        viewModelScope.launch {
            preferences.update { it.copy(baseUrl = value) }
        }
    }

    fun setApiToken(value: String) {
        viewModelScope.launch {
            preferences.update { it.copy(apiToken = value) }
        }
    }

    fun setFallbackProviderId(value: String) {
        viewModelScope.launch {
            preferences.update { it.copy(fallbackProviderId = value) }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val current = preferences.current()
            probeState.value = HermesProbeUiState.Loading
            runCatching {
                client.probe(current)
            }.onSuccess { result ->
                val now = System.currentTimeMillis()
                preferences.update {
                    it.copy(
                        lastStatusAt = now,
                        lastPersonalityAt = now,
                        lastMemoryAt = now,
                    )
                }
                probeState.value = HermesProbeUiState.Success(
                    service = result.status.service,
                    dataRoot = result.status.dataRoot,
                    dataRootExists = result.status.dataRootExists,
                    personalityExists = result.personality.exists,
                    memoryCount = result.memory.memories.size,
                )
            }.onFailure { error ->
                probeState.value = HermesProbeUiState.Error(error.message ?: error::class.java.simpleName)
            }
        }
    }
}

sealed interface HermesProbeUiState {
    data object Idle : HermesProbeUiState
    data object Loading : HermesProbeUiState

    data class Success(
        val service: String,
        val dataRoot: String,
        val dataRootExists: Boolean,
        val personalityExists: Boolean,
        val memoryCount: Int,
    ) : HermesProbeUiState

    data class Error(val message: String) : HermesProbeUiState
}
