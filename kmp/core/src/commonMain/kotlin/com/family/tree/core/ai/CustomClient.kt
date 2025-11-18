package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с пользовательскими OpenAI-совместимыми API.
 * Поддерживает любые сервисы, совместимые с OpenAI Chat Completions API.
 */
class CustomClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        if (config.baseUrl.isBlank()) {
            throw IllegalArgumentException("Base URL is required for custom API")
        }
        
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        
        val requestBody = buildJsonObject {
            put("model", config.model)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
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
                // API key может быть необязательным для локальных сервисов
                if (config.apiKey.isNotBlank()) {
                    header("Authorization", "Bearer ${config.apiKey}")
                }
                setBody(requestBody.toString())
            }
            
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Используем формат OpenAI для парсинга ответа
            val choices = responseJson["choices"]?.jsonArray
            if (choices == null || choices.isEmpty()) {
                throw Exception("No choices in API response")
            }
            
            val message = choices[0].jsonObject["message"]?.jsonObject
            val content = message?.get("content")?.jsonPrimitive?.content
            
            if (content == null) {
                throw Exception("No content in API response")
            }
            
            return content
        } catch (e: Exception) {
            throw Exception(
                "Failed to connect to custom API at ${config.baseUrl}. " +
                "Make sure the endpoint is correct and supports OpenAI-compatible chat completions. " +
                "Original error: ${e.message}",
                e
            )
        } finally {
            client.close()
        }
    }
}
