package com.family.tree.core.ai

import kotlinx.serialization.Serializable

/**
 * Типы поддерживаемых AI провайдеров.
 */
enum class AiProvider {
    OPENAI,      // OpenAI (GPT-4, GPT-3.5)
    ANTHROPIC,   // Anthropic (Claude)
    OLLAMA,      // Локальная модель через Ollama
    CUSTOM       // Пользовательский endpoint (OpenAI-совместимый API)
}

/**
 * Конфигурация для подключения к AI сервису.
 */
@Serializable
data class AiConfig(
    val provider: String = "OPENAI",  // String для совместимости с kotlinx.serialization
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "",  // Для CUSTOM и OLLAMA
    val temperature: Double = 0.7,
    val maxTokens: Int = 4000
) {
    fun getProvider(): AiProvider = try {
        AiProvider.valueOf(provider)
    } catch (e: Exception) {
        AiProvider.OPENAI
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
    
    val OPENAI_GPT4O = AiConfig(
        provider = "OPENAI",
        model = "gpt-4o",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val OPENAI_GPT4_TURBO = AiConfig(
        provider = "OPENAI",
        model = "gpt-4-turbo-preview",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val ANTHROPIC_CLAUDE_SONNET = AiConfig(
        provider = "ANTHROPIC",
        model = "claude-3-5-sonnet-20241022",
        temperature = 0.7,
        maxTokens = 4000
    )
    
    val ANTHROPIC_CLAUDE_HAIKU = AiConfig(
        provider = "ANTHROPIC",
        model = "claude-3-5-haiku-20241022",
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
    
    fun getAllPresets(): List<Pair<String, AiConfig>> = listOf(
        "OpenAI GPT-4o-mini (рекомендуется)" to OPENAI_GPT4O_MINI,
        "OpenAI GPT-4o" to OPENAI_GPT4O,
        "OpenAI GPT-4 Turbo" to OPENAI_GPT4_TURBO,
        "Claude 3.5 Sonnet" to ANTHROPIC_CLAUDE_SONNET,
        "Claude 3.5 Haiku" to ANTHROPIC_CLAUDE_HAIKU,
        "Ollama Llama 3.2" to OLLAMA_LLAMA3,
        "Ollama Mistral" to OLLAMA_MISTRAL
    )
}
