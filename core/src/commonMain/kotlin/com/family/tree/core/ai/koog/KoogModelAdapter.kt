package com.family.tree.core.ai.koog

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import com.family.tree.core.ai.AiMessage
import com.family.tree.core.ai.AiToolCall
import com.family.tree.core.ai.AiFunctionCall
import com.family.tree.core.ai.AiToolDescriptor
import kotlin.time.Instant
import ai.koog.prompt.Prompt
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
 * Updated for Koog 1.0.0 with structured tool calling and response parts.
 */
class KoogModelAdapter(
    private val client: AiClient,
    private val config: AiConfig,
    private val onLog: (String) -> Unit = {}
) : PromptExecutor() {
    
    private fun sanitizeToolName(name: String): String {
        if (name.length <= 64 && name.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
            return name
        }
        val clean = name.filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .take(45)
            .trim { it == '_' || it == '-' }
        val hash = (name.hashCode().toLong() and 0xFFFFFFFFL).toString(36)
        val result = "${clean}_$hash"
        val finalResult = if (result.firstOrNull()?.isLetterOrDigit() == true) result else "tool_$result"
        return finalResult.take(64)
    }
    
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Message.Assistant {
        onLog("[AI-DEBUG] Building prompt from ${prompt.messages.size} messages...")
        prompt.messages.forEachIndexed { index, msg ->
            onLog("  Koog Message $index: class=${msg::class.simpleName}")
            when (msg) {
                is Message.System -> {
                    val texts = msg.parts.filterIsInstance<MessagePart.Text>()
                    onLog("    System parts: textCount=${texts.size}")
                }
                is Message.User -> {
                    val texts = msg.parts.filterIsInstance<MessagePart.Text>()
                    val results = msg.parts.filterIsInstance<MessagePart.Tool.Result>()
                    onLog("    User parts: textCount=${texts.size}, toolResultCount=${results.size}")
                    results.forEach { res ->
                        onLog("      ToolResult: id='${res.id}', tool='${res.tool}', isError=${res.isError}, outputLength=${res.output.length}")
                    }
                }
                is Message.Assistant -> {
                    val texts = msg.parts.filterIsInstance<MessagePart.Text>()
                    val calls = msg.parts.filterIsInstance<MessagePart.Tool.Call>()
                    onLog("    Assistant parts: textCount=${texts.size}, toolCallCount=${calls.size}")
                    calls.forEach { call ->
                        onLog("      ToolCall: id='${call.id}', tool='${call.tool}', argsLength=${call.args.length}")
                    }
                }
            }
        }
        
        // Map Koog messages to AiMessage
        val aiMessages = mutableListOf<AiMessage>()
        prompt.messages.forEach { message: Message ->
            when (message) {
                is Message.System -> {
                    val content = message.parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    aiMessages.add(AiMessage(
                        role = "system",
                        content = content
                    ))
                }
                is Message.User -> {
                    val content = message.parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    
                    // In Koog 1.0.0, tool results reside inside Message.User's parts list
                    val toolResults = message.parts.filterIsInstance<MessagePart.Tool.Result>()
                    if (toolResults.isNotEmpty()) {
                        toolResults.forEach { result ->
                            aiMessages.add(AiMessage(
                                role = "tool",
                                content = result.output,
                                toolCallId = result.id
                            ))
                        }
                    }
                    
                    // Add standard user message if there is text content or if there are no tool results at all
                    if (content != null || toolResults.isEmpty()) {
                        aiMessages.add(AiMessage(
                            role = "user",
                            content = content
                        ))
                    }
                }
                is Message.Assistant -> {
                    val content = message.parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }.takeIf { it.isNotEmpty() }
                    val toolCalls = message.parts.filterIsInstance<MessagePart.Tool.Call>().map { call ->
                        val sanitizedName = sanitizeToolName(call.tool)
                        AiToolCall(
                            id = call.id ?: "",
                            function = AiFunctionCall(
                                name = sanitizedName,
                                arguments = call.args
                            )
                        )
                    }.takeIf { it.isNotEmpty() }
                    
                    aiMessages.add(AiMessage(
                        role = "assistant",
                        content = content,
                        toolCalls = toolCalls
                    ))
                }
            }
        }
        
        // Map Koog ToolDescriptors to AiToolDescriptor by building JSON Schema
        val toolNames = tools.map { it.name }
        val sanitizedToolNames = tools.map { sanitizeToolName(it.name) }
        onLog("[AI-DEBUG] Tool descriptors passed: $toolNames (sanitized: $sanitizedToolNames)")
        
        val sanitizedToOriginal = tools.associateBy({ sanitizeToolName(it.name) }, { it.name })
        
        val aiTools = tools.map { tool: ToolDescriptor ->
            AiToolDescriptor(
                name = sanitizeToolName(tool.name),
                description = tool.description,
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        (tool.requiredParameters + tool.optionalParameters).forEach { param ->
                            putJsonObject(param.name) {
                                val jsonType = when (param.type) {
                                    ToolParameterType.String -> "string"
                                    ToolParameterType.Boolean -> "boolean"
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
        
        val resultParts = mutableListOf<MessagePart.ResponsePart>()
        
        // 1. Text content
        aiResponse.content?.let { 
            if (it.isNotBlank()) {
                resultParts.add(MessagePart.Text(it)) 
            }
        }
        
        // 2. Tool calls (in Koog 1.0.0, we emit all of them as ResponseParts)
        if (!aiResponse.toolCalls.isNullOrEmpty()) {
            aiResponse.toolCalls.forEach { call ->
                val originalName = sanitizedToOriginal[call.function.name] ?: call.function.name
                resultParts.add(
                    MessagePart.Tool.Call(
                        id = call.id,
                        tool = originalName,
                        args = call.function.arguments
                    )
                )
            }
        }
        
        return Message.Assistant(
            parts = resultParts,
            metaInfo = ResponseMetaInfo(
                timestamp = Instant.fromEpochMilliseconds(0),
                totalTokensCount = 0,
                inputTokensCount = 0,
                outputTokensCount = 0
            )
        )
    }

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Assistant> {
        return listOf(execute(prompt, model, tools))
    }

    override suspend fun models(): List<LLModel> = listOf(LLModel(LLMProvider.OpenAI, "default"))

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult {
        return ModerationResult(isHarmful = false, categories = emptyMap())
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> {
        return emptyFlow()
    }

    override fun close() {
        // Cleanup if needed
    }
}
