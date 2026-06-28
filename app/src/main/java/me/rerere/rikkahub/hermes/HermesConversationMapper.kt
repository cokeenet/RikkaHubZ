package me.rerere.rikkahub.hermes

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.MessageNode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.Uuid

object HermesConversationMapper {
    fun toConversation(detail: HermesConversationDetailResponse): Conversation {
        val summary = detail.conversation
        val messages = detail.messages.filter { it.text.isNotBlank() }
        val nodes = messages.mapIndexed { index, message ->
            MessageNode(
                id = stableUuid("hermes-node:${summary.id}:${message.stableSeed(index)}"),
                messages = listOf(message.toUiMessage(summary.id, index)),
                selectIndex = 0,
            )
        }
        val fallbackNow = Instant.now()
        return Conversation(
            id = stableUuid("hermes-conversation:${summary.id}"),
            assistantId = HERMES_ASSISTANT_ID,
            title = summary.title.ifBlank { summary.id.ifBlank { "Hermes conversation" } },
            messageNodes = nodes,
            chatSuggestions = emptyList(),
            isPinned = false,
            createAt = parseInstant(summary.createdAtUtc) ?: messages.firstOrNull()?.createdInstant() ?: fallbackNow,
            updateAt = parseInstant(summary.updatedAtUtc) ?: messages.lastOrNull()?.updatedInstant() ?: fallbackNow,
            customSystemPrompt = null,
            modeInjectionIds = emptySet(),
            lorebookIds = emptySet(),
        )
    }

    fun mergeImportedConversation(imported: Conversation, existing: Conversation): Conversation {
        val importedNodeIds = imported.messageNodes.mapTo(mutableSetOf()) { it.id }
        val localOnlyNodes = existing.messageNodes.filterNot { it.id in importedNodeIds }
        return imported.copy(
            messageNodes = imported.messageNodes + localOnlyNodes,
            chatSuggestions = existing.chatSuggestions,
            isPinned = existing.isPinned,
            updateAt = maxOf(imported.updateAt, existing.updateAt),
            customSystemPrompt = existing.customSystemPrompt,
            modeInjectionIds = existing.modeInjectionIds,
            lorebookIds = existing.lorebookIds,
            workspaceCwd = existing.workspaceCwd,
        )
    }

    fun parseRole(role: String): MessageRole? {
        return when (role.trim().lowercase()) {
            "system" -> MessageRole.SYSTEM
            "user" -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "tool" -> MessageRole.TOOL
            else -> null
        }
    }

    private fun HermesConversationMessage.toUiMessage(conversationId: String, index: Int): UIMessage {
        val created = createdInstant() ?: Instant.now()
        val finished = updatedInstant()
        return UIMessage(
            id = stableUuid("hermes-message:$conversationId:${stableSeed(index)}"),
            role = parseRole(role) ?: MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text(text)),
            createdAt = created.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()),
            finishedAt = finished?.toKotlinInstant()?.toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun HermesConversationMessage.createdInstant(): Instant? = parseInstant(createdAtUtc)

    private fun HermesConversationMessage.updatedInstant(): Instant? = parseInstant(updatedAtUtc)

    private fun HermesConversationMessage.stableSeed(index: Int): String {
        return if (id.isNotBlank()) {
            "$index:$id"
        } else {
            "$index:${createdAtUtc}:${role}:${text.hashCode()}"
        }
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
    }

    private fun stableUuid(seed: String): Uuid {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(StandardCharsets.UTF_8))
            .copyOfRange(0, 16)
        digest[6] = ((digest[6].toInt() and 0x0f) or 0x50).toByte()
        digest[8] = ((digest[8].toInt() and 0x3f) or 0x80).toByte()
        return Uuid.fromByteArray(digest)
    }
}
