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
    CUSTOM,      // Custom endpoint (OpenAI-compatible API)
    LOCAL_LLAMATIK // Local on-device execution via Llamatik
}

/**
 * Types of supported audio transcription providers.
 */
enum class TranscriptionProvider {
    OPENAI_WHISPER,    // OpenAI Whisper API
    GOOGLE_SPEECH,     // Google Cloud Speech-to-Text
    YANDEX_SPEECHKIT,  // Yandex SpeechKit
    SHERPA_LOCAL       // Local offline STT via Sherpa-ONNX
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
    val googleSpeechModel: String = "default", // Google Speech-to-Text model: default, latest_long, latest_short
    val yandexApiKey: String = "",      // API key for Yandex Cloud (SpeechKit)
    
    // Folder ID for Yandex Cloud (optional, can be omitted when using service account API key)
    val yandexFolderId: String = "",
    
    // Tavily API key for Web Search Agent
    val tavilyApiKey: String = "",
    
    // Path to the professional methodology repository (autoresearch-genealogy)
    val autoresearchRepoPath: String = "./autoresearch-genealogy",
    
    // Cookie string for Pamyat Naroda session injection
    val pamyatNarodaCookies: String = "",
    
    // Cookie string for FamilySearch session injection
    val familySearchCookies: String = ""
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
    @Suppress("DEPRECATION")
    fun getApiKeyForProvider(): String {
        return when (getProvider()) {
            AiProvider.OPENAI -> openaiApiKey.ifBlank { apiKey }
            AiProvider.GOOGLE -> googleAiApiKey.ifBlank { googleApiKey.ifBlank { apiKey } }
            AiProvider.YANDEX -> yandexApiKey.ifBlank { apiKey }
            AiProvider.OLLAMA, AiProvider.CUSTOM, AiProvider.LOCAL_LLAMATIK -> apiKey  // For Ollama, Custom and Local use old field
        }
    }
    
    /**
     * Gets the actual API key for the transcription provider.
     */
    @Suppress("DEPRECATION")
    fun getApiKeyForTranscription(): String {
        return when (getTranscriptionProvider()) {
            TranscriptionProvider.OPENAI_WHISPER -> openaiApiKey.ifBlank { apiKey }
            TranscriptionProvider.GOOGLE_SPEECH -> googleAiApiKey.ifBlank { googleApiKey }
            TranscriptionProvider.YANDEX_SPEECHKIT -> yandexApiKey.ifBlank { apiKey }
            TranscriptionProvider.SHERPA_LOCAL -> ""
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
    

    
    val GOOGLE_GEMINI_3_8_FLASH = AiConfig(
        provider = "GOOGLE",
        model = "gemini-3.8-flash",
        temperature = 0.7,
        maxTokens = 4000
    )

    val GOOGLE_GEMINI_2_5_FLASH = AiConfig(
        provider = "GOOGLE",
        model = "gemini-2.5-flash",
        temperature = 0.7,
        maxTokens = 4000
    )

    val OLLAMA_GEMMA_4_E4B = AiConfig(
        provider = "OLLAMA",
        model = "gemma4-e4b-text",
        baseUrl = "http://localhost:11434",
        temperature = 0.7,
        maxTokens = 4000
    )
    

    val LOCAL_GEMMA_4_E4B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "gemma-4-E4B-it-qat-UD-Q4_K_XL.gguf",
        baseUrl = "https://huggingface.co/unsloth/gemma-4-E4B-it-qat-GGUF/resolve/main/gemma-4-E4B-it-qat-UD-Q4_K_XL.gguf",
        temperature = 0.7,
        maxTokens = 4000
    )

    
    val YANDEX_GPT_LITE = AiConfig(
        provider = "YANDEX",
        model = "yandexgpt-lite",
        temperature = 0.6,
        maxTokens = 4000
    )

    fun getAllPresets(): List<Pair<String, AiConfig>> = listOf(
        "OpenAI GPT-4o-mini (recommended)" to OPENAI_GPT4O_MINI,

        "Google Gemini 3.8 Flash" to GOOGLE_GEMINI_3_8_FLASH,
        "Google Gemini 2.5 Flash" to GOOGLE_GEMINI_2_5_FLASH,
        "YandexGPT Lite" to YANDEX_GPT_LITE,

        "Ollama Gemma 4 E4B (local)" to OLLAMA_GEMMA_4_E4B,
        
        "Local Gemma 4 E4B QAT (Offline & Free, ~4GB)" to LOCAL_GEMMA_4_E4B
    )
}
