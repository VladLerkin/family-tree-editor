package com.family.tree.core.ai

import io.ktor.client.request.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.delay

/**
 * Client for OpenAI API.
 */
class OpenAiClient : BaseAiClient() {
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String {
        val message = sendChat(
            messages = listOf(AiMessage(role = "user", content = prompt)),
            config = config
        )
        return message.content ?: ""
    }

    override suspend fun sendChat(
        messages: List<AiMessage>,
        config: AiConfig,
        tools: List<AiToolDescriptor>
    ): AiMessage {
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
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        
                        // OpenAI requires 'content' to be a string for 'assistant' and 'tool' roles.
                        // If it's null, we must explicitly send an empty string or null (depending on API version),
                        // but the error "expected a string, got null" suggests it must be a string.
                        val content = msg.content ?: if (msg.role == "assistant" || msg.role == "tool") "" else null
                        content?.let { put("content", it) }
                        
                        msg.toolCallId?.let { put("tool_call_id", it) }
                        msg.toolCalls?.let { calls ->
                            putJsonArray("tool_calls") {
                                calls.forEach { call ->
                                    addJsonObject {
                                        put("id", call.id)
                                        put("type", call.type)
                                        putJsonObject("function") {
                                            put("name", call.function.name)
                                            put("arguments", call.function.arguments)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (tools.isNotEmpty()) {
                putJsonArray("tools") {
                    tools.forEach { tool ->
                        addJsonObject {
                            put("type", "function")
                            putJsonObject("function") {
                                put("name", tool.name)
                                put("description", tool.description)
                                put("parameters", tool.parameters)
                            }
                        }
                    }
                }
            }
        }
        
        var attempt = 0
        val maxAttempts = 6
        var responseText = ""
        var responseJson: JsonObject = buildJsonObject {}
        
        while (attempt < maxAttempts) {
            try {
                responseText = executeRequest(url, requestBody.toString()) {
                    header("Authorization", "Bearer $apiKey")
                }
                
                responseJson = json.parseToJsonElement(responseText).jsonObject
                
                val errorObj = responseJson["error"]?.jsonObject
                val isRateLimit = errorObj?.get("code")?.jsonPrimitive?.contentOrNull == "rate_limit_exceeded" ||
                                  errorObj?.get("type")?.jsonPrimitive?.contentOrNull == "tokens" ||
                                  errorObj?.get("message")?.jsonPrimitive?.contentOrNull?.contains("Rate limit") == true
                
                if (isRateLimit) {
                    throw Exception("Rate limit exceeded")
                }
                
                val choices = responseJson["choices"]?.jsonArray
                if (choices == null || choices.isEmpty()) {
                    throw Exception("No choices in OpenAI response: $responseText")
                }
                
                break // Success, exit retry loop
            } catch (e: Exception) {
                val isRateLimitException = e.message?.contains("Rate limit exceeded") == true || 
                                           e.message?.contains("429") == true
                
                attempt++
                if (attempt >= maxAttempts || !isRateLimitException) {
                    throw e
                }
                
                // Extract wait time if possible, otherwise exponential backoff
                var waitMs = 2000L * attempt
                val errorMsg = responseJson["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                if (errorMsg != null) {
                    // Try to match "Please try again in Xms."
                    val msMatch = Regex("in (\\d+)ms").find(errorMsg)
                    if (msMatch != null) {
                        waitMs = msMatch.groupValues[1].toLongOrNull() ?: waitMs
                    } else {
                        // Match seconds "in Xs."
                        val sMatch = Regex("in (\\d+(?:\\.\\d+)?)s").find(errorMsg)
                        if (sMatch != null) {
                            waitMs = ((sMatch.groupValues[1].toDoubleOrNull() ?: 1.0) * 1000).toLong()
                        }
                    }
                }
                
                // Add a small buffer to the requested wait time
                waitMs += 500L
                println("[AI-DEBUG] OpenAI Rate Limit hit! Retrying in ${waitMs}ms (Attempt $attempt of $maxAttempts)...")
                delay(waitMs)
            }
        }
        
        val choices = responseJson["choices"]?.jsonArray!!
        val messageObj = choices[0].jsonObject["message"]?.jsonObject
            ?: throw Exception("No message in OpenAI choice")
            
        val role = messageObj["role"]?.jsonPrimitive?.content ?: "assistant"
        val content = messageObj["content"]?.jsonPrimitive?.contentOrNull
        
        val toolCalls = messageObj["tool_calls"]?.jsonArray?.map { callElement ->
            val callObj = callElement.jsonObject
            val funcObj = callObj["function"]?.jsonObject ?: throw Exception("No function in tool call")
            AiToolCall(
                id = callObj["id"]?.jsonPrimitive?.content ?: "",
                type = callObj["type"]?.jsonPrimitive?.content ?: "function",
                function = AiFunctionCall(
                    name = funcObj["name"]?.jsonPrimitive?.content ?: "",
                    arguments = funcObj["arguments"]?.jsonPrimitive?.content ?: "{}"
                )
            )
        }
        
        return AiMessage(
            role = role,
            content = content,
            toolCalls = toolCalls
        )
    }
}
