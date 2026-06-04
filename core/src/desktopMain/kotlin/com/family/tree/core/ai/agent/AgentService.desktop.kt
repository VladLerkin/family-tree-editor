package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.ToolRegistry

internal actual fun createToolRegistry(tools: GenealogyTools): ToolRegistry = ToolRegistry {
    tools.getClassBasedTools().forEach { tool(it) }
}
