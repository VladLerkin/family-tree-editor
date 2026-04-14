package com.family.tree.core.ai.koog

import com.family.tree.core.ai.agent.GenealogyTools
import com.family.tree.core.ai.koog.KoogAgent.ToolCall

/**
 * Wasm/Web implementation.
 * Kotlin/Wasm does not support kotlin-reflect, so we use manual dispatch
 * directly on the known tool classes.
 */
actual class PlatformToolInvoker actual constructor() {
    actual fun getToolDocumentation(tools: List<Any>): String {
        return tools.filterIsInstance<GenealogyTools>().joinToString("\n\n") { _ ->
            """
            Tool Class: GenealogyTools
            Methods:
            - search(name, birth_location?, life_span?, query?, region?, targetSite?): Perform an internet search for genealogy records using Tavily.
            - searchFamilyTree(query): Search for specific individuals or facts in the local family tree by name or location keywords.
            - getGeographicProfile(): Get a summary of all unique geographic locations found in the family tree.
            - readArchiveGuide(fileName): Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').
            - listArchiveGuides(): List all available archive guides for countries and regions.
            - readMethodology(fileName): Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').
            - listMethodologyGuides(): List all available professional methodology workflow guides.
            """.trimIndent()
        }.ifBlank { "No tools available." }
    }

    actual suspend fun invokeTool(tools: List<Any>, call: ToolCall): String {
        val genealogyTool = tools.filterIsInstance<GenealogyTools>().firstOrNull()
            ?: return "Error: GenealogyTools not found."

        return when (call.methodName) {
            "search" -> genealogyTool.search(
                name = call.args["name"] ?: "",
                birth_location = call.args["birth_location"],
                life_span = call.args["life_span"],
                query = call.args["query"],
                region = call.args["region"],
                targetSite = call.args["targetSite"]
            )
            "searchFamilyTree" -> genealogyTool.searchFamilyTree(
                query = call.args["query"] ?: return "Error: 'query' argument required."
            )
            "getGeographicProfile" -> genealogyTool.getGeographicProfile()
            "readArchiveGuide" -> genealogyTool.readArchiveGuide(
                fileName = call.args["fileName"] ?: return "Error: 'fileName' argument required."
            )
            "listArchiveGuides" -> genealogyTool.listArchiveGuides()
            "readMethodology" -> genealogyTool.readMethodology(
                fileName = call.args["fileName"] ?: return "Error: 'fileName' argument required."
            )
            "listMethodologyGuides" -> genealogyTool.listMethodologyGuides()
            else -> "Error: Unknown method '${call.methodName}'."
        }
    }
}
