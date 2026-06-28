package me.rerere.rikkahub.hermes

import kotlinx.serialization.Serializable

@Serializable
data class HermesStatusResponse(
    val service: String = "",
    val status: String = "",
    val dataRoot: String = "",
    val dataRootExists: Boolean = false,
    val serverTimeUtc: String = "",
)

@Serializable
data class HermesPersonalityResponse(
    val exists: Boolean = false,
    val path: String = "",
    val content: String = "",
    val updatedAtUtc: String? = null,
)

@Serializable
data class HermesMemoryListResponse(
    val directory: String = "",
    val exists: Boolean = false,
    val memories: List<HermesMemoryFileResponse> = emptyList(),
)

@Serializable
data class HermesMemoryFileResponse(
    val id: String = "",
    val path: String = "",
    val content: String = "",
    val updatedAtUtc: String = "",
)

data class HermesBridgeProbeResult(
    val status: HermesStatusResponse,
    val personality: HermesPersonalityResponse,
    val memory: HermesMemoryListResponse,
)

@Serializable
data class HermesSyncRunPhaseResult(
    val phase: String = "",
    val success: Boolean = false,
    val message: String = "",
    val completedAtUtc: String = "",
)

@Serializable
data class HermesSyncStatusResponse(
    val isRunning: Boolean = false,
    val currentPhase: String = "",
    val lastStartedAtUtc: String? = null,
    val lastCompletedAtUtc: String? = null,
    val lastSucceeded: Boolean? = null,
    val lastError: String? = null,
    val lastTrigger: String = "",
    val lastPhaseResults: List<HermesSyncRunPhaseResult> = emptyList(),
)

@Serializable
data class HermesSyncRunResponse(
    val started: Boolean = false,
    val status: HermesSyncStatusResponse = HermesSyncStatusResponse(),
)

@Serializable
data class HermesMemoryImportRequest(
    val deviceId: String,
    val mutationId: String,
    val targetId: String,
    val baseHash: String? = null,
    val content: String,
)

@Serializable
data class HermesMemoryImportResponse(
    val deviceId: String = "",
    val mutationId: String = "",
    val targetId: String = "",
    val status: String = "",
    val alreadyProcessed: Boolean = false,
    val importedAtUtc: String = "",
    val contentHash: String = "",
    val previousHash: String? = null,
    val message: String = "",
)

@Serializable
data class HermesSnapshot(
    val syncedAtMillis: Long = 0L,
    val sourceBaseUrl: String = "",
    val service: String = "",
    val status: String = "",
    val dataRoot: String = "",
    val personality: HermesPersonalityResponse = HermesPersonalityResponse(),
    val memories: List<HermesMemoryFileResponse> = emptyList(),
) {
    val hasPersonality: Boolean get() = personality.exists && personality.content.isNotBlank()
    val hasMemories: Boolean get() = memories.any { it.content.isNotBlank() }
    val isUsable: Boolean get() = hasPersonality || hasMemories
}
