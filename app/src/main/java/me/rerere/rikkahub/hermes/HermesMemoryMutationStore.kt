package me.rerere.rikkahub.hermes

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.hermesMemoryQueueDataStore by preferencesDataStore(name = "hermes_memory_queue")

class HermesMemoryMutationStore(
    private val context: Context,
    private val json: Json,
) {
    private val store = context.hermesMemoryQueueDataStore
    private val K_QUEUE_JSON = stringPreferencesKey("queue_json")

    val flow = store.data.map { prefs ->
        decodeQueue(prefs[K_QUEUE_JSON])
    }

    suspend fun current(): HermesMemoryMutationQueue = flow.first()

    suspend fun add(mutation: HermesMemoryMutation) {
        updateQueue { queue ->
            val withoutDuplicate = queue.mutations.filterNot { it.mutationId == mutation.mutationId }
            queue.copy(mutations = withoutDuplicate + mutation)
        }
    }

    suspend fun updateMutation(
        mutationId: String,
        update: (HermesMemoryMutation) -> HermesMemoryMutation,
    ): HermesMemoryMutation? {
        var updated: HermesMemoryMutation? = null
        updateQueue { queue ->
            queue.copy(
                mutations = queue.mutations.map { mutation ->
                    if (mutation.mutationId == mutationId) {
                        update(mutation).also { updated = it }
                    } else {
                        mutation
                    }
                }
            )
        }
        return updated
    }

    suspend fun clearCompleted() {
        updateQueue { queue ->
            queue.copy(
                mutations = queue.mutations.filterNot {
                    it.status == HermesMemoryMutationStatus.Imported
                }
            )
        }
    }

    private suspend fun updateQueue(update: (HermesMemoryMutationQueue) -> HermesMemoryMutationQueue) {
        store.edit { prefs ->
            val current = decodeQueue(prefs[K_QUEUE_JSON])
            prefs[K_QUEUE_JSON] = json.encodeToString(update(current))
        }
    }

    private fun decodeQueue(raw: String?): HermesMemoryMutationQueue {
        if (raw.isNullOrBlank()) {
            return HermesMemoryMutationQueue()
        }

        return try {
            json.decodeFromString<HermesMemoryMutationQueue>(raw)
        } catch (_: SerializationException) {
            HermesMemoryMutationQueue()
        } catch (_: IllegalArgumentException) {
            HermesMemoryMutationQueue()
        }
    }
}
