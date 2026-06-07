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
    VOSK_LOCAL         // Local offline STT via Vosk
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
            TranscriptionProvider.VOSK_LOCAL -> ""
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
    

    
    val GOOGLE_GEMINI_3_1_FLASH_LITE = AiConfig(
        provider = "GOOGLE",
        model = "gemini-3.1-flash-lite",
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
    
    val LOCAL_QWEN_0_5B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
        baseUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val LOCAL_QWEN_3B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "qwen2.5-3b-instruct-q4_k_m.gguf",
        baseUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val LOCAL_LLAMA_3_2_3B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "llama-3.2-3b-instruct-q4_k_m.gguf",
        baseUrl = "https://huggingface.co/hugging-quants/Llama-3.2-3B-Instruct-Q4_K_M-GGUF/resolve/main/llama-3.2-3b-instruct-q4_k_m.gguf",
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

    val LOCAL_GEMMA_4_E2B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "gemma-4-E2B-it-qat-UD-Q4_K_XL.gguf",
        baseUrl = "https://huggingface.co/unsloth/gemma-4-E2B-it-qat-GGUF/resolve/main/gemma-4-E2B-it-qat-UD-Q4_K_XL.gguf",
        temperature = 0.7,
        maxTokens = 4000
    )

    val OLLAMA_QWEN2_5_7B = AiConfig(
        provider = "OLLAMA",
        model = "qwen2.5:7b",
        baseUrl = "http://localhost:11434",
        temperature = 0.7,
        maxTokens = 4000
    )

    val OLLAMA_QWEN2_5_3B = AiConfig(
        provider = "OLLAMA",
        model = "qwen2.5:3b",
        baseUrl = "http://localhost:11434",
        temperature = 0.7,
        maxTokens = 4000
    )

    
    val YANDEX_GPT_LITE = AiConfig(
        provider = "YANDEX",
        model = "yandexgpt-lite",
        temperature = 0.6,
        maxTokens = 4000
    )


    
    
    val LOCAL_GEMMA_2_2B = AiConfig(
        provider = "LOCAL_LLAMATIK",
        model = "gemma-2-2b-it-Q4_K_M.gguf",
        baseUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
        temperature = 0.7,
        maxTokens = 4000
    )

    fun getAllPresets(): List<Pair<String, AiConfig>> = listOf(
        "OpenAI GPT-4o-mini (recommended)" to OPENAI_GPT4O_MINI,

        "Google Gemini 3.1 Flash-Lite" to GOOGLE_GEMINI_3_1_FLASH_LITE,
        "YandexGPT Lite" to YANDEX_GPT_LITE,

        "Ollama Gemma 4 E4B (local)" to OLLAMA_GEMMA_4_E4B,
        "Ollama Qwen 2.5 7B (offline and free)" to OLLAMA_QWEN2_5_7B,
        "Ollama Qwen 2.5 3B (fast & offline)" to OLLAMA_QWEN2_5_3B,
        
        "Local Qwen 2.5 0.5B (Test model, ~400MB)" to LOCAL_QWEN_0_5B,
        "Local Qwen 2.5 3B (High quality, ~2GB)" to LOCAL_QWEN_3B,
        "Local Llama 3.2 3B (High quality, ~2GB)" to LOCAL_LLAMA_3_2_3B,
        "Local Gemma 2 2B (Fast & High quality, ~1.6GB)" to LOCAL_GEMMA_2_2B,
        "Local Gemma 4 E4B QAT (Compression, ~4GB)" to LOCAL_GEMMA_4_E4B,
        "Local Gemma 4 E2B QAT (Compression, ~2GB)" to LOCAL_GEMMA_4_E2B
    )
}
