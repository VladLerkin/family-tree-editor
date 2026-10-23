package com.family.tree.core.ai

import kotlinx.serialization.Serializable

/**
 * Types of supported AI providers.
 */
enum class AiProvider {
    OPENAI,      // OpenAI (GPT-4, GPT-3.5)
    GOOGLE,      // Google (Gemini)
    OLLAMA,      // Local model via Ollama
    CUSTOM,      // Custom endpoint (OpenAI-compatible API)
    LOCAL_LLAMATIK // Local on-device execution via Llamatik
}

/**
 * Types of supported audio transcription providers.
 */
enum class TranscriptionProvider {
    OPENAI_WHISPER,    // OpenAI Whisper API
    GOOGLE_SPEECH,     // Google Cloud Speech-to-Text
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
    val model: String = "gpt-6-luna",
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
            TranscriptionProvider.SHERPA_LOCAL -> ""
        }
    }
}

/**
 * Moves settings saved with the retired `gpt-4o-mini` (API shutdown 2027-01-14) to
 * [AiPresets.OPENAI_GPT6_LUNA]; everything else, including API keys, is kept as is.
 */
fun AiConfig.withMigratedModel(): AiConfig =
    if (provider == "OPENAI" && model == "gpt-4o-mini") copy(model = AiPresets.OPENAI_GPT6_LUNA.model) else this

/** A named [AiConfig] shown in the presets dropdown. */
data class AiPreset(val name: String, val config: AiConfig)

/**
 * Preset configurations for popular models.
 */
object AiPresets {
    val OPENAI_GPT6_LUNA = AiConfig(
        provider = "OPENAI",
        model = "gpt-6-luna",
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

    
    fun getAllPresets(): List<AiPreset> = listOf(
        AiPreset("OpenAI GPT-6 Luna (recommended)", OPENAI_GPT6_LUNA),

        AiPreset("Google Gemini 3.8 Flash", GOOGLE_GEMINI_3_8_FLASH),
        AiPreset("Google Gemini 2.5 Flash", GOOGLE_GEMINI_2_5_FLASH),

        AiPreset("Ollama Gemma 4 E4B (local)", OLLAMA_GEMMA_4_E4B),
        
        AiPreset("Local Gemma 4 E4B QAT (Offline & Free, ~4GB)", LOCAL_GEMMA_4_E4B)
    )
}
