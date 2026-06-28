package me.rerere.rikkahub.hermes

import kotlin.uuid.Uuid

class HermesMemoryMutationRepository(
    private val preferences: HermesBridgePreferences,
    private val client: HermesBridgeClient,
    private val deviceStore: HermesDeviceStore,
    private val mutationStore: HermesMemoryMutationStore,
) {
    val queueFlow = mutationStore.flow

    suspend fun createMutation(
        targetId: String,
        content: String,
        baseHash: String?,
    ): HermesMemoryMutation {
        val now = System.currentTimeMillis()
        val mutation = HermesMemoryMutation(
            deviceId = deviceStore.getOrCreateDeviceId(),
            mutationId = Uuid.random().toString(),
            baseHash = baseHash?.trim()?.ifBlank { null },
            targetId = targetId.trim(),
            content = content,
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        mutationStore.add(mutation)
        return mutation
    }

    suspend fun uploadRetryable(): HermesMemoryUploadSummary {
        val config = preferences.current()
        val retryable = mutationStore.current().mutations.filter {
            it.status == HermesMemoryMutationStatus.Pending ||
                it.status == HermesMemoryMutationStatus.Failed
        }

        var imported = 0
        var conflict = 0
        var failed = 0

        retryable.forEach { mutation ->
            val uploading = mutationStore.updateMutation(mutation.mutationId) {
                HermesMemoryMutationReducer.markUploading(it, System.currentTimeMillis())
            } ?: mutation

            runCatching {
                client.importMemory(
                    config = config,
                    request = HermesMemoryImportRequest(
                        deviceId = uploading.deviceId,
                        mutationId = uploading.mutationId,
                        targetId = uploading.targetId,
                        baseHash = uploading.baseHash,
                        content = uploading.content,
                    )
                )
            }.onSuccess { response ->
                val updated = mutationStore.updateMutation(uploading.mutationId) {
                    HermesMemoryMutationReducer.applyResponse(it, response, System.currentTimeMillis())
                }
                when (updated?.status) {
                    HermesMemoryMutationStatus.Imported -> imported++
                    HermesMemoryMutationStatus.Conflict -> conflict++
                    else -> failed++
                }
            }.onFailure { error ->
                mutationStore.updateMutation(uploading.mutationId) {
                    HermesMemoryMutationReducer.markFailed(it, error, System.currentTimeMillis())
                }
                failed++
            }
        }

        return HermesMemoryUploadSummary(
            attempted = retryable.size,
            imported = imported,
            conflict = conflict,
            failed = failed,
        )
    }

    suspend fun clearCompleted() {
        mutationStore.clearCompleted()
    }
}

data class HermesMemoryUploadSummary(
    val attempted: Int,
    val imported: Int,
    val conflict: Int,
    val failed: Int,
)
