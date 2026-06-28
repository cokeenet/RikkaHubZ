package me.rerere.rikkahub.hermes

import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.Assistant

class HermesChatRouter(
    private val client: HermesBridgeClient,
    private val syncRepository: HermesSyncRepository,
    private val routeResolver: HermesRouteResolver,
) {
    suspend fun tryDesktopReply(
        conversationId: String,
        assistant: Assistant,
        model: Model,
        messages: List<UIMessage>,
    ): HermesDesktopReplyResult {
        val config = syncRepository.currentBridgeConfig()
        val snapshot = syncRepository.currentSnapshot()
        val routeState = routeResolver.resolve(
            assistant = assistant,
            bridgeConfig = config,
            snapshot = snapshot,
        )
        if (routeState.mode != HermesRouteMode.BridgePreferred) {
            return HermesDesktopReplyResult.Skipped(routeState.diagnostic)
        }

        val textMessages = messages.mapNotNull { message ->
            val text = message.parts
                .filterIsInstance<UIMessagePart.Text>()
                .joinToString("\n") { it.text }
                .trim()
            if (text.isBlank()) {
                null
            } else {
                HermesChatMessage(role = message.role, text = text)
            }
        }
        if (textMessages.isEmpty()) {
            return HermesDesktopReplyResult.Skipped("Hermes Bridge chat route skipped because this turn has no text message.")
        }

        return runCatching {
            client.chat(
                config = config,
                request = HermesChatRequest(
                    conversationId = conversationId,
                    assistantId = assistant.id.toString(),
                    modelId = model.modelId,
                    messages = textMessages,
                    routeDiagnostic = routeState.diagnostic,
                )
            )
        }.fold(
            onSuccess = { response ->
                if (response.success && response.reply.isNotBlank()) {
                    HermesDesktopReplyResult.Replied(
                        text = response.reply,
                        source = response.source.ifBlank { "Hermes Bridge" },
                        modelId = response.modelId,
                    )
                } else {
                    HermesDesktopReplyResult.Failed(
                        reason = response.error?.takeIf { it.isNotBlank() }
                            ?: "Hermes Bridge returned an empty chat reply.",
                    )
                }
            },
            onFailure = { error ->
                HermesDesktopReplyResult.Failed(
                    reason = error.message ?: error::class.java.simpleName,
                )
            },
        )
    }
}

sealed interface HermesDesktopReplyResult {
    data class Replied(
        val text: String,
        val source: String,
        val modelId: String,
    ) : HermesDesktopReplyResult

    data class Skipped(val reason: String) : HermesDesktopReplyResult

    data class Failed(val reason: String) : HermesDesktopReplyResult
}
