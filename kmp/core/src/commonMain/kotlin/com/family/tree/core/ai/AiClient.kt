package com.family.tree.core.ai

/**
 * Интерфейс для взаимодействия с AI API.
 */
interface AiClient {
    /**
     * Отправляет промпт AI и возвращает ответ.
     * 
     * @param prompt Промпт для AI
     * @param config Конфигурация AI
     * @return Ответ от AI
     */
    suspend fun sendPrompt(prompt: String, config: AiConfig): String
}

/**
 * Фабрика для создания клиентов AI в зависимости от провайдера.
 */
object AiClientFactory {
    /**
     * Создаёт клиент для указанного провайдера.
     */
    fun createClient(provider: AiProvider): AiClient {
        return when (provider) {
            AiProvider.OPENAI -> OpenAiClient()
            AiProvider.ANTHROPIC -> AnthropicClient()
            AiProvider.OLLAMA -> OllamaClient()
            AiProvider.CUSTOM -> CustomClient()
        }
    }
    
    /**
     * Создаёт клиент на основе конфигурации.
     */
    fun createClient(config: AiConfig): AiClient {
        return createClient(config.getProvider())
    }
}

/**
 * Результат выполнения AI запроса.
 */
sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String, val cause: Throwable? = null) : AiResult()
}

/**
 * Обёртка для безопасного выполнения AI запросов с обработкой ошибок.
 */
suspend fun AiClient.sendPromptSafe(prompt: String, config: AiConfig): AiResult {
    return try {
        val response = sendPrompt(prompt, config)
        AiResult.Success(response)
    } catch (e: Exception) {
        AiResult.Error(
            message = "Failed to get AI response: ${e.message}",
            cause = e
        )
    }
}
