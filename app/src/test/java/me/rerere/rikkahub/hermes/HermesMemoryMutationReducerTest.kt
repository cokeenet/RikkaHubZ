package me.rerere.rikkahub.hermes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesMemoryMutationReducerTest {
    private val mutation = HermesMemoryMutation(
        deviceId = "android-device",
        mutationId = "mutation-1",
        baseHash = "old-hash",
        targetId = "mobile.md",
        content = "用户喜欢 C#。",
        createdAtMillis = 100L,
        updatedAtMillis = 100L,
    )

    @Test
    fun markUploading_clearsPreviousError() {
        val failed = mutation.copy(
            status = HermesMemoryMutationStatus.Failed,
            lastError = "timeout",
        )

        val uploading = HermesMemoryMutationReducer.markUploading(failed, nowMillis = 200L)

        assertEquals(HermesMemoryMutationStatus.Uploading, uploading.status)
        assertEquals(200L, uploading.updatedAtMillis)
        assertNull(uploading.lastError)
    }

    @Test
    fun applyResponse_marksImportedIdempotentResultAsImported() {
        val imported = HermesMemoryMutationReducer.applyResponse(
            mutation = mutation,
            response = HermesMemoryImportResponse(
                deviceId = "android-device",
                mutationId = "mutation-1",
                targetId = "mobile.md",
                status = "imported",
                alreadyProcessed = true,
                importedAtUtc = "2026-06-28T00:00:00Z",
                contentHash = "new-hash",
                previousHash = "old-hash",
                message = "Memory imported.",
            ),
            nowMillis = 300L,
        )

        assertEquals(HermesMemoryMutationStatus.Imported, imported.status)
        assertTrue(imported.alreadyProcessed)
        assertEquals("new-hash", imported.contentHash)
        assertNull(imported.lastError)
    }

    @Test
    fun applyResponse_marksConflictWithoutPretendingSuccess() {
        val conflict = HermesMemoryMutationReducer.applyResponse(
            mutation = mutation,
            response = HermesMemoryImportResponse(
                deviceId = "android-device",
                mutationId = "mutation-1",
                targetId = "mobile.md",
                status = "conflict",
                alreadyProcessed = false,
                importedAtUtc = "2026-06-28T00:01:00Z",
                contentHash = "incoming-hash",
                previousHash = "desktop-hash",
                message = "Base hash does not match current memory content.",
            ),
            nowMillis = 400L,
        )

        assertEquals(HermesMemoryMutationStatus.Conflict, conflict.status)
        assertEquals("desktop-hash", conflict.previousHash)
        assertEquals("Base hash does not match current memory content.", conflict.lastError)
    }

    @Test
    fun summary_countsRetryableAndLatestError() {
        val summary = HermesMemoryQueueSummary.from(
            listOf(
                mutation,
                mutation.copy(
                    mutationId = "mutation-2",
                    status = HermesMemoryMutationStatus.Failed,
                    lastError = "network timeout",
                ),
                mutation.copy(
                    mutationId = "mutation-3",
                    status = HermesMemoryMutationStatus.Imported,
                    lastError = null,
                ),
            )
        )

        assertEquals(3, summary.total)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.imported)
        assertTrue(summary.hasRetryable)
        assertEquals("network timeout", summary.latestError)
    }
}
