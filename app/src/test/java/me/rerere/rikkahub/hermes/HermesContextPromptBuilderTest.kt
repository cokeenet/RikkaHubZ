package me.rerere.rikkahub.hermes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HermesContextPromptBuilderTest {
    private val builder = HermesContextPromptBuilder()

    @Test
    fun build_returnsBlankWhenSnapshotMissing() {
        assertTrue(builder.build(null).isBlank())
    }

    @Test
    fun build_includesPersonalityAndMemories() {
        val prompt = builder.build(
            snapshot = HermesSnapshot(
                syncedAtMillis = 123L,
                sourceBaseUrl = "http://desktop:3001",
                personality = HermesPersonalityResponse(
                    exists = true,
                    content = "你是电脑 Hermes 的延续。",
                ),
                memories = listOf(
                    HermesMemoryFileResponse(
                        id = "daily.md",
                        content = "用户喜欢 C# 和 NativeAOT。",
                        updatedAtUtc = "2026-06-28T00:00:00Z",
                    )
                ),
            ),
            routeState = HermesRouteState(
                mode = HermesRouteMode.PhoneFallback,
                diagnostic = "phone provider will use cached context",
            ),
        )

        assertTrue(prompt.contains("<hermes_mobile_context>"))
        assertTrue(prompt.contains("route_mode=PhoneFallback"))
        assertTrue(prompt.contains("phone provider will use cached context"))
        assertTrue(prompt.contains("你是电脑 Hermes 的延续。"))
        assertTrue(prompt.contains("用户喜欢 C# 和 NativeAOT。"))
        assertTrue(prompt.contains("source=http://desktop:3001"))
    }

    @Test
    fun build_omitsEmptySnapshot() {
        val prompt = builder.build(HermesSnapshot())

        assertFalse(prompt.contains("<hermes_mobile_context>"))
        assertTrue(prompt.isBlank())
    }

    @Test
    fun build_includesRouteDiagnosticWhenSnapshotIsEmpty() {
        val prompt = builder.build(
            snapshot = null,
            routeState = HermesRouteState(
                mode = HermesRouteMode.PhoneFallback,
                diagnostic = "no usable snapshot",
            ),
        )

        assertTrue(prompt.contains("<hermes_mobile_context>"))
        assertTrue(prompt.contains("route_mode=PhoneFallback"))
        assertTrue(prompt.contains("no usable snapshot"))
        assertTrue(prompt.contains("No usable desktop Hermes personality or memory snapshot"))
    }
}
