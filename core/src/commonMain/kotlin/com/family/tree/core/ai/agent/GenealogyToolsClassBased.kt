package com.family.tree.core.ai.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.typeToken
import kotlinx.serialization.Serializable

/** Пустые аргументы для инструментов без параметров. */
@Serializable class GenealogyEmptyArgs

/** Common arguments for tools that take a single fileName. */
@Serializable data class FileNameArgs(val fileName: String)

/** Arguments for search tool. */
@Serializable
data class SearchArgs(
        val name: String = "",
        val birth_location: String? = null,
        val life_span: String? = null,
        val query: String? = null,
        val region: String? = null,
        val targetSite: String? = null
)

/** Arguments for family tree search. */
@Serializable data class QueryArgs(val query: String)

// --- CLASS BASED TOOLS ---

class ReadMethodologyTool(private val tools: GenealogyTools) :
        Tool<FileNameArgs, String>(
                argsType = typeToken<FileNameArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "readMethodology",
                                description =
                                        "Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "fileName",
                                                        "The filename of the methodology guide",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: FileNameArgs): String =
                tools.readMethodology(args.fileName)
}

class ReadArchiveGuideTool(private val tools: GenealogyTools) :
        Tool<FileNameArgs, String>(
                argsType = typeToken<FileNameArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "readArchiveGuide",
                                description =
                                        "Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "fileName",
                                                        "The filename of the archive guide",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: FileNameArgs): String =
                tools.readArchiveGuide(args.fileName)
}

class ListArchiveGuidesTool(private val tools: GenealogyTools) :
        Tool<GenealogyEmptyArgs, String>(
                argsType = typeToken<GenealogyEmptyArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "listArchiveGuides",
                                description =
                                        "List all available archive guides for countries and regions."
                        )
        ) {
        override suspend fun execute(args: GenealogyEmptyArgs): String = tools.listArchiveGuides()
}

class ListMethodologyGuidesTool(private val tools: GenealogyTools) :
        Tool<GenealogyEmptyArgs, String>(
                argsType = typeToken<GenealogyEmptyArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "listMethodologyGuides",
                                description =
                                        "List all available professional methodology workflow guides."
                        )
        ) {
        override suspend fun execute(args: GenealogyEmptyArgs): String =
                tools.listMethodologyGuides()
}

class ListExamplesTool(private val tools: GenealogyTools) :
        Tool<GenealogyEmptyArgs, String>(
                argsType = typeToken<GenealogyEmptyArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "listExamples",
                                description =
                                        "List all available research examples showing how to apply methodology."
                        )
        ) {
        override suspend fun execute(args: GenealogyEmptyArgs): String = tools.listExamples()
}

class ReadExampleTool(private val tools: GenealogyTools) :
        Tool<FileNameArgs, String>(
                argsType = typeToken<FileNameArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "readExample",
                                description =
                                        "Read a specific research example (e.g., 'tree-expansion-session.md') to see professional protocols in action.",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "fileName",
                                                        "The filename of the research example",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: FileNameArgs): String = tools.readExample(args.fileName)
}

class ListReferenceGuidesTool(private val tools: GenealogyTools) :
        Tool<GenealogyEmptyArgs, String>(
                argsType = typeToken<GenealogyEmptyArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "listReferenceGuides",
                                description =
                                        "List all available reference guides (standard conventions, glossaries)."
                        )
        ) {
        override suspend fun execute(args: GenealogyEmptyArgs): String = tools.listReferenceGuides()
}

class ReadReferenceGuideTool(private val tools: GenealogyTools) :
        Tool<FileNameArgs, String>(
                argsType = typeToken<FileNameArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "readReferenceGuide",
                                description =
                                        "Read a specific reference guide (e.g., 'gedcom-guide.md', 'confidence-tiers.md').",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "fileName",
                                                        "The filename of the reference guide",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: FileNameArgs): String =
                tools.readReferenceGuide(args.fileName)
}

class GetGeographicProfileTool(private val tools: GenealogyTools) :
        Tool<GenealogyEmptyArgs, String>(
                argsType = typeToken<GenealogyEmptyArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "getGeographicProfile",
                                description =
                                        "Get a summary of all unique geographic locations found in the family tree."
                        )
        ) {
        override suspend fun execute(args: GenealogyEmptyArgs): String =
                tools.getGeographicProfile()
}

class SearchFamilyTreeTool(private val tools: GenealogyTools) :
        Tool<QueryArgs, String>(
                argsType = typeToken<QueryArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "searchFamilyTree",
                                description =
                                        "Search for specific individuals or facts in the local family tree.",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "query",
                                                        "The search query (name, date, or place)",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: QueryArgs): String = tools.searchFamilyTree(args.query)
}

class GeneralWebSearchTool(private val tools: GenealogyTools) :
        Tool<SearchArgs, String>(
                argsType = typeToken<SearchArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "generalWebSearch",
                                description =
                                        "Perform an internet search for historical context, geographical questions, or open forums (e.g. 'vgd.ru'). NOT for direct database searches.",
                                optionalParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "name",
                                                        "Name of the person",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "birth_location",
                                                        "Known birth location",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "life_span",
                                                        "Approximate years (e.g. 1890-1950)",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "query",
                                                        "Specific search query",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "region",
                                                        "Geographic region for precision",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "targetSite",
                                                        "High-precision site to search (e.g. 'vgd.ru')",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: SearchArgs): String =
                tools.generalWebSearch(
                        name = args.name,
                        birth_location = args.birth_location,
                        life_span = args.life_span,
                        query = args.query,
                        region = args.region,
                        targetSite = args.targetSite
                )
}

@Serializable
data class PamyatNarodaSearchArgs(
        val firstName: String,
        val lastName: String,
        val patronymic: String? = null,
        val birthYear: String? = null
)

class PamyatNarodaSearchTool(private val tools: GenealogyTools) :
        Tool<PamyatNarodaSearchArgs, String>(
                argsType = typeToken<PamyatNarodaSearchArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "searchPamyatNaroda",
                                description =
                                        "Search the 'Pamyat Naroda' WWII database for Soviet military records. Target individuals born ~1880-1930 who could have served in 1941-1945.",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "firstName",
                                                        "Given name of the person",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "lastName",
                                                        "Surname of the person",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "patronymic",
                                                        "Patronymic (отчество) - separate field",
                                                        ToolParameterType.String
                                                )
                                        ),
                                optionalParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "birthYear",
                                                        "Year of birth",
                                                        ToolParameterType.String
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: PamyatNarodaSearchArgs): String =
                tools.searchPamyatNaroda(
                        firstName = args.firstName,
                        lastName = args.lastName,
                        patronymic = args.patronymic,
                        birthYear = args.birthYear
                )
}

@Serializable
data class FamilySearchSearchArgs(
        val firstName: String,
        val lastName: String,
        val birthYear: String? = null,
        val birthPlace: String? = null,
        val deathYear: String? = null,
        val deathPlace: String? = null,
        val gender: String? = null,
        val exactMatch: Boolean = false
)

class FamilySearchQueryTool(private val tools: GenealogyTools) :
        Tool<FamilySearchSearchArgs, String>(
                argsType = typeToken<FamilySearchSearchArgs>(),
                resultType = typeToken<String>(),
                descriptor =
                        ToolDescriptor(
                                name = "queryFamilySearch",
                                description =
                                        "Search FamilySearch. Automatically queries BOTH Historical Records and Family Tree.",
                                requiredParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "firstName",
                                                        "Given name",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "lastName",
                                                        "Surname",
                                                        ToolParameterType.String
                                                )
                                        ),
                                optionalParameters =
                                        listOf(
                                                ToolParameterDescriptor(
                                                        "birthYear",
                                                        "Year of birth",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "birthPlace",
                                                        "Place of birth",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "deathYear",
                                                        "Year of death",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "deathPlace",
                                                        "Place of death",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "gender",
                                                        "Optional gender: 'Male' or 'Female'",
                                                        ToolParameterType.String
                                                ),
                                                ToolParameterDescriptor(
                                                        "exactMatch",
                                                        "Set to true for exact matching",
                                                        ToolParameterType.Boolean
                                                )
                                        )
                        )
        ) {
        override suspend fun execute(args: FamilySearchSearchArgs): String {
                return tools.queryFamilySearch(
                        firstName = args.firstName,
                        lastName = args.lastName,
                        birthYear = args.birthYear,
                        birthPlace = args.birthPlace,
                        deathYear = args.deathYear,
                        deathPlace = args.deathPlace,
                        gender = args.gender,
                        exactMatch = args.exactMatch
                )
        }
}

/** Extension to provide class-based tools for non-JVM platforms. */
fun GenealogyTools.getClassBasedTools(): List<Tool<*, *>> =
        listOf(
                ReadMethodologyTool(this),
                ReadArchiveGuideTool(this),
                ListArchiveGuidesTool(this),
                ListMethodologyGuidesTool(this),
                ListExamplesTool(this),
                ReadExampleTool(this),
                ListReferenceGuidesTool(this),
                ReadReferenceGuideTool(this),
                GetGeographicProfileTool(this),
                PamyatNarodaSearchTool(this),
                FamilySearchQueryTool(this),
                GeneralWebSearchTool(this),
                SearchFamilyTreeTool(this)
        )
