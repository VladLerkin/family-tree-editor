package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools

internal actual fun createToolRegistry(tools: GenealogyTools): ToolRegistry = ToolRegistry {
    tools(GenealogyTools::class.asTools(tools))
}
