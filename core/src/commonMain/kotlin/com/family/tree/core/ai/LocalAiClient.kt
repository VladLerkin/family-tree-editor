package com.family.tree.core.ai

import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocalAiClient(
    private val directoryProvider: ModelDirectoryProvider
) : AiClient {
    
    // Cache the instance to avoid reloading the 4GB file on every request
    private var currentModelPath: String? = null
    
    override suspend fun sendPrompt(prompt: String, config: AiConfig): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // MUST be called BEFORE ensureModelLoaded so that gpuLayers=99 and useMmap=true are applied during llama_model_load!
            LlamaBridge.updateGenerateParams(
                temperature = 0.1f, // Force low temperature for structured JSON extraction
                maxTokens = config.maxTokens,
                topP = 0.9f,
                topK = 40,
                repeatPenalty = 1.1f,
                contextLength = 16384, // Increased from 4096 to support large agent prompts
                numThreads = 4, // 4 threads is optimal for Apple Silicon (avoids CPU/GPU lock contention)
                useMmap = true,
                flashAttention = true, // Enable Flash Attention for speedup
                batchSize = 512,
                gpuLayers = 99 // Offload all layers to GPU (Metal/CUDA)
            )
            
            ensureModelLoaded(config)
            
            suspendCancellableCoroutine { continuation ->
                val sb = StringBuilder()
                LlamaBridge.generateStream(
                    prompt = prompt,
                    callback = object : GenStream {
                        override fun onDelta(text: String) {
                            sb.append(text)
                            // Print to log for debugging
                            println("[DEBUG_LOG] LocalAiClient onDelta: $text")
                        }
                        override fun onComplete() {
                            if (continuation.isActive) {
                                continuation.resume(sb.toString())
                            }
                        }
                        override fun onError(message: String) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(RuntimeException(message))
                            }
                        }
                    }
                )
                continuation.invokeOnCancellation {
                    LlamaBridge.nativeCancelGenerate()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to generate response from local model: ${e.message}", e)
        }
    }
    
    override suspend fun sendChat(
        messages: List<AiMessage>,
        config: AiConfig,
        tools: List<AiToolDescriptor>
    ): AiMessage {
        val formattedPrompt = formatPrompt(messages, config.model, tools)
        val response = sendPrompt(formattedPrompt, config)
        
        // Parse Koog-style tool calls from the response string
        // Format: <|tool_call>call:toolName{key:<|"|>value<|"|>}<tool_call|>
        val toolCalls = mutableListOf<AiToolCall>()
        val toolCallRegex = "(?s)<\\|tool_call\\>call:([a-zA-Z0-9_\\-]+)\\{(.*?)\\}<tool_call\\|>".toRegex()
        
        val matches = toolCallRegex.findAll(response)
        for (match in matches) {
            val functionName = match.groupValues[1]
            val argsRaw = match.groupValues[2]
            
            val argsJson = parseKoogArgsToJson(argsRaw)
            
            toolCalls.add(
                AiToolCall(
                    id = "call_" + kotlin.random.Random.nextInt(10000, 99999),
                    function = AiFunctionCall(
                        name = functionName,
                        arguments = argsJson
                    )
                )
            )
        }
        
        // Also check for Qwen native tool calls: <tool_call>{"name": "...", "arguments": {...}}</tool_call>
        val qwenToolRegex = "(?s)<tool_call>\\s*(\\{.*?\\})\\s*</tool_call>".toRegex()
        val qwenMatches = qwenToolRegex.findAll(response)
        for (match in qwenMatches) {
            try {
                val jsonStr = match.groupValues[1]
                val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val argsRegex = "\"arguments\"\\s*:\\s*(\\{.*?\\})".toRegex()
                
                val nameMatch = nameRegex.find(jsonStr)
                val argsMatch = argsRegex.find(jsonStr)
                
                if (nameMatch != null) {
                    val functionName = nameMatch.groupValues[1]
                    val argsJson = argsMatch?.groupValues?.get(1) ?: "{}"
                    
                    toolCalls.add(
                        AiToolCall(
                            id = "call_" + kotlin.random.Random.nextInt(10000, 99999),
                            function = AiFunctionCall(
                                name = functionName,
                                arguments = argsJson
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        // Also check for Markdown plaintext tool calls (Qwen hallucination fallback)
        val markdownToolRegex = "(?s)```plaintext\\s+([a-zA-Z0-9_\\-]+)\\s+```".toRegex()
        val markdownMatches = markdownToolRegex.findAll(response)
        for (match in markdownMatches) {
            val functionName = match.groupValues[1].trim()
            // Ensure we don't duplicate if another regex already caught it
            if (toolCalls.none { it.function.name == functionName }) {
                toolCalls.add(
                    AiToolCall(
                        id = "call_" + kotlin.random.Random.nextInt(10000, 99999),
                        function = AiFunctionCall(
                            name = functionName,
                            arguments = "{}"
                        )
                    )
                )
            }
        }
        
        // Remove parsed tool calls from content
        var cleanedContent = response.replace(toolCallRegex, "").trim()
        cleanedContent = cleanedContent.replace(qwenToolRegex, "").trim()
        cleanedContent = cleanedContent.replace(markdownToolRegex, "").trim()
        
        return AiMessage(
            role = "assistant", 
            content = cleanedContent.takeIf { it.isNotEmpty() },
            toolCalls = toolCalls.takeIf { it.isNotEmpty() }
        )
    }
    
    private fun parseKoogArgsToJson(argsRaw: String): String {
        if (argsRaw.isBlank()) return "{}"
        
        val map = mutableMapOf<String, String>()
        // Match key:<|"|>value<|"|> (multiline supported)
        val argRegex = "(?s)([a-zA-Z0-9_]+):<\\|\"\\|>(.*?)<\\|\"\\|>".toRegex()
        val argMatches = argRegex.findAll(argsRaw)
        
        for (argMatch in argMatches) {
            map[argMatch.groupValues[1]] = argMatch.groupValues[2]
        }
        
        // Build JSON manually
        val sb = StringBuilder()
        sb.append("{")
        val entries = map.entries.toList()
        for (i in entries.indices) {
            val entry = entries[i]
            sb.append("\"").append(entry.key).append("\": \"")
            val escapedValue = entry.value.replace("\"", "\\\"").replace("\n", "\\n")
            sb.append(escapedValue).append("\"")
            if (i < entries.size - 1) sb.append(", ")
        }
        sb.append("}")
        return sb.toString()
    }
    
    private fun ensureModelLoaded(config: AiConfig) {
        val modelFileName = config.model
        val dirPath = directoryProvider.getDirectory()
        val absolutePath = "$dirPath/$modelFileName"
        
        if (currentModelPath == absolutePath) {
            return // Already loaded
        }
        
        try {
            val loaded = LlamaBridge.initGenerateModel(absolutePath)
            if (!loaded) {
                throw RuntimeException("Llamatik engine failed to load model from $absolutePath")
            }
            currentModelPath = absolutePath
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize Llamatik context: ${e.message}", e)
        }
    }
    
    private fun formatPrompt(messages: List<AiMessage>, modelName: String, tools: List<AiToolDescriptor>): String {
        val lowerModel = modelName.lowercase()
        val sb = StringBuilder()
        
        var qwenToolsStr = ""
        if (lowerModel.contains("qwen") && tools.isNotEmpty()) {
            val toolsJsonBuilder = StringBuilder()
            toolsJsonBuilder.append("<tools>\n")
            tools.forEach { tool ->
                val paramJson = tool.parameters.toString()
                val desc = tool.description.replace("\"", "\\\"").replace("\n", " ")
                toolsJsonBuilder.append("{\"type\": \"function\", \"function\": {\"name\": \"${tool.name}\", \"description\": \"$desc\", \"parameters\": $paramJson}}\n")
            }
            toolsJsonBuilder.append("</tools>\n")
            toolsJsonBuilder.append("\nFor each function call, return a json object with function name and arguments within <tool_call></tool_call> XML tags:\n")
            toolsJsonBuilder.append("<tool_call>\n{\"name\": \"function_name\", \"arguments\": {\"arg_name\": \"arg_value\"}}\n</tool_call>\n")
            qwenToolsStr = toolsJsonBuilder.toString()
        }
        
        if (lowerModel.contains("gemma")) {
            for (message in messages) {
                val role = if (message.role == "assistant") "model" else "user"
                sb.append("<start_of_turn>${role}\n")
                sb.append("${message.content}\n<end_of_turn>\n")
            }
            sb.append("<start_of_turn>model\n")
        } else if (lowerModel.contains("qwen")) {
            // ChatML format for Qwen
            for (message in messages) {
                sb.append("<|im_start|>${message.role}\n")
                if (message.role == "system" && qwenToolsStr.isNotEmpty()) {
                    sb.append("${message.content}\n\n# Tools\n\nYou may call one or more functions to assist with the user query.\n\nYou are provided with function signatures within <tools></tools> XML tags:\n$qwenToolsStr")
                } else {
                    sb.append("${message.content}")
                }
                sb.append("<|im_end|>\n")
            }
            sb.append("<|im_start|>assistant\n")
        } else if (lowerModel.contains("llama")) {
            // Llama 3 format
            for (message in messages) {
                sb.append("<|start_header_id|>${message.role}<|end_header_id|>\n\n")
                sb.append("${message.content}<|eot_id|>\n")
            }
            sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        } else {
            // Fallback
            for (message in messages) {
                sb.append("### ${message.role.uppercase()}:\n")
                sb.append("${message.content}\n\n")
            }
            sb.append("### ASSISTANT:\n")
        }
        return sb.toString()
    }
}
