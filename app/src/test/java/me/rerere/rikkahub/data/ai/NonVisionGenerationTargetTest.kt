package me.rerere.rikkahub.data.ai

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonVisionGenerationTargetTest {
    @Test
    fun latestMessageContainsImageParts_ignoresOlderImages() {
        val messages = listOf(
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(
                    UIMessagePart.Tool(
                        toolCallId = "call-1",
                        toolName = "take_screenshot",
                        input = "{}",
                        output = listOf(UIMessagePart.Image("file:///cache/screenshots/screen.png")),
                    )
                ),
            ),
            UIMessage.user("接下来只回答文字问题"),
        )

        assertFalse(messages.latestMessageContainsImageParts())
    }

    @Test
    fun latestMessageContainsImageParts_detectsLatestToolImage() {
        val messages = listOf(
            UIMessage.user("看一下屏幕"),
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(
                    UIMessagePart.Tool(
                        toolCallId = "call-1",
                        toolName = "take_screenshot",
                        input = "{}",
                        output = listOf(UIMessagePart.Image("file:///cache/screenshots/screen.png")),
                    )
                ),
            ),
        )

        assertTrue(messages.latestMessageContainsImageParts())
    }
}
