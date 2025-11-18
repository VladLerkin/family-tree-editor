package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с Anthropic Claude API.
 */
class AnthropicClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        if (config.apiKey.isBlank()) {
            throw IllegalArgumentException("Anthropic API key is required")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://api.anthropic.com/v1" }
        val url = "$baseUrl/messages"
        
        val requestBody = buildJsonObject {
            put("model", config.model)
            put("max_tokens", config.maxTokens)
            put("temperature", config.temperature)
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
                header("x-api-key", config.apiKey)
                header("anthropic-version", "2023-06-01")
                setBody(requestBody.toString())
            }
            
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем текст ответа из структуры Anthropic
            val content = responseJson["content"]?.jsonArray
            if (content == null || content.isEmpty()) {
                throw Exception("No content in Anthropic response")
            }
            
            val textContent = content[0].jsonObject["text"]?.jsonPrimitive?.content
            
            if (textContent == null) {
                throw Exception("No text in Anthropic response")
            }
            
            return textContent
        } finally {
            client.close()
        }
    }
}
