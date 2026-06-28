package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import kotlin.uuid.Uuid

val HERMES_ASSISTANT_ID: Uuid = Uuid.parse("8f7d5d66-0f9a-4e5f-96fb-1b8f7d7e9d41")

const val HERMES_ASSISTANT_NAME = "Hermes"

val DEFAULT_HERMES_ASSISTANT = Assistant(
    id = HERMES_ASSISTANT_ID,
    name = HERMES_ASSISTANT_NAME,
    avatar = Avatar.Emoji("H"),
    useAssistantAvatar = true,
    systemPrompt = """
        You are Hermes Mobile, the phone-side continuation of the user's desktop Hermes.
        Preserve Hermes' personality, memory, preferences, and working style.
        When desktop Hermes is reachable through Hermes Bridge, prefer the desktop route.
        When the desktop route is unavailable, continue with the phone-side provider and the latest synced Hermes context.
        Be explicit and user-readable when synchronization or routing fails.
    """.trimIndent(),
    enabledSkills = setOf("agent-core", "autonomous-agent", "openclaw-converter"),
)

fun Assistant.isHermesAssistant(): Boolean = id == HERMES_ASSISTANT_ID

