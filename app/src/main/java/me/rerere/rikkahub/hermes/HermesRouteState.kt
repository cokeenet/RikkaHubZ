package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.data.model.Assistant

enum class HermesRouteMode {
    NotHermes,
    BridgePreferred,
    PhoneFallback,
}

data class HermesRouteState(
    val mode: HermesRouteMode,
    val diagnostic: String,
) {
    val shouldInjectMobileContext: Boolean
        get() = mode == HermesRouteMode.BridgePreferred || mode == HermesRouteMode.PhoneFallback

    companion object {
        val NotHermes = HermesRouteState(
            mode = HermesRouteMode.NotHermes,
            diagnostic = "Current assistant is not Hermes.",
        )
    }
}

class HermesRouteResolver {
    fun resolve(
        assistant: Assistant,
        bridgeConfig: HermesBridgeConfig,
        snapshot: HermesSnapshot?,
    ): HermesRouteState {
        if (!assistant.isHermesAssistant()) {
            return HermesRouteState.NotHermes
        }

        val hasBridgeConfig = bridgeConfig.baseUrl.isNotBlank() && bridgeConfig.apiToken.isNotBlank()
        if (hasBridgeConfig) {
            return HermesRouteState(
                mode = HermesRouteMode.BridgePreferred,
                diagnostic = "Hermes Bridge configured; desktop route is preferred when chat proxy is available.",
            )
        }

        val fallbackReason = if (snapshot?.isUsable == true) {
            "Hermes Bridge is not fully configured; phone provider will use the latest synced Hermes context."
        } else {
            "Hermes Bridge is not fully configured and no usable Hermes snapshot is cached."
        }
        return HermesRouteState(
            mode = HermesRouteMode.PhoneFallback,
            diagnostic = fallbackReason,
        )
    }
}

