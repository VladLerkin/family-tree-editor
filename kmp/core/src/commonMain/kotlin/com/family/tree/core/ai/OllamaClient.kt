package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с локальными моделями через Ollama.
 * Ollama использует OpenAI-совместимый API.
 */
class OllamaClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
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
        
        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000 // 120 seconds for LLM processing
                connectTimeoutMillis = 30_000  // 30 seconds for connection
                socketTimeoutMillis = 120_000  // 120 seconds for socket
            }
        }
        try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            val responseText = response.bodyAsText()
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
        } finally {
            client.close()
        }
    }
}
