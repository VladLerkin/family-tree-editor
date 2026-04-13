package com.family.tree.core.ai.agent

import com.family.tree.core.ProjectData
import com.family.tree.core.ai.koog.Tool
import com.family.tree.core.platform.ResourceLoader

class GenealogyTools(
    private val projectData: ProjectData,
    private val tavilyClient: TavilyClient,
    private val apiKey: String,
    private val repoPath: String = "files/autoresearch-genealogy"
) {
    private val loader = ResourceLoader()

    @Tool("Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').")
    suspend fun readMethodology(fileName: String): String {
        return loader.readFile(repoPath, "workflows/$fileName") 
            ?: "Error: Workflow $fileName not found in $repoPath/workflows/"
    }

    @Tool("Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').")
    suspend fun readArchiveGuide(fileName: String): String {
        return loader.readFile(repoPath, "archives/$fileName")
            ?: "Error: Archive guide $fileName not found in $repoPath/archives/"
    }

    @Tool("List all available archive guides for countries and regions.")
    suspend fun listArchiveGuides(): String {
        val files = loader.listDirectory(repoPath, "archives")
        return if (files.isEmpty()) {
            "No archive guides found."
        } else {
            "Available archive guides: ${files.joinToString(", ")}"
        }
    }

    @Tool("List all available professional methodology workflow guides.")
    suspend fun listMethodologyGuides(): String {
        val files = loader.listDirectory(repoPath, "workflows")
        return if (files.isEmpty()) {
            "No methodology guides found."
        } else {
            "Available methodology guides: ${files.joinToString(", ")}"
        }
    }

    @Tool("Get a summary of all unique geographic locations (countries, regions) found in the family tree.")
    fun getGeographicProfile(): String {
        val places = projectData.individuals
            .flatMap { it.events.mapNotNull { e -> e.place?.trim() }.filter { it.isNotBlank() } }
            .distinct()
            .sorted()
        
        return if (places.isEmpty()) {
            "No geographic locations found in the current family tree."
        } else {
            "The family tree contains the following locations: ${places.joinToString(", ")}"
        }
    }

    @Tool("Search for specific individuals or facts in the local family tree by name or location keywords.")
    fun searchFamilyTree(query: String): String {
        val results = projectData.individuals.filter { person ->
            person.displayName.contains(query, ignoreCase = true) ||
            person.events.any { it.place?.contains(query, ignoreCase = true) == true } ||
            person.events.any { it.date?.contains(query, ignoreCase = true) == true }
        }.take(10)
        
        return if (results.isEmpty()) {
            "No matching individuals found in the local tree for query: '$query'."
        } else {
            "Found ${results.size} matches:\n" + results.joinToString("\n") { 
                "- ${it.displayName} (${it.birthYear ?: "?"} - ${it.deathYear ?: "?"})" 
            }
        }
    }

    @Tool("Perform an internet search for genealogy records using Tavily. Specify 'region' and 'targetSite' (e.g. 'vgd.ru') for higher precision.")
    suspend fun search(
        name: String,
        birth_location: String? = null,
        life_span: String? = null,
        query: String? = null,
        region: String? = null,
        targetSite: String? = null
    ): String {
        val baseQuery = if (query.isNullOrBlank()) {
            buildString {
                append(name)
                birth_location?.let { append(" born in $it") }
                life_span?.let { append(" ($it)") }
                region?.let { append(" $it") }
                targetSite?.let { append(" site:$it") }
                append(" genealogy records")
            }
        } else {
            // If custom query provided, still try to anchor it with name and region if missing
            var q = query
            if (!q.contains(name, ignoreCase = true)) q = "$name $q"
            if (region != null && !q.contains(region, ignoreCase = true)) q = "$q $region"
            if (targetSite != null && !q.contains(targetSite, ignoreCase = true)) q = "$q site:$targetSite"
            q
        }
        
        return tavilyClient.search(apiKey, baseQuery, searchDepth = "advanced")
    }
}

data class AgentProposal(
    val promptName: String,
    val taskDescription: String,
    val results: String
)
