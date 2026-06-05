package com.family.tree.core.ai

import kotlinx.serialization.json.*

/**
 * Клиент для работы с локальными моделями через Ollama.
 * Ollama использует OpenAI-совместимый API.
 */
class OllamaClient : BaseAiClient() {
    
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
        val baseUrl = config.baseUrl.ifBlank { "http://localhost:11434" }
        val url = "$baseUrl/api/chat"
        
        val requestBody = buildJsonObject {
            put("model", config.model)
            put("stream", false)
            putJsonArray("messages") {
                messages.forEach { msg ->
                    addJsonObject {
                        put("role", msg.role)
                        val content = msg.content ?: if (msg.role == "assistant" || msg.role == "tool") "" else null
                        content?.let { put("content", it) }
                        
                        msg.toolCalls?.let { calls ->
                            putJsonArray("tool_calls") {
                                calls.forEach { call ->
                                    addJsonObject {
                                        putJsonObject("function") {
                                            put("name", call.function.name)
                                            val argsString = call.function.arguments.ifBlank { "{}" }
                                            put("arguments", json.parseToJsonElement(argsString))
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
            putJsonObject("options") {
                put("temperature", config.temperature)
                put("num_predict", config.maxTokens)
            }
        }
        
        println("[AI-DEBUG] Ollama Request JSON: " + requestBody.toString())
        
        try {
            val responseText = executeRequest(url, requestBody.toString())
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            if (responseJson.containsKey("error")) {
                val errorMsg = responseJson["error"]?.jsonPrimitive?.content ?: responseText
                throw Exception("Ollama API error: $errorMsg")
            }
            
            val messageObj = responseJson["message"]?.jsonObject
                ?: throw Exception("No message in Ollama response. Full response: $responseText")
                
            val role = messageObj["role"]?.jsonPrimitive?.content ?: "assistant"
            val content = messageObj["content"]?.jsonPrimitive?.contentOrNull
            
            val toolCalls = messageObj["tool_calls"]?.jsonArray?.mapIndexed { index, callElement ->
                val callObj = callElement.jsonObject
                val funcObj = callObj["function"]?.jsonObject ?: throw Exception("No function in tool call")
                val argumentsObj = funcObj["arguments"]?.jsonObject
                AiToolCall(
                    id = "call_ollama_$index",
                    type = "function",
                    function = AiFunctionCall(
                        name = funcObj["name"]?.jsonPrimitive?.content ?: "",
                        arguments = argumentsObj?.toString() ?: "{}"
                    )
                )
            }
            
            return AiMessage(
                role = role,
                content = content,
                toolCalls = if (toolCalls.isNullOrEmpty()) null else toolCalls
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw Exception(
                "Failed to connect to Ollama at $baseUrl. " +
                "Make sure Ollama is running (ollama serve) and the model '${config.model}' is installed. " +
                "Original error: ${e.message}",
                e
            )
        }
    }
}
