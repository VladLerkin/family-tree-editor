package com.family.tree.core.ai.koog

import com.family.tree.core.ai.AiClient
import com.family.tree.core.ai.AiConfig

/**
 * Koog (Kotlin Object-Oriented generation) API Shim/Wrapper
 * This allows the application to build agentic workflows using Koog concepts.
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tool(val description: String)

interface AgentModel {
    suspend fun generateResponse(prompt: String): String
}

class KoogAgent(
    private val name: String,
    private val instructions: String,
    private val tools: List<Any>,
    private val model: AgentModel,
    private val maxIterations: Int = 10,
    private val onLog: (String) -> Unit = {}
) {
    suspend fun execute(task: String): String {
        val history = mutableListOf<String>()
        history.add("User Task: $task")
        
        val invoker = PlatformToolInvoker()
        val toolDocs = invoker.getToolDocumentation(tools)

        var iteration = 0
        var lastAiOutput = ""

        while (iteration < maxIterations) {
            iteration++
            onLog("Iteration $iteration: Thinking...")
            
            val fullPrompt = buildString {
                appendLine("You are $name.")
                appendLine(instructions)
                appendLine("\n### Available Tools")
                appendLine(toolDocs)
                appendLine("\n### Tool Call Protocol")
                appendLine("If you need to use a tool, output a block like this:")
                appendLine("```tool_code")
                appendLine("methodName(arg1=\"value\", ...)")
                appendLine("```")
                appendLine("CRITICAL: You MUST provide all required arguments. Empty calls like `methodName({})` or `methodName()` for tools that require parameters are strictly FORBIDDEN.")
                appendLine("I will execute it and provide the result. You can omit the class prefix. If you have the final answer, just provide it WITHOUT a tool block.")
                
                appendLine("\n### Conversation History")
                history.forEach { appendLine(it) }
                
                if (lastAiOutput.isNotEmpty()) {
                    appendLine("\nLast Response: $lastAiOutput")
                }
                appendLine("\nAssistant:")
            }

            val response = model.generateResponse(fullPrompt)
            lastAiOutput = response
            history.add("Assistant: $response")
            onLog("Agent Thought: ${response.take(100)}...")

            val toolCall = parseToolCall(response)
            if (toolCall == null) {
                // No more tool calls, we are done
                onLog("Agent finished without further tool calls.")
                return response
            }

            // Execute tool via platform invoker
            onLog("⚙️ [INVOKE] ${toolCall.className ?: "*"}.${toolCall.methodName}(${toolCall.args})")
            val result = invoker.invokeTool(tools, toolCall)
            val preview = if (result.length > 500) result.take(500) + "\n...[+${result.length - 500} chars]" else result
            onLog("✅ [RESULT] ${toolCall.methodName} →\n$preview")
            history.add("System (Tool Result): $result")
        }

        return lastAiOutput
    }

    private fun parseToolCall(text: String): ToolCall? {
        // More robust regex: allows optional class prefix like Class.method() or just method()
        val regex = "(?s)```tool_code\\s+(?:([\\w\\d]+)\\.)?([\\w\\d]+)\\((.*)\\)\\s+```".toRegex()
        val match = regex.find(text) ?: return null
        
        val className = match.groupValues[1] // Might be empty if no dot
        val methodName = match.groupValues[2]
        val argsText = match.groupValues[3]
        
        // Quoted-string-aware argument parser:
        // Splits on commas that are NOT inside quotes, so values like name="Иванов, Иван" work correctly.
        val args = if (argsText.isBlank()) {
            emptyMap()
        } else {
            parseToolArgs(argsText)
        }
            
        return ToolCall(className.ifBlank { null }, methodName, args)
    }

    data class ToolCall(val className: String?, val methodName: String, val args: Map<String, String>)
}

/**
 * Parses tool call arguments that are comma-separated key="value" pairs,
 * correctly handling commas inside quoted strings.
 * E.g.: name="Иванов, Иван", region="Тверская губерния"
 */
private fun parseToolArgs(argsText: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var inQuote = false
    var quoteChar = ' '

    for (ch in argsText) {
        when {
            inQuote && ch == quoteChar -> { inQuote = false; current.append(ch) }
            !inQuote && (ch == '"' || ch == '\'') -> { inQuote = true; quoteChar = ch; current.append(ch) }
            !inQuote && ch == ',' -> { tokens.add(current.toString()); current.clear() }
            else -> current.append(ch)
        }
    }
    if (current.isNotBlank()) tokens.add(current.toString())

    for (token in tokens) {
        val eqIdx = token.indexOf('=')
        if (eqIdx < 0) continue
        val key = token.substring(0, eqIdx).trim()
        val value = token.substring(eqIdx + 1).trim()
            .removeSurrounding("\"").removeSurrounding("'")
        if (key.isNotBlank()) result[key] = value
    }
    return result
}

class AiClientAgentModel(
    private val client: AiClient,
    private val config: AiConfig
) : AgentModel {
    override suspend fun generateResponse(prompt: String): String {
        return client.sendPrompt(prompt, config)
    }
}
