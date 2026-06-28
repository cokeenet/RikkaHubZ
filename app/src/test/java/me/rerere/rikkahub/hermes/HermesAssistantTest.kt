package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.data.datastore.DEFAULT_ASSISTANTS
import me.rerere.rikkahub.data.datastore.DEFAULT_ASSISTANTS_IDS
import me.rerere.rikkahub.data.model.Assistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesAssistantTest {
    @Test
    fun defaultAssistants_includeStableHermesAssistant() {
        val hermes = DEFAULT_ASSISTANTS.first { it.id == HERMES_ASSISTANT_ID }

        assertEquals(HERMES_ASSISTANT_NAME, hermes.name)
        assertTrue(hermes.useAssistantAvatar)
        assertTrue(hermes.systemPrompt.contains("Hermes Mobile"))
        assertTrue(DEFAULT_ASSISTANTS_IDS.contains(HERMES_ASSISTANT_ID))
    }

    @Test
    fun isHermesAssistant_usesStableIdOnly() {
        assertTrue(DEFAULT_HERMES_ASSISTANT.isHermesAssistant())
        assertTrue(!Assistant(name = HERMES_ASSISTANT_NAME).isHermesAssistant())
    }
}

