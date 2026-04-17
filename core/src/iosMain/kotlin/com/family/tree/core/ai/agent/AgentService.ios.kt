package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.ToolRegistry

internal actual fun createToolRegistry(tools: GenealogyTools): ToolRegistry = ToolRegistry {
    // Using class-based tools for iOS since reflection-based tool registration is JVM-only in Koog 0.8.0.
    tools.getClassBasedTools().forEach { tool(it) }
}
