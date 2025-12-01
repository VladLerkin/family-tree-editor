package com.family.tree.core.ai

/**
 * Interface for interacting with AI API.
 */
interface AiClient {
    /**
     * Sends a prompt to AI and returns the response.
     * 
     * @param prompt Prompt for AI
     * @param config AI configuration
     * @return Response from AI
     */
    suspend fun sendPrompt(prompt: String, config: AiConfig): String
    
    /**
     * Transcribes audio to text (currently supported only by OpenAI).
     * 
     * @param audioData Audio data (supported formats: m4a, mp3, wav, webm)
     * @param config AI configuration
     * @return Transcribed text
     */
    suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        throw UnsupportedOperationException("Audio transcription is not supported by this AI provider")
    }
}

/**
 * Factory for creating AI clients based on provider.
 */
object AiClientFactory {
    /**
     * Creates a client for the specified provider.
     */
    fun createClient(provider: AiProvider): AiClient {
        return when (provider) {
            AiProvider.OPENAI -> OpenAiClient()
            AiProvider.GOOGLE -> GoogleClient()
            AiProvider.YANDEX -> YandexClient()
            AiProvider.OLLAMA -> OllamaClient()
            AiProvider.CUSTOM -> CustomClient()
        }
    }
    
    /**
     * Creates a client based on configuration.
     */
    fun createClient(config: AiConfig): AiClient {
        return createClient(config.getProvider())
    }
}

/**
 * Result of AI request execution.
 */
sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String, val cause: Throwable? = null) : AiResult()
}

/**
 * Wrapper for safe execution of AI requests with error handling.
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
