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
                contextLength = 4096,
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
        val formattedPrompt = formatPrompt(messages, config.model)
        val response = sendPrompt(formattedPrompt, config)
        return AiMessage(role = "assistant", content = response)
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
    
    private fun formatPrompt(messages: List<AiMessage>, modelName: String): String {
        val lowerModel = modelName.lowercase()
        val sb = StringBuilder()
        
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
                sb.append("${message.content}<|im_end|>\n")
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
