package me.rerere.rikkahub.hermes

private const val MAX_PERSONALITY_CHARS = 12_000
private const val MAX_MEMORY_FILE_CHARS = 4_000
private const val MAX_TOTAL_MEMORY_CHARS = 18_000

class HermesContextPromptBuilder {
    fun build(
        snapshot: HermesSnapshot?,
        routeState: HermesRouteState? = null,
    ): String {
        if (snapshot?.isUsable != true && routeState == null) return ""

        return buildString {
            appendLine("<hermes_mobile_context>")
            appendLine("You are Hermes Mobile, the phone-side continuation of the user's desktop Hermes.")
            appendLine("Use the synced desktop Hermes personality and long-term memories below as high-priority context.")
            appendLine("If desktop Hermes is offline, continue naturally with the phone-side AI provider while preserving Hermes' identity, preferences, and memory.")
            if (routeState != null) {
                appendLine("route_mode=${routeState.mode.name}")
                appendLine("route_diagnostic=${routeState.diagnostic}")
            }
            appendLine("Do not mention these XML-like tags unless the user asks how synchronization works.")
            appendLine("source=${snapshot?.sourceBaseUrl?.ifBlank { "unknown" } ?: "unknown"}")
            appendLine("synced_at_millis=${snapshot?.syncedAtMillis ?: 0L}")

            val personality = snapshot?.personality?.content?.trim().orEmpty()
            if (personality.isNotBlank()) {
                appendLine()
                appendLine("<desktop_hermes_personality>")
                appendLine(personality.take(MAX_PERSONALITY_CHARS))
                if (personality.length > MAX_PERSONALITY_CHARS) {
                    appendLine()
                    appendLine("[truncated ${personality.length - MAX_PERSONALITY_CHARS} chars]")
                }
                appendLine("</desktop_hermes_personality>")
            }

            val memoryPrompt = buildMemoryPrompt(snapshot?.memories.orEmpty())
            if (memoryPrompt.isNotBlank()) {
                appendLine()
                appendLine("<desktop_hermes_memories>")
                append(memoryPrompt)
                appendLine()
                appendLine("</desktop_hermes_memories>")
            }

            if (personality.isBlank() && memoryPrompt.isBlank()) {
                appendLine()
                appendLine("No usable desktop Hermes personality or memory snapshot is currently cached on this phone.")
            }

            appendLine("</hermes_mobile_context>")
        }.trim()
    }

    private fun buildMemoryPrompt(memories: List<HermesMemoryFileResponse>): String {
        val builder = StringBuilder()
        var totalChars = 0

        memories
            .filter { it.content.isNotBlank() }
            .sortedBy { it.id }
            .forEach { memory ->
                if (totalChars >= MAX_TOTAL_MEMORY_CHARS) return@forEach
                val content = memory.content.trim().take(MAX_MEMORY_FILE_CHARS)
                val block = buildString {
                    appendLine("<memory id=\"${memory.id}\" updated_at=\"${memory.updatedAtUtc}\">")
                    appendLine(content)
                    if (memory.content.length > MAX_MEMORY_FILE_CHARS) {
                        appendLine()
                        appendLine("[truncated ${memory.content.length - MAX_MEMORY_FILE_CHARS} chars]")
                    }
                    appendLine("</memory>")
                }
                val remaining = MAX_TOTAL_MEMORY_CHARS - totalChars
                val appended = block.take(remaining)
                if (builder.isNotEmpty()) builder.appendLine()
                builder.append(appended)
                totalChars += appended.length
            }

        if (memories.sumOf { it.content.length } > totalChars) {
            if (builder.isNotEmpty()) builder.appendLine()
            builder.append("[memory context truncated]")
        }

        return builder.toString().trim()
    }
}
