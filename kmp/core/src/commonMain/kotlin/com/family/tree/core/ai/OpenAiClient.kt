package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с OpenAI API.
 */
class OpenAiClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        if (config.apiKey.isBlank()) {
            throw IllegalArgumentException("OpenAI API key is required")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://api.openai.com/v1" }
        val url = "$baseUrl/chat/completions"
        
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
                header("Authorization", "Bearer ${config.apiKey}")
                setBody(requestBody.toString())
            }
            
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем текст ответа из структуры OpenAI
            val choices = responseJson["choices"]?.jsonArray
            if (choices == null || choices.isEmpty()) {
                throw Exception("No choices in OpenAI response")
            }
            
            val message = choices[0].jsonObject["message"]?.jsonObject
            val content = message?.get("content")?.jsonPrimitive?.content
            
            if (content == null) {
                throw Exception("No content in OpenAI response")
            }
            
            return content
        } finally {
            client.close()
        }
    }
    
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        if (config.apiKey.isBlank()) {
            throw IllegalArgumentException("OpenAI API key is required")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://api.openai.com/v1" }
        val url = "$baseUrl/audio/transcriptions"
        
        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000 // 120 seconds for audio processing
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        
        try {
            val response = client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    append("file", audioData, Headers.build {
                        append(HttpHeaders.ContentType, "audio/m4a")
                        append(HttpHeaders.ContentDisposition, "filename=\"audio.m4a\"")
                    })
                    append("model", "whisper-1")
                    append("language", "ru")
                }
            ) {
                header("Authorization", "Bearer ${config.apiKey}")
            }
            
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем транскрибированный текст
            val text = responseJson["text"]?.jsonPrimitive?.content
            
            if (text == null) {
                throw Exception("No text in Whisper API response")
            }
            
            return text
        } finally {
            client.close()
        }
    }
}
