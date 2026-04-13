package com.family.tree.core.ai.koog

import com.family.tree.core.ai.koog.KoogAgent.ToolCall

/**
 * Platform-specific tool invoker to handle reflection or manual dispatch.
 */
expect class PlatformToolInvoker() {
    /**
     * Generates a string documentation for the provided tool objects.
     */
    fun getToolDocumentation(tools: List<Any>): String

    /**
     * Invokes a specific tool method on the provided tools.
     */
    suspend fun invokeTool(tools: List<Any>, call: ToolCall): String
}
