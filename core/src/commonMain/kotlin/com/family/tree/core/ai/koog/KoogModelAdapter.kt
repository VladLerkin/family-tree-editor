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
import ai.koog.agents.core.tools.ToolParameterType
import com.family.tree.core.ai.AiMessage
import com.family.tree.core.ai.AiToolCall
import com.family.tree.core.ai.AiFunctionCall
import com.family.tree.core.ai.AiToolDescriptor
import kotlin.time.Instant
import ai.koog.prompt.dsl.Prompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonPrimitive

/**
 * Adapter that bridges our AiClient to Koog's PromptExecutor.
 * Updated for Koog 0.8.0 with structured tool calling support.
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
        prompt.messages.forEachIndexed { i, m -> onLog("[AI-DEBUG]   Msg $i: ${m::class.simpleName}") }
        
        // Map Koog messages to AiMessage
        val aiMessages = mutableListOf<AiMessage>()
        prompt.messages.forEach { message: Message ->
            when (message) {
                is Message.System -> {
                    aiMessages.add(AiMessage(
                        role = "system",
                        content = message.parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    ))
                }
                is Message.User -> {
                    val content = message.parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    
                    val prev = aiMessages.lastOrNull()
                    if (prev != null && prev.role == "assistant" && !prev.toolCalls.isNullOrEmpty()) {
                        // Workaround: If Koog appends a User message (e.g., error string) directly after a Tool call,
                        // OpenAI will crash because it expects a Tool response.
                        aiMessages.add(AiMessage(
                            role = "tool",
                            content = content ?: "Execution failed",
                            toolCallId = prev.toolCalls.first().id
                        ))
                    } else {
                        aiMessages.add(AiMessage(
                            role = "user",
                            content = content
                        ))
                    }
                }
                is Message.Assistant -> {
                    val content = message.parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    val toolCalls = message.parts.filterIsInstance<Message.Tool.Call>().map { call ->
                        AiToolCall(
                            id = call.id ?: "",
                            function = AiFunctionCall(
                                name = call.tool,
                                arguments = call.content
                            )
                        )
                    }.takeIf { it.isNotEmpty() }
                    
                    aiMessages.add(AiMessage(
                        role = "assistant",
                        content = content,
                        toolCalls = toolCalls
                    ))
                }
                is Message.Tool.Result -> {
                    val content = message.parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() } ?: "Empty execution result"
                    
                    // Fallback to the last assistant's tool call ID if Koog omitted it (happens on execution failure)
                    val actualToolCallId = message.id ?: run {
                        val prevAsst = aiMessages.lastOrNull { it.role == "assistant" && !it.toolCalls.isNullOrEmpty() }
                        prevAsst?.toolCalls?.firstOrNull()?.id ?: ""
                    }
                    
                    aiMessages.add(AiMessage(
                        role = "tool",
                        content = content,
                        toolCallId = actualToolCallId
                    ))
                }
                is Message.Tool.Call -> {
                    val aiToolCall = AiToolCall(
                        id = message.id ?: "",
                        function = AiFunctionCall(
                            name = message.tool,
                            arguments = message.content
                        )
                    )
                    // Group with the preceding assistant message if possible (to satisfy OpenAI requirements of a single assistant message having tool_calls)
                    val last = aiMessages.lastOrNull()
                    if (last != null && last.role == "assistant") {
                        val newToolCalls = (last.toolCalls ?: emptyList()) + aiToolCall
                        aiMessages[aiMessages.size - 1] = last.copy(toolCalls = newToolCalls)
                    } else {
                        aiMessages.add(AiMessage(
                            role = "assistant",
                            content = null,
                            toolCalls = listOf(aiToolCall)
                        ))
                    }
                }
                else -> {
                    val content = message.parts.filterIsInstance<ContentPart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    
                    val prev = aiMessages.lastOrNull()
                    if (prev != null && prev.role == "assistant" && !prev.toolCalls.isNullOrEmpty()) {
                        aiMessages.add(AiMessage(
                            role = "tool",
                            content = content ?: "Execution failed",
                            toolCallId = prev.toolCalls.first().id
                        ))
                    } else {
                        aiMessages.add(AiMessage(
                            role = "user",
                            content = content
                        ))
                    }
                }
            }
        }
        
        // Map Koog ToolDescriptors to AiToolDescriptor by building JSON Schema
        val aiTools = tools.map { tool: ToolDescriptor ->
            AiToolDescriptor(
                name = tool.name,
                description = tool.description,
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        (tool.requiredParameters + tool.optionalParameters).forEach { param ->
                            putJsonObject(param.name) {
                                val jsonType = when (param.type) {
                                    ToolParameterType.String -> "string"
                                    else -> "string" 
                                }
                                put("type", jsonType)
                                put("description", param.description)
                            }
                        }
                    }
                    if (tool.requiredParameters.isNotEmpty()) {
                        putJsonArray("required") {
                            tool.requiredParameters.forEach { add(JsonPrimitive(it.name)) }
                        }
                    }
                }
            )
        }
        
        val totalChars = aiMessages.sumOf { it.content?.length ?: 0 }
        onLog("[AI-DEBUG] Sending prompt to ${config.provider} (Messages: ${aiMessages.size}, Tool Defs: ${aiTools.size}, Total Content Chars: $totalChars)")
        
        val aiResponse = client.sendChat(aiMessages, config, aiTools)
        
        onLog("[AI-DEBUG] Received response (Length: ${aiResponse.content?.length ?: 0}, Tool Calls: ${aiResponse.toolCalls?.size ?: 0})")
        if (aiResponse.content != null) {
            onLog("[AI-DEBUG] Response snippet: ${aiResponse.content.take(200)}")
        }
        
        val resultArr = mutableListOf<Message.Response>()
        
        // In Koog 0.8.0, the graph engine might only route the FIRST tool call.
        // If we emit multiple tool calls to the prompt history but only execute one,
        // it leaves unresolved tool calls in the history, triggering OpenAI API errors
        // ("An assistant message with 'tool_calls' must be followed by tool messages").
        // To fix this cleanly, we force sequential tool execution by returning ONLY the first tool call.
        if (!aiResponse.toolCalls.isNullOrEmpty()) {
            val call = aiResponse.toolCalls.first()
            resultArr.add(
                Message.Tool.Call(
                    id = call.id,
                    tool = call.function.name,
                    content = call.function.arguments,
                    metaInfo = ResponseMetaInfo(
                        timestamp = Instant.fromEpochMilliseconds(0),
                        totalTokensCount = 0,
                        inputTokensCount = 0,
                        outputTokensCount = 0
                    )
                )
            )
        } else {
            // Add Assistant message for text content
            val textParts = mutableListOf<ContentPart>()
            aiResponse.content?.let { 
                if (it.isNotBlank()) {
                    textParts.add(ContentPart.Text(it)) 
                }
            }
            
            resultArr.add(
                Message.Assistant(
                    parts = textParts,
                    metaInfo = ResponseMetaInfo(
                        timestamp = Instant.fromEpochMilliseconds(0),
                        totalTokensCount = 0,
                        inputTokensCount = 0,
                        outputTokensCount = 0
                    )
                )
            )
        }
        
        return resultArr
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
