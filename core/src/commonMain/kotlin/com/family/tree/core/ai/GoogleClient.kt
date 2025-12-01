package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Client for Google Gemini API.
 */
class GoogleClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val apiKey = config.getApiKeyForProvider()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Google API key is required")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://generativelanguage.googleapis.com/v1" }
        val url = "$baseUrl/models/${config.model}:generateContent?key=$apiKey"
        
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", config.temperature)
                put("maxOutputTokens", config.maxTokens)
            }
            // Add safety settings to avoid false blocks
            // for genealogical content (names, dates, family relationships)
            putJsonArray("safetySettings") {
                addJsonObject {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_NONE")
                }
                addJsonObject {
                    put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                    put("threshold", "BLOCK_NONE")
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
            // Log request for debugging
            val requestBodyString = requestBody.toString()
            println("[DEBUG_LOG] GoogleClient: Sending request to Gemini API")
            println("[DEBUG_LOG] GoogleClient: Request body length: ${requestBodyString.length}")
            println("[DEBUG_LOG] GoogleClient: Request body preview (first 1000 chars): ${requestBodyString.take(1000)}")
            
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBodyString)
            }
            
            val responseText = response.bodyAsText()
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Log full response for debugging
            println("[DEBUG_LOG] GoogleClient: Full Gemini API response: $responseText")
            
            // Extract response text from Gemini API structure
            val candidates = responseJson["candidates"]?.jsonArray
            if (candidates == null || candidates.isEmpty()) {
                // Check for block information
                val promptFeedback = responseJson["promptFeedback"]?.jsonObject
                if (promptFeedback != null) {
                    println("[DEBUG_LOG] GoogleClient: promptFeedback found: $promptFeedback")
                    val blockReason = promptFeedback["blockReason"]?.jsonPrimitive?.content
                    if (blockReason != null) {
                        throw Exception("Google Gemini blocked the request: $blockReason")
                    }
                }
                throw Exception("No candidates in Google Gemini response. Full response: $responseText")
            }
            
            val content = candidates[0].jsonObject["content"]?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            if (parts == null || parts.isEmpty()) {
                throw Exception("No parts in Google Gemini response")
            }
            
            val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
            
            if (text == null) {
                throw Exception("No text in Google Gemini response")
            }
            
            return text
        } finally {
            client.close()
        }
    }
}
