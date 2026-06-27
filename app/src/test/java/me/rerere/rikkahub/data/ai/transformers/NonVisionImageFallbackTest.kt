package me.rerere.rikkahub.data.ai.transformers

import kotlinx.coroutines.runBlocking
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonVisionImageFallbackTest {
    @Test
    fun containsImage_detectsToolOutputImage() {
        val part = UIMessagePart.Tool(
            toolCallId = "call-1",
            toolName = "take_screenshot",
            input = "{}",
            output = listOf(
                UIMessagePart.Image("file:///cache/screenshots/screen.png"),
                UIMessagePart.Text("{\"success\":true}"),
            ),
        )

        assertTrue(part.containsImage())
    }

    @Test
    fun replaceImagesForTextModel_replacesNestedToolImages() = runBlocking {
        val messages = listOf(
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(
                    UIMessagePart.Tool(
                        toolCallId = "call-1",
                        toolName = "take_screenshot",
                        input = "{}",
                        output = listOf(
                            UIMessagePart.Image("file:///cache/screenshots/screen.png"),
                            UIMessagePart.Text("{\"success\":true}"),
                        ),
                    )
                ),
            )
        )

        val replaced = messages.replaceImagesForTextModel { image ->
            UIMessagePart.Text("[described ${image.url}]")
        }
        val tool = replaced.single().parts.single() as UIMessagePart.Tool

        assertEquals("[described file:///cache/screenshots/screen.png]", (tool.output[0] as UIMessagePart.Text).text)
        assertEquals("{\"success\":true}", (tool.output[1] as UIMessagePart.Text).text)
        assertFalse(tool.output.any { it is UIMessagePart.Image })
    }
}
