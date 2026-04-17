package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

/**
 * Common arguments for tools that take a single fileName.
 */
@Serializable
data class FileNameArgs(val fileName: String) : ToolArgs

/**
 * Arguments for search tool.
 */
@Serializable
data class SearchArgs(
    val name: String = "",
    val birth_location: String? = null,
    val life_span: String? = null,
    val query: String? = null,
    val region: String? = null,
    val targetSite: String? = null
) : ToolArgs

/**
 * Arguments for family tree search.
 */
@Serializable
data class QueryArgs(val query: String) : ToolArgs

// --- CLASS BASED TOOLS ---

class ReadMethodologyTool(private val tools: GenealogyTools) : Tool<FileNameArgs, String>(
    FileNameArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "readMethodology",
        description = "Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').",
        requiredParameters = listOf(
            ToolParameterDescriptor("fileName", "The filename of the methodology guide", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: FileNameArgs): String = tools.readMethodology(args.fileName)
}

class ReadArchiveGuideTool(private val tools: GenealogyTools) : Tool<FileNameArgs, String>(
    FileNameArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "readArchiveGuide",
        description = "Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').",
        requiredParameters = listOf(
            ToolParameterDescriptor("fileName", "The filename of the archive guide", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: FileNameArgs): String = tools.readArchiveGuide(args.fileName)
}

class ListArchiveGuidesTool(private val tools: GenealogyTools) : Tool<ToolArgs.Empty, String>(
    ToolArgs.Empty.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "listArchiveGuides",
        description = "List all available archive guides for countries and regions."
    )
) {
    override suspend fun execute(args: ToolArgs.Empty): String = tools.listArchiveGuides()
}

class ListMethodologyGuidesTool(private val tools: GenealogyTools) : Tool<ToolArgs.Empty, String>(
    ToolArgs.Empty.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "listMethodologyGuides",
        description = "List all available professional methodology workflow guides."
    )
) {
    override suspend fun execute(args: ToolArgs.Empty): String = tools.listMethodologyGuides()
}

class ListExamplesTool(private val tools: GenealogyTools) : Tool<ToolArgs.Empty, String>(
    ToolArgs.Empty.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "listExamples",
        description = "List all available research examples showing how to apply methodology."
    )
) {
    override suspend fun execute(args: ToolArgs.Empty): String = tools.listExamples()
}

class ReadExampleTool(private val tools: GenealogyTools) : Tool<FileNameArgs, String>(
    FileNameArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "readExample",
        description = "Read a specific research example (e.g., 'tree-expansion-session.md') to see professional protocols in action.",
        requiredParameters = listOf(
            ToolParameterDescriptor("fileName", "The filename of the research example", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: FileNameArgs): String = tools.readExample(args.fileName)
}

class ListReferenceGuidesTool(private val tools: GenealogyTools) : Tool<ToolArgs.Empty, String>(
    ToolArgs.Empty.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "listReferenceGuides",
        description = "List all available reference guides (standard conventions, glossaries)."
    )
) {
    override suspend fun execute(args: ToolArgs.Empty): String = tools.listReferenceGuides()
}

class ReadReferenceGuideTool(private val tools: GenealogyTools) : Tool<FileNameArgs, String>(
    FileNameArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "readReferenceGuide",
        description = "Read a specific reference guide (e.g., 'gedcom-guide.md', 'confidence-tiers.md').",
        requiredParameters = listOf(
            ToolParameterDescriptor("fileName", "The filename of the reference guide", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: FileNameArgs): String = tools.readReferenceGuide(args.fileName)
}

class GetGeographicProfileTool(private val tools: GenealogyTools) : Tool<ToolArgs.Empty, String>(
    ToolArgs.Empty.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "getGeographicProfile",
        description = "Get a summary of all unique geographic locations found in the family tree."
    )
) {
    override suspend fun execute(args: ToolArgs.Empty): String = tools.getGeographicProfile()
}

class SearchFamilyTreeTool(private val tools: GenealogyTools) : Tool<QueryArgs, String>(
    QueryArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "searchFamilyTree",
        description = "Search for specific individuals or facts in the local family tree.",
        requiredParameters = listOf(
            ToolParameterDescriptor("query", "The search query (name, date, or place)", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: QueryArgs): String = tools.searchFamilyTree(args.query)
}

class TavilySearchTool(private val tools: GenealogyTools) : Tool<SearchArgs, String>(
    SearchArgs.serializer(),
    String.serializer(),
    ToolDescriptor(
        name = "search",
        description = "Perform an internet search for genealogy records using Tavily.",
        optionalParameters = listOf(
            ToolParameterDescriptor("name", "Name of the person", ToolParameterType.String),
            ToolParameterDescriptor("birth_location", "Known birth location", ToolParameterType.String),
            ToolParameterDescriptor("life_span", "Approximate years (e.g. 1890-1950)", ToolParameterType.String),
            ToolParameterDescriptor("query", "Specific search query", ToolParameterType.String),
            ToolParameterDescriptor("region", "Geographic region for precision", ToolParameterType.String),
            ToolParameterDescriptor("targetSite", "High-precision site to search (e.g. 'vgd.ru')", ToolParameterType.String)
        )
    )
) {
    override suspend fun execute(args: SearchArgs): String = tools.search(
        name = args.name,
        birth_location = args.birth_location,
        life_span = args.life_span,
        query = args.query,
        region = args.region,
        targetSite = args.targetSite
    )
}

/**
 * Extension to provide class-based tools for non-JVM platforms.
 */
fun GenealogyTools.getClassBasedTools(): List<Tool<*, *>> = listOf(
    ReadMethodologyTool(this),
    ReadArchiveGuideTool(this),
    ListArchiveGuidesTool(this),
    ListMethodologyGuidesTool(this),
    ListExamplesTool(this),
    ReadExampleTool(this),
    ListReferenceGuidesTool(this),
    ReadReferenceGuideTool(this),
    GetGeographicProfileTool(this),
    SearchFamilyTreeTool(this),
    TavilySearchTool(this)
)
