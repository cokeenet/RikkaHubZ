package me.rerere.rikkahub.hermes

import kotlinx.serialization.Serializable

@Serializable
data class HermesMemoryMutation(
    val deviceId: String,
    val mutationId: String,
    val baseHash: String? = null,
    val targetId: String,
    val content: String,
    val status: HermesMemoryMutationStatus = HermesMemoryMutationStatus.Pending,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val lastError: String? = null,
    val bridgeStatus: String? = null,
    val alreadyProcessed: Boolean = false,
    val importedAtUtc: String? = null,
    val contentHash: String? = null,
    val previousHash: String? = null,
)

@Serializable
enum class HermesMemoryMutationStatus {
    Pending,
    Uploading,
    Imported,
    Conflict,
    Failed,
}

@Serializable
data class HermesMemoryMutationQueue(
    val mutations: List<HermesMemoryMutation> = emptyList(),
)

data class HermesMemoryQueueSummary(
    val total: Int,
    val pending: Int,
    val uploading: Int,
    val imported: Int,
    val conflict: Int,
    val failed: Int,
    val latestError: String?,
) {
    val hasRetryable: Boolean get() = pending > 0 || failed > 0

    companion object {
        val Empty = HermesMemoryQueueSummary(
            total = 0,
            pending = 0,
            uploading = 0,
            imported = 0,
            conflict = 0,
            failed = 0,
            latestError = null,
        )

        fun from(mutations: List<HermesMemoryMutation>): HermesMemoryQueueSummary {
            return HermesMemoryQueueSummary(
                total = mutations.size,
                pending = mutations.count { it.status == HermesMemoryMutationStatus.Pending },
                uploading = mutations.count { it.status == HermesMemoryMutationStatus.Uploading },
                imported = mutations.count { it.status == HermesMemoryMutationStatus.Imported },
                conflict = mutations.count { it.status == HermesMemoryMutationStatus.Conflict },
                failed = mutations.count { it.status == HermesMemoryMutationStatus.Failed },
                latestError = mutations
                    .asReversed()
                    .firstOrNull { !it.lastError.isNullOrBlank() }
                    ?.lastError,
            )
        }
    }
}

object HermesMemoryMutationReducer {
    fun markUploading(
        mutation: HermesMemoryMutation,
        nowMillis: Long,
    ): HermesMemoryMutation {
        return mutation.copy(
            status = HermesMemoryMutationStatus.Uploading,
            updatedAtMillis = nowMillis,
            lastError = null,
        )
    }

    fun applyResponse(
        mutation: HermesMemoryMutation,
        response: HermesMemoryImportResponse,
        nowMillis: Long,
    ): HermesMemoryMutation {
        val imported = response.status.equals("imported", ignoreCase = true)
        val conflict = response.status.equals("conflict", ignoreCase = true)
        return mutation.copy(
            status = when {
                imported -> HermesMemoryMutationStatus.Imported
                conflict -> HermesMemoryMutationStatus.Conflict
                else -> HermesMemoryMutationStatus.Failed
            },
            updatedAtMillis = nowMillis,
            lastError = when {
                imported -> null
                conflict -> response.message.ifBlank { "Memory import conflict." }
                else -> response.message.ifBlank { "Memory import returned status: ${response.status}" }
            },
            bridgeStatus = response.status,
            alreadyProcessed = response.alreadyProcessed,
            importedAtUtc = response.importedAtUtc,
            contentHash = response.contentHash,
            previousHash = response.previousHash,
        )
    }

    fun markFailed(
        mutation: HermesMemoryMutation,
        error: Throwable,
        nowMillis: Long,
    ): HermesMemoryMutation {
        return mutation.copy(
            status = HermesMemoryMutationStatus.Failed,
            updatedAtMillis = nowMillis,
            lastError = error.message ?: error::class.java.simpleName,
        )
    }
}
