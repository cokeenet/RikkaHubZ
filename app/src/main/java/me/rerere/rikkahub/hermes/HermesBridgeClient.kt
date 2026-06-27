package me.rerere.rikkahub.hermes

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class HermesBridgeClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun getStatus(config: HermesBridgeConfig): HermesStatusResponse {
        return get(config, "/api/status", requiresToken = false)
    }

    suspend fun getPersonality(config: HermesBridgeConfig): HermesPersonalityResponse {
        return get(config, "/api/personality", requiresToken = true)
    }

    suspend fun getMemory(config: HermesBridgeConfig): HermesMemoryListResponse {
        return get(config, "/api/memory", requiresToken = true)
    }

    suspend fun probe(config: HermesBridgeConfig): HermesBridgeProbeResult {
        val status = getStatus(config)
        val personality = getPersonality(config)
        val memory = getMemory(config)
        return HermesBridgeProbeResult(status, personality, memory)
    }

    private inline fun <reified T> get(
        config: HermesBridgeConfig,
        path: String,
        requiresToken: Boolean,
    ): T {
        val baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            throw IOException("Hermes Bridge URL is empty.")
        }
        if (requiresToken && config.apiToken.isBlank()) {
            throw IOException("Hermes Bridge API token is empty.")
        }

        val request = Request.Builder()
            .url(baseUrl + path)
            .apply {
                if (requiresToken) {
                    header("Authorization", "Bearer ${config.apiToken}")
                }
            }
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Hermes Bridge request failed: HTTP ${response.code} ${body.take(160)}")
            }
            return json.decodeFromString(body)
        }
    }
}
