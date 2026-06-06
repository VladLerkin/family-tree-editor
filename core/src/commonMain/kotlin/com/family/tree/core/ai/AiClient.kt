package com.family.tree.core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Interface for interacting with AI API.
 */
interface AiClient {
    /**
     * Sends a simple prompt to AI and returns the response.
     * Deprecated in favor of sendChat for complex interactions.
     * 
     * @param prompt Prompt for AI
     * @param config AI configuration
     * @return Response from AI
     */
    suspend fun sendPrompt(prompt: String, config: AiConfig): String
    
    /**
     * Sends a sequence of messages and optional tool descriptors to the AI.
     * Support for Function Calling (Tool Calling).
     */
    suspend fun sendChat(
        messages: List<AiMessage>,
        config: AiConfig,
        tools: List<AiToolDescriptor> = emptyList()
    ): AiMessage {
        // Default implementation for backward compatibility or simple providers
        val flattenedPrompt = messages.joinToString("\n") { "${it.role}: ${it.content ?: ""}" }
        val response = sendPrompt(flattenedPrompt, config)
        return AiMessage(role = "assistant", content = response)
    }

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
 * Data class for AI message in a conversation.
 */
@Serializable
data class AiMessage(
    val role: String, // system, user, assistant, tool
    val content: String? = null,
    val toolCalls: List<AiToolCall>? = null,
    val toolCallId: String? = null // Required if role is "tool"
)

/**
 * Data class for a tool call from the AI.
 */
@Serializable
data class AiToolCall(
    val id: String,
    val type: String = "function",
    val function: AiFunctionCall
)

/**
 * Data class for function call details.
 */
@Serializable
data class AiFunctionCall(
    val name: String,
    val arguments: String // JSON string
)

/**
 * Data class for defining a tool available to the AI.
 */
@Serializable
data class AiToolDescriptor(
    val name: String,
    val description: String,
    val parameters: JsonObject // JSON Schema
)

/**
 * Factory for creating AI clients based on provider.
 */
class AiClientFactory(
    private val openAiClient: OpenAiClient,
    private val googleClient: GoogleClient,
    private val yandexClient: YandexClient,
    private val ollamaClient: OllamaClient,
    private val customClient: CustomClient,
    private val localAiClient: LocalAiClient
) {
    /**
     * Creates a client for the specified provider.
     */
    fun createClient(provider: AiProvider): AiClient {
        return when (provider) {
            AiProvider.OPENAI -> openAiClient
            AiProvider.GOOGLE -> googleClient
            AiProvider.YANDEX -> yandexClient
            AiProvider.OLLAMA -> ollamaClient
            AiProvider.CUSTOM -> customClient
            AiProvider.LOCAL_LLAMATIK -> localAiClient
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
