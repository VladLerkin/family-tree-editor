package com.family.tree.core.ai.koog

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import ai.koog.agents.core.llm.LLMRequest
import ai.koog.agents.core.llm.LLMResponse
import ai.koog.agents.core.model.LLMModel

/**
 * Adapter that bridges our AiClient to Koog's LLMModel.
 */
class KoogModelAdapter(
    private val client: AiClient,
    private val config: AiConfig
) : LLMModel {
    override suspend fun generateResponse(request: LLMRequest): LLMResponse {
        // Simple implementation: concatenate all messages into a single prompt for our AiClient.
        // In a real scenario, this should handle chat messages properly if AiClient supports it.
        val prompt = request.messages.joinToString("\n") { 
            "${it.role}: ${it.content}"
        }
        
        val textResponse = client.sendPrompt(prompt, config)
        
        return LLMResponse(
            content = textResponse,
            toolCalls = emptyList() // Our AiClient doesn't return structured tool calls yet, 
                                    // but we expect Koog to parse them from the text if needed.
        )
    }
}
