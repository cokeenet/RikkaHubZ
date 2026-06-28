package me.rerere.rikkahub.hermes

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

private val Context.hermesDeviceDataStore by preferencesDataStore(name = "hermes_device")

class HermesDeviceStore(private val context: Context) {
    private val store = context.hermesDeviceDataStore
    private val K_DEVICE_ID = stringPreferencesKey("device_id")

    val deviceIdFlow = store.data.map { prefs ->
        prefs[K_DEVICE_ID].orEmpty()
    }

    suspend fun getOrCreateDeviceId(): String {
        val current = deviceIdFlow.first()
        if (current.isNotBlank()) {
            return current
        }

        val generated = "android-${Uuid.random()}"
        store.edit { prefs ->
            if (prefs[K_DEVICE_ID].isNullOrBlank()) {
                prefs[K_DEVICE_ID] = generated
            }
        }

        return deviceIdFlow.first().ifBlank { generated }
    }
}
