package com.family.tree.core.ai

import kotlinx.serialization.Serializable

/**
 * Типы поддерживаемых AI провайдеров.
 */
enum class AiProvider {
    OPENAI,      // OpenAI (GPT-4, GPT-3.5)
    GOOGLE,      // Google (Gemini)
    OLLAMA,      // Локальная модель через Ollama
    YANDEX,      // YandexGPT
    CUSTOM       // Пользовательский endpoint (OpenAI-совместимый API)
}

/**
 * Типы поддерживаемых провайдеров транскрипции аудио.
 */
enum class TranscriptionProvider {
    OPENAI_WHISPER,    // OpenAI Whisper API
    GOOGLE_SPEECH,     // Google Cloud Speech-to-Text
    YANDEX_SPEECHKIT   // Yandex SpeechKit
}

/**
 * Конфигурация для подключения к AI сервису.
 */
@Serializable
data class AiConfig(
    val provider: String = "OPENAI",  // String для совместимости с kotlinx.serialization
    @Deprecated("Use openaiApiKey, anthropicApiKey, or googleApiKey instead")
    val apiKey: String = "",  // Оставлено для обратной совместимости
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "",  // Для CUSTOM и OLLAMA
    val temperature: Double = 0.7,
    val maxTokens: Int = 4000,
    val language: String = "",  // Язык для транскрипции аудио (ISO-639-1 код, например "ka" для грузинского)
    val transcriptionProvider: String = "OPENAI_WHISPER",  // Провайдер транскрипции: OPENAI_WHISPER или GOOGLE_SPEECH
    @Deprecated("Use googleApiKey instead")
    val googleApiKey: String = "",  // Оставлено для обратной совместимости (транскрипция)
    
    // Отдельные API ключи для каждой группы провайдеров
    val openaiApiKey: String = "",     // API ключ для OpenAI (GPT модели и Whisper)
    val googleAiApiKey: String = "",   // API ключ для Google AI (Gemini модели и Speech-to-Text)
    val yandexApiKey: String = "",      // API ключ для Yandex Cloud (SpeechKit)
    
    // Идентификатор каталога (Folder ID) для Yandex Cloud (опционально, можно не указывать при использовании API ключа сервисного аккаунта)
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
     * Получает актуальный API ключ для текущего провайдера.
     * Сначала проверяет специфичные ключи группы, затем fallback на старые поля.
     */
    fun getApiKeyForProvider(): String {
        return when (getProvider()) {
            AiProvider.OPENAI -> openaiApiKey.ifBlank { apiKey }
            AiProvider.GOOGLE -> googleAiApiKey.ifBlank { googleApiKey.ifBlank { apiKey } }
            AiProvider.YANDEX -> yandexApiKey.ifBlank { apiKey }
            AiProvider.OLLAMA, AiProvider.CUSTOM -> apiKey  // Для Ollama и Custom используем старое поле
        }
    }
    
    /**
     * Получает актуальный API ключ для провайдера транскрипции.
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
 * Предустановленные конфигурации для популярных моделей.
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
        "OpenAI GPT-4o-mini (рекомендуется)" to OPENAI_GPT4O_MINI,

        "Google Gemini 2.0 Flash" to GOOGLE_GEMINI_2_0_FLASH,
        "YandexGPT 4" to YANDEX_GPT_4,

        "Ollama Llama 3.2" to OLLAMA_LLAMA3,
        "Ollama Mistral" to OLLAMA_MISTRAL
    )
}
