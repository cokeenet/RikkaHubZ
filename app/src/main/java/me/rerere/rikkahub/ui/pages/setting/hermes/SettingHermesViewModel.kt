package me.rerere.rikkahub.ui.pages.setting.hermes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.hermes.HermesBridgeClient
import me.rerere.rikkahub.hermes.HermesBridgeConfig
import me.rerere.rikkahub.hermes.HermesBridgePreferences
import me.rerere.rikkahub.hermes.HermesMemoryMutation
import me.rerere.rikkahub.hermes.HermesMemoryMutationRepository
import me.rerere.rikkahub.hermes.HermesMemoryQueueSummary
import me.rerere.rikkahub.hermes.HermesMemoryUploadSummary
import me.rerere.rikkahub.hermes.HermesSnapshot
import me.rerere.rikkahub.hermes.HermesSyncRunPhaseResult
import me.rerere.rikkahub.hermes.HermesSyncStatusResponse
import me.rerere.rikkahub.hermes.HermesSyncRepository

class SettingHermesViewModel(
    private val preferences: HermesBridgePreferences,
    private val client: HermesBridgeClient,
    private val syncRepository: HermesSyncRepository,
    private val memoryMutationRepository: HermesMemoryMutationRepository,
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

    val memoryQueueState: StateFlow<HermesMemoryQueueUiState> =
        memoryMutationRepository.queueFlow.map { queue ->
            HermesMemoryQueueUiState(
                mutations = queue.mutations,
                summary = HermesMemoryQueueSummary.from(queue.mutations),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HermesMemoryQueueUiState.Empty,
        )

    var probeState = kotlinx.coroutines.flow.MutableStateFlow<HermesProbeUiState>(HermesProbeUiState.Idle)
        private set

    var syncState = kotlinx.coroutines.flow.MutableStateFlow<HermesSyncUiState>(HermesSyncUiState.Idle)
        private set

    var bridgeSyncState = kotlinx.coroutines.flow.MutableStateFlow<HermesBridgeSyncUiState>(HermesBridgeSyncUiState.Idle)
        private set

    var memoryMutationState = kotlinx.coroutines.flow.MutableStateFlow<HermesMemoryMutationUiState>(HermesMemoryMutationUiState.Idle)
        private set

    var memoryUploadState = kotlinx.coroutines.flow.MutableStateFlow<HermesMemoryUploadUiState>(HermesMemoryUploadUiState.Idle)
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

    fun refreshBridgeSyncStatus() {
        viewModelScope.launch {
            bridgeSyncState.value = HermesBridgeSyncUiState.Loading
            runCatching {
                syncRepository.getBridgeSyncStatus()
            }.onSuccess { status ->
                bridgeSyncState.value = HermesBridgeSyncUiStateMapper.fromStatus(status)
            }.onFailure { error ->
                bridgeSyncState.value = HermesBridgeSyncUiState.Error(
                    message = error.message ?: error::class.java.simpleName,
                    lastKnown = bridgeSyncState.value.statusOrNull(),
                )
            }
        }
    }

    fun triggerBridgeSync() {
        viewModelScope.launch {
            bridgeSyncState.value = HermesBridgeSyncUiState.RunningAction(
                lastKnown = bridgeSyncState.value.statusOrNull(),
            )
            runCatching {
                syncRepository.runBridgeSync()
            }.onSuccess { response ->
                bridgeSyncState.value = HermesBridgeSyncUiStateMapper.fromRunResponse(
                    started = response.started,
                    status = response.status,
                )
            }.onFailure { error ->
                bridgeSyncState.value = HermesBridgeSyncUiState.Error(
                    message = error.message ?: error::class.java.simpleName,
                    lastKnown = bridgeSyncState.value.statusOrNull(),
                )
            }
        }
    }

    fun createMemoryMutation(
        targetId: String,
        content: String,
        baseHash: String,
    ) {
        viewModelScope.launch {
            val cleanTargetId = targetId.trim()
            val cleanContent = content.trim()
            if (cleanTargetId.isBlank() || cleanContent.isBlank()) {
                memoryMutationState.value = HermesMemoryMutationUiState.Error("Target ID 和内容都不能为空")
                return@launch
            }

            memoryMutationState.value = HermesMemoryMutationUiState.Loading
            runCatching {
                memoryMutationRepository.createMutation(
                    targetId = cleanTargetId,
                    content = cleanContent,
                    baseHash = baseHash,
                )
            }.onSuccess { mutation ->
                memoryMutationState.value = HermesMemoryMutationUiState.Success(mutation.mutationId)
            }.onFailure { error ->
                memoryMutationState.value = HermesMemoryMutationUiState.Error(
                    error.message ?: error::class.java.simpleName
                )
            }
        }
    }

    fun uploadMemoryMutations() {
        viewModelScope.launch {
            memoryUploadState.value = HermesMemoryUploadUiState.Loading
            runCatching {
                memoryMutationRepository.uploadRetryable()
            }.onSuccess { summary ->
                memoryUploadState.value = HermesMemoryUploadUiState.Success(summary)
            }.onFailure { error ->
                memoryUploadState.value = HermesMemoryUploadUiState.Error(
                    error.message ?: error::class.java.simpleName
                )
            }
        }
    }

    fun clearImportedMemoryMutations() {
        viewModelScope.launch {
            memoryMutationRepository.clearCompleted()
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

sealed interface HermesBridgeSyncUiState {
    data object Idle : HermesBridgeSyncUiState
    data object Loading : HermesBridgeSyncUiState
    data class RunningAction(val lastKnown: BridgeSyncStatusUi?) : HermesBridgeSyncUiState
    data class Available(val status: BridgeSyncStatusUi, val actionMessage: String? = null) : HermesBridgeSyncUiState
    data class Error(val message: String, val lastKnown: BridgeSyncStatusUi?) : HermesBridgeSyncUiState
}

data class BridgeSyncStatusUi(
    val isRunning: Boolean,
    val currentPhase: String,
    val lastStartedAtUtc: String?,
    val lastCompletedAtUtc: String?,
    val lastSucceeded: Boolean?,
    val lastError: String?,
    val lastTrigger: String,
    val lastPhaseResults: List<HermesSyncRunPhaseResult>,
) {
    val headline: String
        get() = when {
            isRunning -> "电脑端正在同步"
            lastSucceeded == true -> "电脑端上次同步成功"
            lastSucceeded == false -> "电脑端上次同步失败"
            else -> "电脑端尚无同步结果"
        }

    val diagnosticMessage: String
        get() = when {
            isRunning -> "阶段: ${currentPhase.ifBlank { "未知" }}"
            lastSucceeded == true -> "完成时间: ${lastCompletedAtUtc ?: "未知"}"
            lastSucceeded == false -> lastError?.ifBlank { null } ?: "Bridge 未返回错误信息"
            else -> "可以从手机触发 Bridge 执行一次同步"
        }
}

object HermesBridgeSyncUiStateMapper {
    fun fromStatus(status: HermesSyncStatusResponse): HermesBridgeSyncUiState.Available {
        return HermesBridgeSyncUiState.Available(status = status.toUi())
    }

    fun fromRunResponse(
        started: Boolean,
        status: HermesSyncStatusResponse,
    ): HermesBridgeSyncUiState.Available {
        return HermesBridgeSyncUiState.Available(
            status = status.toUi(),
            actionMessage = if (started) "已触发电脑端同步" else "电脑端已有同步任务在运行",
        )
    }

    private fun HermesSyncStatusResponse.toUi(): BridgeSyncStatusUi {
        return BridgeSyncStatusUi(
            isRunning = isRunning,
            currentPhase = currentPhase,
            lastStartedAtUtc = lastStartedAtUtc,
            lastCompletedAtUtc = lastCompletedAtUtc,
            lastSucceeded = lastSucceeded,
            lastError = lastError,
            lastTrigger = lastTrigger,
            lastPhaseResults = lastPhaseResults,
        )
    }
}

private fun HermesBridgeSyncUiState.statusOrNull(): BridgeSyncStatusUi? = when (this) {
    is HermesBridgeSyncUiState.Available -> status
    is HermesBridgeSyncUiState.Error -> lastKnown
    is HermesBridgeSyncUiState.RunningAction -> lastKnown
    HermesBridgeSyncUiState.Idle,
    HermesBridgeSyncUiState.Loading -> null
}

data class HermesMemoryQueueUiState(
    val mutations: List<HermesMemoryMutation>,
    val summary: HermesMemoryQueueSummary,
) {
    companion object {
        val Empty = HermesMemoryQueueUiState(
            mutations = emptyList(),
            summary = HermesMemoryQueueSummary.Empty,
        )
    }
}

sealed interface HermesMemoryMutationUiState {
    data object Idle : HermesMemoryMutationUiState
    data object Loading : HermesMemoryMutationUiState
    data class Success(val mutationId: String) : HermesMemoryMutationUiState
    data class Error(val message: String) : HermesMemoryMutationUiState
}

sealed interface HermesMemoryUploadUiState {
    data object Idle : HermesMemoryUploadUiState
    data object Loading : HermesMemoryUploadUiState
    data class Success(val summary: HermesMemoryUploadSummary) : HermesMemoryUploadUiState
    data class Error(val message: String) : HermesMemoryUploadUiState
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
