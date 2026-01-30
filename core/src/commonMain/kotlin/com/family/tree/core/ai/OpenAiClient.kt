package com.family.tree.core.ai

import io.ktor.http.*
import io.ktor.client.request.*
import kotlinx.serialization.json.*

/**
 * Client for OpenAI API.
 */
class OpenAiClient : BaseAiClient() {
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val apiKey = config.getApiKeyForProvider()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("OpenAI API key is required. Please configure it in the AI Settings menu.")
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
        
        val responseText = executeRequest(url, requestBody.toString()) {
            header("Authorization", "Bearer $apiKey")
        }
        
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        
        // Extract response text from OpenAI structure
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
    }
}
