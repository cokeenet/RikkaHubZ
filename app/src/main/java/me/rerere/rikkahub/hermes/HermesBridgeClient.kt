package me.rerere.rikkahub.hermes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class HermesBridgeClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStatus(config: HermesBridgeConfig): HermesStatusResponse {
        return get(config, "/api/status", requiresToken = false)
    }

    suspend fun getPersonality(config: HermesBridgeConfig): HermesPersonalityResponse {
        return get(config, "/api/personality", requiresToken = true)
    }

    suspend fun getMemory(config: HermesBridgeConfig): HermesMemoryListResponse {
        return get(config, "/api/memory", requiresToken = true)
    }

    suspend fun getSyncStatus(config: HermesBridgeConfig): HermesSyncStatusResponse {
        return get(config, "/api/sync/status", requiresToken = true)
    }

    suspend fun runSync(config: HermesBridgeConfig): HermesSyncRunResponse {
        return post(config, "/api/sync/run", bodyJson = null, treatConflictAsBody = true)
    }

    suspend fun importMemory(
        config: HermesBridgeConfig,
        request: HermesMemoryImportRequest,
    ): HermesMemoryImportResponse {
        return post(
            config = config,
            path = "/api/memory/import",
            bodyJson = json.encodeToString(request),
            treatConflictAsBody = true,
        )
    }

    suspend fun probe(config: HermesBridgeConfig): HermesBridgeProbeResult {
        val status = getStatus(config)
        val personality = getPersonality(config)
        val memory = getMemory(config)
        return HermesBridgeProbeResult(status, personality, memory)
    }

    private suspend inline fun <reified T> get(
        config: HermesBridgeConfig,
        path: String,
        requiresToken: Boolean,
    ): T = withContext(Dispatchers.IO) {
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
            json.decodeFromString(body)
        }
    }

    private suspend inline fun <reified T> post(
        config: HermesBridgeConfig,
        path: String,
        bodyJson: String?,
        treatConflictAsBody: Boolean,
    ): T = withContext(Dispatchers.IO) {
        val baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            throw IOException("Hermes Bridge URL is empty.")
        }
        if (config.apiToken.isBlank()) {
            throw IOException("Hermes Bridge API token is empty.")
        }

        val requestBody = (bodyJson ?: "{}").toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(baseUrl + path)
            .header("Authorization", "Bearer ${config.apiToken}")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful && !(treatConflictAsBody && response.code == 409)) {
                throw IOException("Hermes Bridge request failed: HTTP ${response.code} ${body.take(160)}")
            }
            json.decodeFromString(body)
        }
    }
}
