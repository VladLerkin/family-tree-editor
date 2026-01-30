package com.family.tree.core.ai

import kotlinx.serialization.json.*

/**
 * Client for Google Gemini API.
 */
class GoogleClient : BaseAiClient() {
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val apiKey = config.getApiKeyForProvider()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Google API key is required. Please configure it in the AI Settings menu.")
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
        
        // Log request for debugging
        val requestBodyString = requestBody.toString()
        println("[DEBUG_LOG] GoogleClient: Sending request to Gemini API")
        println("[DEBUG_LOG] GoogleClient: Request body length: ${requestBodyString.length}")
        println("[DEBUG_LOG] GoogleClient: Request body preview (first 1000 chars): ${requestBodyString.take(1000)}")
        
        val responseText = executeRequest(url, requestBodyString)
        
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
    }
}
