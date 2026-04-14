package com.family.tree.core.ai.agent

import com.family.tree.core.ProjectData
import com.family.tree.core.ai.koog.Tool
import com.family.tree.core.platform.ResourceLoader

class GenealogyTools(
    private val projectData: ProjectData,
    private val tavilyClient: TavilyClient,
    private val apiKey: String,
    private val repoPath: String = "files/autoresearch-genealogy",
    private val onLog: (String) -> Unit = {}
) {
    private val loader = ResourceLoader()

    @Tool("Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').")
    suspend fun readMethodology(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listMethodologyGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readMethodology(\"$fileName\")")
        val result = loader.readFile(repoPath, "workflows/$fileName")
            ?: "Error: Workflow $fileName not found in $repoPath/workflows/"
        onLog("📖 [TOOL RESULT] readMethodology → ${result.take(120)}...")
        return result
    }

    @Tool("Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').")
    suspend fun readArchiveGuide(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listArchiveGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readArchiveGuide(\"$fileName\")")
        val result = loader.readFile(repoPath, "archives/$fileName")
            ?: "Error: Archive guide $fileName not found in $repoPath/archives/"
        onLog("📖 [TOOL RESULT] readArchiveGuide → ${result.take(120)}...")
        return result
    }

    @Tool("List all available archive guides for countries and regions.")
    suspend fun listArchiveGuides(): String {
        onLog("📋 [TOOL] listArchiveGuides()")
        val files = loader.listDirectory(repoPath, "archives")
        val result = if (files.isEmpty()) {
            "No archive guides found."
        } else {
            "Available archive guides: ${files.joinToString(", ")}"
        }
        onLog("📋 [TOOL RESULT] listArchiveGuides → $result")
        return result
    }

    @Tool("List all available professional methodology workflow guides.")
    suspend fun listMethodologyGuides(): String {
        onLog("📋 [TOOL] listMethodologyGuides()")
        val files = loader.listDirectory(repoPath, "workflows")
        val result = if (files.isEmpty()) {
            "No methodology guides found."
        } else {
            "Available methodology guides: ${files.joinToString(", ")}"
        }
        onLog("📋 [TOOL RESULT] listMethodologyGuides → $result")
        return result
    }

    @Tool("List all available research examples showing how to apply methodology.")
    suspend fun listExamples(): String {
        onLog("📋 [TOOL] listExamples()")
        val files = loader.listDirectory(repoPath, "examples")
        val result = if (files.isEmpty()) {
            "No research examples found."
        } else {
            "Available examples: ${files.joinToString(", ")}"
        }
        onLog("📋 [TOOL RESULT] listExamples → $result")
        return result
    }

    @Tool("Read a specific research example (e.g., 'tree-expansion-session.md') to see professional protocols in action.")
    suspend fun readExample(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listExamples' to find the available file names first."
        }
        onLog("📖 [TOOL] readExample(\"$fileName\")")
        val result = loader.readFile(repoPath, "examples/$fileName")
            ?: "Error: Example $fileName not found in $repoPath/examples/"
        onLog("📖 [TOOL RESULT] readExample → ${result.take(120)}...")
        return result
    }

    @Tool("List all available reference guides (standard conventions, glossaries).")
    suspend fun listReferenceGuides(): String {
        onLog("📋 [TOOL] listReferenceGuides()")
        val files = loader.listDirectory(repoPath, "reference")
        val result = if (files.isEmpty()) {
            "No reference guides found."
        } else {
            "Available reference guides: ${files.joinToString(", ")}"
        }
        onLog("📋 [TOOL RESULT] listReferenceGuides → $result")
        return result
    }

    @Tool("Read a specific reference guide (e.g., 'gedcom-guide.md', 'confidence-tiers.md').")
    suspend fun readReferenceGuide(fileName: String): String {
        if (fileName.isBlank() || fileName == "null") {
            return "Error: fileName is required. Use 'listReferenceGuides' to find the available file names first."
        }
        onLog("📖 [TOOL] readReferenceGuide(\"$fileName\")")
        val result = loader.readFile(repoPath, "reference/$fileName")
            ?: "Error: Reference guide $fileName not found in $repoPath/reference/"
        onLog("📖 [TOOL RESULT] readReferenceGuide → ${result.take(120)}...")
        return result
    }

    @Tool("Get a summary of all unique geographic locations (countries, regions) found in the family tree.")
    fun getGeographicProfile(): String {
        onLog("🗺️ [TOOL] getGeographicProfile()")
        val places = projectData.individuals
            .flatMap { it.events.mapNotNull { e -> e.place?.trim() }.filter { it.isNotBlank() } }
            .distinct()
            .sorted()

        val result = if (places.isEmpty()) {
            "No geographic locations found in the current family tree."
        } else {
            "The family tree contains the following locations: ${places.joinToString(", ")}"
        }
        onLog("🗺️ [TOOL RESULT] getGeographicProfile → $result")
        return result
    }

    @Tool("Search for specific individuals or facts in the local family tree by name or location keywords.")
    fun searchFamilyTree(query: String): String {
        onLog("🔍 [TOOL] searchFamilyTree(query=\"$query\")")
        val results = projectData.individuals.filter { person ->
            person.displayName.contains(query, ignoreCase = true) ||
            person.events.any { it.place?.contains(query, ignoreCase = true) == true } ||
            person.events.any { it.date?.contains(query, ignoreCase = true) == true }
        }.take(10)

        val result = if (results.isEmpty()) {
            "No matching individuals found in the local tree for query: '$query'."
        } else {
            "Found ${results.size} matches:\n" + results.joinToString("\n") {
                "- ${it.displayName} (${it.birthYear ?: "?"} - ${it.deathYear ?: "?"})"
            }
        }
        onLog("🔍 [TOOL RESULT] searchFamilyTree → $result")
        return result
    }

    @Tool("Perform an internet search for genealogy records using Tavily. Specify 'region' and 'targetSite' (e.g. 'vgd.ru') for higher precision.")
    suspend fun search(
        name: String = "",
        birth_location: String? = null,
        life_span: String? = null,
        query: String? = null,
        region: String? = null,
        targetSite: String? = null
    ): String {
        val baseQuery = if (query.isNullOrBlank()) {
            buildString {
                if (name.isNotBlank()) append(name)
                birth_location?.let { append(" born in $it") }
                life_span?.let { append(" ($it)") }
                region?.let { append(" $it") }
                targetSite?.let { append(" site:$it") }
                if (isNotBlank()) append(" genealogy records")
                else append("genealogy records")
            }
        } else {
            var q = query
            if (name.isNotBlank() && !q.contains(name, ignoreCase = true)) q = "$name $q"
            if (region != null && !q.contains(region, ignoreCase = true)) q = "$q $region"
            if (targetSite != null && !q.contains(targetSite, ignoreCase = true)) q = "$q site:$targetSite"
            q
        }

        onLog("🌐 [TAVILY REQUEST] query=\"$baseQuery\"  apiKey=${if (apiKey.isBlank()) "MISSING!" else "***${apiKey.takeLast(4)}"}")

        if (apiKey.isBlank()) {
            val err = "❌ Tavily API key is not set. Go to AI Settings and enter your Tavily key."
            onLog(err)
            return err
        }

        val result = tavilyClient.search(apiKey, baseQuery, searchDepth = "advanced", onLog = onLog)
        onLog("🌐 [TAVILY RESPONSE] ${result.take(300)}...")
        return result
    }
}

data class AgentProposal(
    val promptName: String,
    val taskDescription: String,
    val results: String
)
