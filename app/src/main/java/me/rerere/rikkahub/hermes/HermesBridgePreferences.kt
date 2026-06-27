package me.rerere.rikkahub.hermes

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.hermesBridgeDataStore by preferencesDataStore(name = "hermes_bridge")

class HermesBridgePreferences(private val context: Context) {
    private val store = context.hermesBridgeDataStore

    private val K_BASE_URL = stringPreferencesKey("base_url")
    private val K_API_TOKEN = stringPreferencesKey("api_token")
    private val K_FALLBACK_PROVIDER_ID = stringPreferencesKey("fallback_provider_id")
    private val K_LAST_STATUS_AT = longPreferencesKey("last_status_at")
    private val K_LAST_PERSONALITY_AT = longPreferencesKey("last_personality_at")
    private val K_LAST_MEMORY_AT = longPreferencesKey("last_memory_at")

    val flow = store.data.map { p ->
        HermesBridgeConfig(
            baseUrl = p[K_BASE_URL]?.ifBlank { DEFAULT_BASE_URL } ?: DEFAULT_BASE_URL,
            apiToken = p[K_API_TOKEN].orEmpty(),
            fallbackProviderId = p[K_FALLBACK_PROVIDER_ID].orEmpty(),
            lastStatusAt = p[K_LAST_STATUS_AT],
            lastPersonalityAt = p[K_LAST_PERSONALITY_AT],
            lastMemoryAt = p[K_LAST_MEMORY_AT],
        )
    }

    suspend fun current(): HermesBridgeConfig = flow.first()

    suspend fun update(fn: (HermesBridgeConfig) -> HermesBridgeConfig) {
        store.edit { p ->
            val next = fn(
                HermesBridgeConfig(
                    baseUrl = p[K_BASE_URL]?.ifBlank { DEFAULT_BASE_URL } ?: DEFAULT_BASE_URL,
                    apiToken = p[K_API_TOKEN].orEmpty(),
                    fallbackProviderId = p[K_FALLBACK_PROVIDER_ID].orEmpty(),
                    lastStatusAt = p[K_LAST_STATUS_AT],
                    lastPersonalityAt = p[K_LAST_PERSONALITY_AT],
                    lastMemoryAt = p[K_LAST_MEMORY_AT],
                )
            )
            p[K_BASE_URL] = next.baseUrl.trim().trimEnd('/')
            p[K_API_TOKEN] = next.apiToken.trim()
            p[K_FALLBACK_PROVIDER_ID] = next.fallbackProviderId.trim()
            next.lastStatusAt?.let { p[K_LAST_STATUS_AT] = it } ?: p.remove(K_LAST_STATUS_AT)
            next.lastPersonalityAt?.let { p[K_LAST_PERSONALITY_AT] = it } ?: p.remove(K_LAST_PERSONALITY_AT)
            next.lastMemoryAt?.let { p[K_LAST_MEMORY_AT] = it } ?: p.remove(K_LAST_MEMORY_AT)
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL = "http://127.0.0.1:3001"
    }
}
