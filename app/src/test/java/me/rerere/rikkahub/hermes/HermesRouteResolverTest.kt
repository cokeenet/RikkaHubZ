package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.data.model.Assistant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesRouteResolverTest {
    private val resolver = HermesRouteResolver()

    @Test
    fun resolve_returnsNotHermesForRegularAssistant() {
        val state = resolver.resolve(
            assistant = Assistant(),
            bridgeConfig = HermesBridgeConfig(baseUrl = "http://desktop:3001", apiToken = "secret"),
            snapshot = usableSnapshot(),
        )

        assertEquals(HermesRouteMode.NotHermes, state.mode)
        assertFalse(state.shouldInjectMobileContext)
    }

    @Test
    fun resolve_prefersBridgeWhenHermesBridgeIsConfigured() {
        val state = resolver.resolve(
            assistant = DEFAULT_HERMES_ASSISTANT,
            bridgeConfig = HermesBridgeConfig(baseUrl = "http://desktop:3001", apiToken = "secret"),
            snapshot = usableSnapshot(),
        )

        assertEquals(HermesRouteMode.BridgePreferred, state.mode)
        assertTrue(state.shouldInjectMobileContext)
        assertTrue(state.diagnostic.contains("desktop route"))
    }

    @Test
    fun resolve_usesPhoneFallbackWhenTokenMissing() {
        val state = resolver.resolve(
            assistant = DEFAULT_HERMES_ASSISTANT,
            bridgeConfig = HermesBridgeConfig(baseUrl = "http://desktop:3001", apiToken = ""),
            snapshot = usableSnapshot(),
        )

        assertEquals(HermesRouteMode.PhoneFallback, state.mode)
        assertTrue(state.shouldInjectMobileContext)
        assertTrue(state.diagnostic.contains("phone provider"))
    }

    private fun usableSnapshot(): HermesSnapshot = HermesSnapshot(
        personality = HermesPersonalityResponse(
            exists = true,
            content = "Hermes personality",
        ),
    )
}

