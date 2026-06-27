package me.rerere.rikkahub.hermes

data class HermesBridgeConfig(
    val baseUrl: String = "http://127.0.0.1:3001",
    val apiToken: String = "",
    val fallbackProviderId: String = "",
    val lastStatusAt: Long? = null,
    val lastPersonalityAt: Long? = null,
    val lastMemoryAt: Long? = null,
)
