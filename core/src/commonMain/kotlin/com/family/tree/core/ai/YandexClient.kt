package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Client for YandexGPT API (text generation).
 * 
 * Documentation: https://yandex.cloud/en/docs/foundation-models/concepts/yandexgpt
 */
class YandexClient : AiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val apiKey = config.getApiKeyForProvider()
        val folderId = config.yandexFolderId.trim()

        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Yandex Cloud API key is required. Get it from Yandex Cloud Console.")
        }

        // Determine model (yandexgpt or yandexgpt-lite)
        // Note: 'yandexgpt' points to the current version (currently GPT-4 equivalent)
        val modelName = when {
            config.model.contains("lite", ignoreCase = true) -> "yandexgpt-lite"
            else -> "yandexgpt"
        }
        
        // modelUri format: gpt://<folder_id>/<model_name>/latest
        // YandexGPT API requires folder_id in URI
        val modelUri = "gpt://$folderId/$modelName/latest"
        
        println("[DEBUG_LOG] YandexClient: Sending request to YandexGPT")
        println("[DEBUG_LOG] YandexClient: folderId='$folderId'")
        println("[DEBUG_LOG] YandexClient: modelName='$modelName'")
        println("[DEBUG_LOG] YandexClient: modelUri='$modelUri'")
        
        val url = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"

        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
            }
        }

        try {
            val requestBody = buildJsonObject {
                put("modelUri", modelUri)
                putJsonObject("completionOptions") {
                    put("stream", false)
                    put("temperature", config.temperature)
                    put("maxTokens", config.maxTokens)
                }
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("text", "You are a helpful assistant for genealogy data extraction.")
                    }
                    addJsonObject {
                        put("role", "user")
                        put("text", prompt)
                    }
                }
            }

            val response = client.post(url) {
                header("Authorization", "Api-Key $apiKey")
                // Add x-folder-id only if specified and not "default"
                // When using service account API key, folder ID is determined automatically
                if (folderId.isNotBlank()) {
                    header("x-folder-id", folderId)
                }
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            val responseText = response.bodyAsText()
            
            if (!response.status.isSuccess()) {
                throw Exception("YandexGPT API error: ${response.status} - $responseText")
            }

            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Extract result from response
            val result = responseJson["result"]?.jsonObject
            val alternatives = result?.get("alternatives")?.jsonArray
            val firstAlt = alternatives?.firstOrNull()?.jsonObject
            val message = firstAlt?.get("message")?.jsonObject
            val text = message?.get("text")?.jsonPrimitive?.content

            if (text.isNullOrBlank()) {
                throw Exception("Empty response from YandexGPT")
            }

            return text

        } catch (e: Exception) {
            throw Exception("Failed to connect to YandexGPT: ${e.message}", e)
        } finally {
            client.close()
        }
    }
}
