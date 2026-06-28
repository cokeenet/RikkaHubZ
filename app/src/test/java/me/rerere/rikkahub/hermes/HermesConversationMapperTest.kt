package me.rerere.rikkahub.hermes

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.MessageNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

class HermesConversationMapperTest {
    @Test
    fun toConversation_preservesRoleTextAndTimeline() {
        val detail = HermesConversationDetailResponse(
            conversation = HermesConversationSummary(
                id = "desktop-1",
                title = "Desktop chat",
                createdAtUtc = "2026-06-27T10:00:00Z",
                updatedAtUtc = "2026-06-27T10:02:00Z",
                messageCount = 2,
            ),
            messages = listOf(
                HermesConversationMessage(
                    id = "m1",
                    role = "user",
                    text = "hello",
                    createdAtUtc = "2026-06-27T10:00:00Z",
                ),
                HermesConversationMessage(
                    id = "m2",
                    role = "assistant",
                    text = "hi",
                    modelId = "desktop-model",
                    createdAtUtc = "2026-06-27T10:01:00Z",
                    updatedAtUtc = "2026-06-27T10:02:00Z",
                ),
            ),
        )

        val conversation = HermesConversationMapper.toConversation(detail)

        assertEquals(HERMES_ASSISTANT_ID, conversation.assistantId)
        assertEquals("Desktop chat", conversation.title)
        assertEquals(2, conversation.messageNodes.size)
        assertEquals(MessageRole.USER, conversation.currentMessages[0].role)
        assertEquals(MessageRole.ASSISTANT, conversation.currentMessages[1].role)
        assertEquals("hello", (conversation.currentMessages[0].parts.single() as UIMessagePart.Text).text)
        assertEquals("hi", (conversation.currentMessages[1].parts.single() as UIMessagePart.Text).text)
        assertEquals("2026-06-27T10:00:00Z", conversation.createAt.toString())
        assertEquals("2026-06-27T10:02:00Z", conversation.updateAt.toString())
    }

    @Test
    fun toConversation_usesStableLocalIdsForRepeatedImport() {
        val detail = HermesConversationDetailResponse(
            conversation = HermesConversationSummary(
                id = "same-desktop-id",
                title = "Repeated",
                createdAtUtc = "2026-06-27T10:00:00Z",
                updatedAtUtc = "2026-06-27T10:00:00Z",
            ),
            messages = listOf(
                HermesConversationMessage(
                    id = "m1",
                    role = "user",
                    text = "same",
                    createdAtUtc = "2026-06-27T10:00:00Z",
                )
            ),
        )

        val first = HermesConversationMapper.toConversation(detail)
        val second = HermesConversationMapper.toConversation(detail)

        assertEquals(first.id, second.id)
        assertEquals(first.messageNodes.single().id, second.messageNodes.single().id)
        assertEquals(first.currentMessages.single().id, second.currentMessages.single().id)
    }

    @Test
    fun toConversation_keepsDuplicateDesktopMessageIdsUniqueByIndex() {
        val detail = HermesConversationDetailResponse(
            conversation = HermesConversationSummary(
                id = "duplicate-message-id",
                title = "Duplicate",
                createdAtUtc = "2026-06-27T10:00:00Z",
                updatedAtUtc = "2026-06-27T10:01:00Z",
            ),
            messages = listOf(
                HermesConversationMessage(
                    id = "same",
                    role = "user",
                    text = "first",
                    createdAtUtc = "2026-06-27T10:00:00Z",
                ),
                HermesConversationMessage(
                    id = "same",
                    role = "assistant",
                    text = "second",
                    createdAtUtc = "2026-06-27T10:01:00Z",
                ),
            ),
        )

        val conversation = HermesConversationMapper.toConversation(detail)

        assertEquals(2, conversation.messageNodes.map { it.id }.toSet().size)
        assertEquals(2, conversation.currentMessages.map { it.id }.toSet().size)
    }

    @Test
    fun mergeImportedConversation_preservesLocalContinuationNodes() {
        val imported = HermesConversationMapper.toConversation(
            HermesConversationDetailResponse(
                conversation = HermesConversationSummary(
                    id = "continued-desktop-chat",
                    title = "Desktop",
                    createdAtUtc = "2026-06-27T10:00:00Z",
                    updatedAtUtc = "2026-06-27T10:01:00Z",
                ),
                messages = listOf(
                    HermesConversationMessage(
                        id = "m1",
                        role = "user",
                        text = "from desktop",
                        createdAtUtc = "2026-06-27T10:00:00Z",
                    ),
                ),
            )
        )
        val localNode = MessageNode(
            id = Uuid.random(),
            messages = listOf(
                UIMessage(
                    role = MessageRole.USER,
                    parts = listOf(UIMessagePart.Text("from phone")),
                )
            ),
        )
        val existing = imported.copy(
            messageNodes = imported.messageNodes + localNode,
            isPinned = true,
        )

        val merged = HermesConversationMapper.mergeImportedConversation(imported, existing)

        assertEquals(imported.messageNodes.size + 1, merged.messageNodes.size)
        assertEquals(localNode.id, merged.messageNodes.last().id)
        assertTrue(merged.isPinned)
    }

    @Test
    fun parseRole_rejectsUnknownRole() {
        assertEquals(MessageRole.SYSTEM, HermesConversationMapper.parseRole("system"))
        assertEquals(MessageRole.USER, HermesConversationMapper.parseRole("user"))
        assertEquals(MessageRole.ASSISTANT, HermesConversationMapper.parseRole("assistant"))
        assertEquals(MessageRole.TOOL, HermesConversationMapper.parseRole("tool"))
        assertTrue(HermesConversationMapper.parseRole("alien") == null)
    }
}
