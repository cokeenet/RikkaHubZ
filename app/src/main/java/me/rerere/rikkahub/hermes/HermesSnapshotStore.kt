package me.rerere.rikkahub.hermes

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.hermesSnapshotDataStore by preferencesDataStore(name = "hermes_snapshot")

class HermesSnapshotStore(
    private val context: Context,
    private val json: Json,
) {
    private val store = context.hermesSnapshotDataStore
    private val K_SNAPSHOT_JSON = stringPreferencesKey("snapshot_json")

    val flow = store.data.map { prefs ->
        prefs[K_SNAPSHOT_JSON]?.let(::decodeSnapshot)
    }

    suspend fun current(): HermesSnapshot? = flow.first()

    suspend fun save(snapshot: HermesSnapshot) {
        store.edit { prefs ->
            prefs[K_SNAPSHOT_JSON] = json.encodeToString(snapshot)
        }
    }

    suspend fun clear() {
        store.edit { prefs ->
            prefs.remove(K_SNAPSHOT_JSON)
        }
    }

    private fun decodeSnapshot(raw: String): HermesSnapshot? {
        return try {
            json.decodeFromString<HermesSnapshot>(raw)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
