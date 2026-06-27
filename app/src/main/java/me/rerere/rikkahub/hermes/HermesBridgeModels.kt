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
