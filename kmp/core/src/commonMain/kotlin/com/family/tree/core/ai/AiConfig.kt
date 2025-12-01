package com.family.tree.core.ai

import kotlinx.serialization.Serializable

/**
 * Types of supported AI providers.
 */
enum class AiProvider {
    OPENAI,      // OpenAI (GPT-4, GPT-3.5)
    GOOGLE,      // Google (Gemini)
    OLLAMA,      // Local model via Ollama
    YANDEX,      // YandexGPT
    CUSTOM       // Custom endpoint (OpenAI-compatible API)
}

/**
 * Types of supported audio transcription providers.
 */
enum class TranscriptionProvider {
    OPENAI_WHISPER,    // OpenAI Whisper API
    GOOGLE_SPEECH,     // Google Cloud Speech-to-Text
    YANDEX_SPEECHKIT   // Yandex SpeechKit
}

/**
 * Configuration for connecting to an AI service.
 */
@Serializable
data class AiConfig(
    val provider: String = "OPENAI",  // String for kotlinx.serialization compatibility
    @Deprecated("Use openaiApiKey, anthropicApiKey, or googleApiKey instead")
    val apiKey: String = "",  // Kept for backward compatibility
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "",  // For CUSTOM and OLLAMA
    val temperature: Double = 0.7,
    val maxTokens: Int = 4000,
    val language: String = "",  // Language for audio transcription (ISO-639-1 code, e.g. "ka" for Georgian)
    val transcriptionProvider: String = "OPENAI_WHISPER",  // Transcription provider: OPENAI_WHISPER or GOOGLE_SPEECH
    @Deprecated("Use googleApiKey instead")
    val googleApiKey: String = "",  // Kept for backward compatibility (transcription)
    
    // Separate API keys for each provider group
    val openaiApiKey: String = "",     // API key for OpenAI (GPT models and Whisper)
    val googleAiApiKey: String = "",   // API key for Google AI (Gemini models and Speech-to-Text)
    val yandexApiKey: String = "",      // API key for Yandex Cloud (SpeechKit)
    
    // Folder ID for Yandex Cloud (optional, can be omitted when using service account API key)
    val yandexFolderId: String = "b1guuckqs9tjoc2aiuge"
) {
    fun getProvider(): AiProvider = try {
        AiProvider.valueOf(provider)
    } catch (e: Exception) {
        AiProvider.OPENAI
    }
    
    fun getTranscriptionProvider(): TranscriptionProvider = try {
        TranscriptionProvider.valueOf(transcriptionProvider)
    } catch (e: Exception) {
        TranscriptionProvider.OPENAI_WHISPER
    }
    
    /**
     * Gets the actual API key for the current provider.
     * First checks provider-specific keys, then falls back to old fields.
     */
    fun getApiKeyForProvider(): String {
        return when (getProvider()) {
            AiProvider.OPENAI -> openaiApiKey.ifBlank { apiKey }
            AiProvider.GOOGLE -> googleAiApiKey.ifBlank { googleApiKey.ifBlank { apiKey } }
            AiProvider.YANDEX -> yandexApiKey.ifBlank { apiKey }
            AiProvider.OLLAMA, AiProvider.CUSTOM -> apiKey  // For Ollama and Custom use old field
        }
    }
    
    /**
     * Gets the actual API key for the transcription provider.
     */
    fun getApiKeyForTranscription(): String {
        return when (getTranscriptionProvider()) {
            TranscriptionProvider.OPENAI_WHISPER -> openaiApiKey.ifBlank { apiKey }
            TranscriptionProvider.GOOGLE_SPEECH -> googleAiApiKey.ifBlank { googleApiKey }
            TranscriptionProvider.YANDEX_SPEECHKIT -> yandexApiKey.ifBlank { apiKey }
        }
    }
}

/**
 * Preset configurations for popular models.
 */
object AiPresets {
    val OPENAI_GPT4O_MINI = AiConfig(
        provider = "OPENAI",
        model = "gpt-4o-mini",
        temperature = 0.7,
        maxTokens = 4000
    )
    

    
    val GOOGLE_GEMINI_2_0_FLASH = AiConfig(
        provider = "GOOGLE",
        model = "gemini-2.0-flash",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val OLLAMA_LLAMA3 = AiConfig(
        provider = "OLLAMA",
        model = "llama3.2",
        baseUrl = "http://localhost:11434",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val OLLAMA_MISTRAL = AiConfig(
        provider = "OLLAMA",
        model = "mistral",
        baseUrl = "http://localhost:11434",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val YANDEX_GPT_4 = AiConfig(
        provider = "YANDEX",
        model = "yandexgpt",
        temperature = 0.6,
        maxTokens = 4000
    )


    
    fun getAllPresets(): List<Pair<String, AiConfig>> = listOf(
        "OpenAI GPT-4o-mini (recommended)" to OPENAI_GPT4O_MINI,

        "Google Gemini 2.0 Flash" to GOOGLE_GEMINI_2_0_FLASH,
        "YandexGPT 4" to YANDEX_GPT_4,

        "Ollama Llama 3.2" to OLLAMA_LLAMA3,
        "Ollama Mistral" to OLLAMA_MISTRAL
    )
}
