package me.rerere.rikkahub.hermes

class HermesSyncRepository(
    private val preferences: HermesBridgePreferences,
    private val client: HermesBridgeClient,
    private val snapshotStore: HermesSnapshotStore,
) {
    val snapshotFlow = snapshotStore.flow

    suspend fun currentSnapshot(): HermesSnapshot? = snapshotStore.current()

    suspend fun syncNow(): HermesSnapshot {
        val config = preferences.current()
        val result = client.probe(config)
        val now = System.currentTimeMillis()
        val snapshot = HermesSnapshot(
            syncedAtMillis = now,
            sourceBaseUrl = config.baseUrl.trim().trimEnd('/'),
            service = result.status.service,
            status = result.status.status,
            dataRoot = result.status.dataRoot,
            personality = result.personality,
            memories = result.memory.memories,
        )
        snapshotStore.save(snapshot)
        preferences.update {
            it.copy(
                lastStatusAt = now,
                lastPersonalityAt = now,
                lastMemoryAt = now,
            )
        }
        return snapshot
    }

    suspend fun clearSnapshot() {
        snapshotStore.clear()
    }
}
