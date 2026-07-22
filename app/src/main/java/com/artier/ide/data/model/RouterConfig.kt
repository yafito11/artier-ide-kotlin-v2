package com.artier.ide.data.model

data class RouterConfig(
    val port: Int = 20128,
    val host: String = "127.0.0.1",
    val providers: List<ProviderConfig> = emptyList(),
    val quotas: Map<String, QuotaConfig> = emptyMap()
)

data class ProviderConfig(
    val name: String,
    val type: ProviderType,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val models: List<String> = emptyList(),
    val priority: Int = 0,
    val isEnabled: Boolean = true
)

enum class ProviderType {
    OpenAI,
    Claude,
    Gemini,
    Custom
}

data class QuotaConfig(
    val daily: Long = 100000,
    val monthly: Long = 2000000
)

data class QuotaUsage(
    val model: String,
    val dailyUsed: Long = 0,
    val dailyLimit: Long = 100000,
    val monthlyUsed: Long = 0,
    val monthlyLimit: Long = 2000000
) {
    val dailyRemaining: Long
        get() = dailyLimit - dailyUsed

    val monthlyRemaining: Long
        get() = monthlyLimit - monthlyUsed

    val dailyPercentage: Float
        get() = if (dailyLimit > 0) dailyUsed.toFloat() / dailyLimit else 0f

    val monthlyPercentage: Float
        get() = if (monthlyLimit > 0) monthlyUsed.toFloat() / monthlyLimit else 0f
}

data class RouterStatus(
    val isRunning: Boolean = false,
    val port: Int = 20128,
    val uptime: Long = 0,
    val providers: List<ProviderStatus> = emptyList(),
    val quotas: Map<String, QuotaUsage> = emptyMap()
)

data class ProviderStatus(
    val name: String,
    val type: ProviderType,
    val isAvailable: Boolean = false,
    val models: List<String> = emptyList(),
    val errorCount: Int = 0,
    val lastUsed: Long? = null
)

sealed class RouterEvent {
    data class RouterStarted(val port: Int) : RouterEvent()
    data class RouterStopped(val reason: String? = null) : RouterEvent()
    data class RouterError(val message: String) : RouterEvent()
    data class ProviderStatusChanged(val provider: String, val available: Boolean) : RouterEvent()
    data class QuotaUpdated(val model: String, val usage: QuotaUsage) : RouterEvent()
}

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val stream: Boolean = false
)

data class RouterChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

data class Choice(
    val index: Int,
    val message: RouterChatMessage,
    val finishReason: String? = null
)

data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)