package com.family.tree.core.ai

import io.ktor.client.request.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с пользовательскими OpenAI-совместимыми API.
 * Поддерживает любые сервисы, совместимые с OpenAI Chat Completions API.
 */
class CustomClient : BaseAiClient() {
    
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
        if (config.baseUrl.isBlank()) {
            throw IllegalArgumentException("Base URL is required for custom API")
        }
        
        val url = "${config.baseUrl.trimEnd('/')}/chat/completions"
        
        val requestBody = buildJsonObject {
            put("model", config.model)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
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
        
        println("[AI-DEBUG] Custom API Request JSON: " + requestBody.toString())
        
        try {
            val apiKey = config.getApiKeyForProvider()
            val responseText = executeRequest(url, requestBody.toString()) {
                if (apiKey.isNotBlank()) {
                    header("Authorization", "Bearer $apiKey")
                }
            }
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            val choices = responseJson["choices"]?.jsonArray
            if (choices == null || choices.isEmpty()) {
                throw Exception("No choices in API response")
            }
            
            val messageObj = choices[0].jsonObject["message"]?.jsonObject
                ?: throw Exception("No message in API response")
                
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception(
                "Failed to connect to custom API at ${config.baseUrl}. " +
                "Make sure the endpoint is correct and supports OpenAI-compatible chat completions. " +
                "Original error: ${e.message}",
                e
            )
        }
    }
}
