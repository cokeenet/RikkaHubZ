package me.rerere.rikkahub.ui.pages.setting.hermes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.hermes.HermesBridgeClient
import me.rerere.rikkahub.hermes.HermesBridgeConfig
import me.rerere.rikkahub.hermes.HermesBridgePreferences
import me.rerere.rikkahub.hermes.HermesSnapshot
import me.rerere.rikkahub.hermes.HermesSyncRepository

class SettingHermesViewModel(
    private val preferences: HermesBridgePreferences,
    private val client: HermesBridgeClient,
    private val syncRepository: HermesSyncRepository,
) : ViewModel() {
    val config: StateFlow<HermesBridgeConfig> = preferences.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HermesBridgeConfig(),
    )

    val cacheState: StateFlow<HermesCacheUiState> = combine(
        config,
        syncRepository.snapshotFlow,
    ) { config, snapshot ->
        HermesCacheUiState.from(config, snapshot)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HermesCacheUiState.Empty,
    )

    var probeState = kotlinx.coroutines.flow.MutableStateFlow<HermesProbeUiState>(HermesProbeUiState.Idle)
        private set

    var syncState = kotlinx.coroutines.flow.MutableStateFlow<HermesSyncUiState>(HermesSyncUiState.Idle)
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

    fun syncToPhone() {
        viewModelScope.launch {
            syncState.value = HermesSyncUiState.Loading
            runCatching {
                syncRepository.syncNow()
            }.onSuccess { snapshot ->
                syncState.value = HermesSyncUiState.Success(
                    syncedAtMillis = snapshot.syncedAtMillis,
                    personalityExists = snapshot.hasPersonality,
                    memoryCount = snapshot.memories.size,
                )
            }.onFailure { error ->
                syncState.value = HermesSyncUiState.Error(error.message ?: error::class.java.simpleName)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            syncRepository.clearSnapshot()
            syncState.value = HermesSyncUiState.Idle
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

sealed interface HermesSyncUiState {
    data object Idle : HermesSyncUiState
    data object Loading : HermesSyncUiState

    data class Success(
        val syncedAtMillis: Long,
        val personalityExists: Boolean,
        val memoryCount: Int,
    ) : HermesSyncUiState

    data class Error(val message: String) : HermesSyncUiState
}

sealed interface HermesCacheUiState {
    val lastSyncedAt: Long?
    val sourceBaseUrl: String
    val personalityExists: Boolean
    val memoryCount: Int
    val isUsable: Boolean

    data object Empty : HermesCacheUiState {
        override val lastSyncedAt: Long? = null
        override val sourceBaseUrl: String = ""
        override val personalityExists: Boolean = false
        override val memoryCount: Int = 0
        override val isUsable: Boolean = false
    }

    data class Available(
        override val lastSyncedAt: Long,
        override val sourceBaseUrl: String,
        override val personalityExists: Boolean,
        override val memoryCount: Int,
        override val isUsable: Boolean,
    ) : HermesCacheUiState

    companion object {
        fun from(config: HermesBridgeConfig, snapshot: HermesSnapshot?): HermesCacheUiState {
            if (snapshot == null) {
                return Empty
            }
            return Available(
                lastSyncedAt = snapshot.syncedAtMillis,
                sourceBaseUrl = snapshot.sourceBaseUrl.ifBlank { config.baseUrl },
                personalityExists = snapshot.hasPersonality,
                memoryCount = snapshot.memories.size,
                isUsable = snapshot.isUsable,
            )
        }
    }
}
