package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.data.repository.ConversationRepository

class HermesConversationSyncRepository(
    private val syncRepository: HermesSyncRepository,
    private val client: HermesBridgeClient,
    private val conversationRepository: ConversationRepository,
) {
    suspend fun listDesktopConversations(): HermesConversationListResponse {
        val config = syncRepository.currentBridgeConfig()
        return client.getConversations(config)
    }

    suspend fun importConversation(id: String): HermesConversationImportResult {
        val config = syncRepository.currentBridgeConfig()
        val detail = client.getConversation(config, id)
        val importedConversation = HermesConversationMapper.toConversation(detail)
        val existingConversation = conversationRepository.getConversationById(importedConversation.id)
        val conversation = existingConversation?.let { existing ->
            HermesConversationMapper.mergeImportedConversation(importedConversation, existing)
        } ?: importedConversation
        if (existingConversation != null) {
            conversationRepository.updateConversation(conversation)
        } else {
            conversationRepository.insertConversation(conversation)
        }
        return HermesConversationImportResult(
            conversationId = conversation.id.toString(),
            desktopConversationId = detail.conversation.id,
            title = conversation.title,
            messageCount = conversation.messageNodes.size,
            updated = existingConversation != null,
        )
    }

    suspend fun importRecent(limit: Int = Int.MAX_VALUE): HermesConversationImportSummary {
        val list = listDesktopConversations()
        val imported = mutableListOf<HermesConversationImportResult>()
        list.conversations.take(limit).forEach { summary ->
            imported += importConversation(summary.id)
        }
        return HermesConversationImportSummary(
            listed = list.conversations.size,
            imported = imported,
            cursor = list.cursor,
            source = list.source,
        )
    }
}

data class HermesConversationImportResult(
    val conversationId: String,
    val desktopConversationId: String,
    val title: String,
    val messageCount: Int,
    val updated: Boolean,
)

data class HermesConversationImportSummary(
    val listed: Int,
    val imported: List<HermesConversationImportResult>,
    val cursor: String,
    val source: String,
) {
    val importedCount: Int get() = imported.size
    val updatedCount: Int get() = imported.count { it.updated }
    val insertedCount: Int get() = imported.count { !it.updated }
}
