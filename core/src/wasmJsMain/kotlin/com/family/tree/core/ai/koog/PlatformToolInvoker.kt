package com.family.tree.core.ai.koog

import com.family.tree.core.ai.koog.KoogAgent.ToolCall

/**
 * Web stub implementation.
 */
actual class PlatformToolInvoker actual constructor() {
    actual fun getToolDocumentation(tools: List<Any>): String {
        return "Tool documentation not supported on this platform."
    }

    actual suspend fun invokeTool(tools: List<Any>, call: ToolCall): String {
        return "Tool invocation not supported on this platform."
    }
}
