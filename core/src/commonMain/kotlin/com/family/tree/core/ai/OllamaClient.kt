package com.family.tree.core.ai

import kotlinx.serialization.json.*

/**
 * Клиент для работы с локальными моделями через Ollama.
 * Ollama использует OpenAI-совместимый API.
 */
class OllamaClient : BaseAiClient() {
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val baseUrl = config.baseUrl.ifBlank { "http://localhost:11434" }
        val url = "$baseUrl/api/chat"
        
        val requestBody = buildJsonObject {
            put("model", config.model)
            put("stream", false)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            putJsonObject("options") {
                put("temperature", config.temperature)
                put("num_predict", config.maxTokens)
            }
        }
        
        try {
            val responseText = executeRequest(url, requestBody.toString())
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем текст ответа из структуры Ollama
            val message = responseJson["message"]?.jsonObject
            val content = message?.get("content")?.jsonPrimitive?.content
            
            if (content == null) {
                throw Exception("No content in Ollama response")
            }
            
            return content
        } catch (e: Exception) {
            // Более подробная ошибка для локальных моделей
            throw Exception(
                "Failed to connect to Ollama at $baseUrl. " +
                "Make sure Ollama is running (ollama serve) and the model '${config.model}' is installed. " +
                "Original error: ${e.message}",
                e
            )
        }
    }
}
