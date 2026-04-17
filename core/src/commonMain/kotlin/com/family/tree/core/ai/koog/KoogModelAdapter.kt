package com.family.tree.core.ai.koog

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.agents.core.tools.ToolDescriptor
import kotlin.time.Instant
import ai.koog.prompt.dsl.Prompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Adapter that bridges our AiClient to Koog's PromptExecutor.
 * Updated for Koog 0.8.0.
 */
class KoogModelAdapter(
    private val client: AiClient,
    private val config: AiConfig,
    private val onLog: (String) -> Unit = {}
) : PromptExecutor() {
    
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> {
        onLog("[AI-DEBUG] Building prompt from ${prompt.messages.size} messages...")
        
        // Build a combined prompt string from all messages in the Prompt object
        val promptText = prompt.messages.joinToString("\n") { message ->
            val roles = when (message) {
                is Message.System -> "System: "
                is Message.User -> "User: "
                is Message.Assistant -> "Assistant: "
                else -> ""
            }
            val text = message.parts
                .filterIsInstance<ContentPart.Text>()
                .joinToString("") { it.text }
            roles + text
        }
        
        onLog("[AI-DEBUG] Sending prompt to ${config.provider} (Length: ${promptText.length})")
        if (promptText.length > 500) {
            onLog("[AI-DEBUG] Prompt snippet: ${promptText.take(250)}...[TRUNCATED]...${promptText.takeLast(250)}")
        } else {
            onLog("[AI-DEBUG] Full prompt: $promptText")
        }
        
        val textResponse = client.sendPrompt(promptText, config)
        
        onLog("[AI-DEBUG] Received response (Length: ${textResponse.length})")
        onLog("[AI-DEBUG] Response snippet: ${textResponse.take(200)}")
        
        return listOf(
            Message.Assistant(
                parts = listOf(ContentPart.Text(textResponse)),
                metaInfo = ResponseMetaInfo(
                    timestamp = Instant.fromEpochMilliseconds(0),
                    totalTokensCount = 0,
                    inputTokensCount = 0,
                    outputTokensCount = 0
                )
            )
        )
    }

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<List<Message.Response>> {
        return listOf(execute(prompt, model, tools))
    }

    override suspend fun models(): List<LLModel> = listOf(LLModel(LLMProvider.OpenAI, "default"))

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        // Basic implementation, moderation not yet supported by our client
        // Returning a successful moderation result
        return ModerationResult(isHarmful = false, categories = emptyMap())
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> {
        // Streaming not yet implemented in our AiClient
        return emptyFlow()
    }

    override fun close() {
        // Cleanup if needed
    }
}
