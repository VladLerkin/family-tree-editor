package com.family.tree.core.ai.agent

import com.family.tree.core.ProjectData
import com.family.tree.core.ai.AiClientFactory
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.koog.AiClientAgentModel
import com.family.tree.core.ai.koog.KoogAgent
import com.family.tree.core.ai.agent.GenealogyTools
import com.family.tree.core.export.MarkdownTreeExporter
import io.ktor.client.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class AgentService(
    private val settingsStorage: AiSettingsStorage,
    private val httpClient: HttpClient,
    private val json: Json,
    private val exporter: MarkdownTreeExporter = MarkdownTreeExporter()
) {

    private val _agentLogs = MutableStateFlow<List<String>>(emptyList())
    val agentLogs: StateFlow<List<String>> = _agentLogs.asStateFlow()

    private val tavilyClient = TavilyClient(httpClient, json)

    fun loadConfig() = settingsStorage.loadConfig()

    fun clearLogs() {
        _agentLogs.value = emptyList()
    }

    private fun log(message: String) {
        _agentLogs.value = _agentLogs.value + message
    }

    suspend fun loadAvailablePrompts(): List<PromptTemplate> {
        val config = settingsStorage.loadConfig()
        val path = config.autoresearchRepoPath
        println("[DEBUG_LOG] AgentService: loadAvailablePrompts() called with repoPath: $path")
        log("Loading available prompts. Config path: $path")
        val prompts = PromptTemplate.loadExternalPrompts(path)
        println("[DEBUG_LOG] AgentService: Found ${prompts.size} prompts.")
        log("Found ${prompts.size} prompts.")
        return prompts
    }

    suspend fun runAutoresearchPrompt(
        projectData: ProjectData,
        promptName: String,
        promptInstructions: String
    ): AgentProposal {
        log("Initializing agent for prompt: ${promptName}...")
        
        val config = settingsStorage.loadConfig()
        val tavilyKey = config.tavilyApiKey
        val repoPath = config.autoresearchRepoPath
        
        if (tavilyKey.isBlank()) {
            log("WARNING: Tavily API Key is not set in AI Config. Web search might fail.")
        }
        
        log("Exporting current family tree context to Markdown...")
        val treeMarkdown = exporter.exportToString(projectData)
        
        // Load external prompts if repo path exists
        val externalPrompts = PromptTemplate.loadExternalPrompts(repoPath)
        val template = externalPrompts.find { it.name == promptName || it.id == promptName }
        
        val variables = mapOf(
            "FAMILY_TREE" to treeMarkdown,
            "VAULT_PATH" to "./vault", // Default or from settings
            "DATE" to "2026-04-13" // Current date
        )
        
        val baseInstructions = if (template != null) {
            template.fillTemplate(variables)
        } else {
            // Manual interpolation for custom prompts
            var instructions = promptInstructions
            variables.forEach { (k, v) ->
                instructions = instructions.replace("{{$k}}", v).replace("[$k]", v)
            }
            instructions
        }

        // Guaranteed tree injection with XML tags for better parsing
        val contextHeader = if (!baseInstructions.contains(treeMarkdown)) {
            "\n\n### PROJECT_CONTEXT\n<Family_Tree>\n$treeMarkdown\n</Family_Tree>\n"
        } else ""

        // Inject professional methodology skills with directive workflow
        val methodologyInstructions = """
            
            ### Professional Methodology (Skills)
            You are an expert genealogist using the Autoresearch Genealogy protocol. 
            
            #### STEP 1: SCAN LOCAL CONTEXT
            - First, analyze the provided <Family_Tree> to identify the names, dates, and specifically the **geographic locations** (countries, parishes, regions) of the ancestors in scope.
            - Use the 'getGeographicProfile' tool to get a high-level summary of relevant regions.
            
            #### STEP 2: DISCOVER ARCHIVES
            - Once regions are identified, use 'listArchiveGuides' to see available guides for different countries/regions.
            - Match the identified regions with the available guides (e.g., use 'russia-ukraine.md' for both Russia and Ukraine).
            - Then use 'readArchiveGuide' with the exact filename to find specific online databases and search parameters.
            
            #### STEP 3: EXECUTE RESEARCH
            - Use 'listMethodologyGuides' and 'readMethodology' to consult workflow guides (e.g., 'discrepancy-resolution.md') if you find conflicting data.
            - **Native Language Directive**: For ancestors in Russia, Ukraine, or Belarus, you MUST perform searches using **Cyrillic (Russian/Ukrainian names)**.
            - **Targeted Search Strategy**: Do not just do generic searches. Use 'targetSite' to search specific databases mentioned in archive guides (e.g., 'vgd.ru', 'pamyat-naroda.ru').
            - **Regional Precision**: Always include the 'region' or country in your search tool calls to avoid irrelevant results from other countries (e.g., Maryland results for Russian records).
            - Follow the Tiered Source hierarchy: Tier 1 (Vital records) > Tier 2 (Newspapers) > Tier 3 (User trees).
            - Execute web searches using 'search'.
            #### STEP 4: FINAL PROPOSAL FORMATTING
            Your final answer MUST be structured as follows:
            1. **Summary of Research**: Briefly state which databases and archives were searched.
            2. **External Findings (New Data Only)**: 
               - **IMPORTANT**: Mention ONLY facts that were discovered in EXTERNAL sources (web search, archives). 
               - **NO ECHOING**: Do NOT report facts that are already present in the provided <Family_Tree> context as 'New'.
               - **Deep Verification**: Every fact MUST be explicitly present in the 'Snippet' text of one of your search results. If it is not in the results, it is a hallucination—DISCARD IT.
               - **No Fake URLs**: You MUST provide the exact URL from the 'URL' field of the result. Forging URLs (e.g., using placeholders like `123456789`, `abcdef`, `findagrave.com/memorial/000`) is strictly FORBIDDEN and will lead to total task failure.
               - **Source Attribution**: Format each finding: `[NEW] Birth Date: 12 May 1890 [Source: Title](URL)`.
               - Clearly mark what is NEW and what is a CORRECTION of existing data.
            3. **[SOURCES]**: Mandatory section at the end listing ALL URLs found during the research. Do NOT list 'Family Tree' as a source here; ONLY external internet links.
            4. **Next Steps**: Recommended further searches if no external data was found.
            
            **AUTO-STOPPING POLICY**: Do NOT stop until you have attempted at least one specific search for EACH geographic region identified in Step 1. Use your high iteration limit to be exhaustive.
        """.trimIndent()
        
        val finalInstructions = baseInstructions + contextHeader + methodologyInstructions
        
        log("Instructions prepared. Context length: ${finalInstructions.length} characters.")
        
        val tools = listOf(
            GenealogyTools(projectData, tavilyClient, tavilyKey, repoPath)
        )
        
        log("Connecting to AI Provider: ${config.provider}...")
        val aiClient = AiClientFactory.createClient(config)
        val agentModel = AiClientAgentModel(aiClient, config)
        
        val agent = KoogAgent(
            name = "Autoresearch Genealogy Assistant",
            instructions = finalInstructions,
            tools = tools,
            model = agentModel,
            maxIterations = 25, 
            onLog = { log(it) }
        )
        
        log("Starting autonomous agent execution loop (max 5 iterations)...")
        
        try {
            val response = agent.execute("Analyze the family tree and proceed with the research goals using professional methodology.")
            log("Agent execution completed successfully!")
            
            return AgentProposal(
                promptName = promptName,
                taskDescription = "Processed ${promptName} with professional methodology skills.",
                results = response
            )
        } catch (e: Exception) {
            log("Agent execution failed: ${e.message}")
            return AgentProposal(
                promptName = promptName,
                taskDescription = "Execution failed",
                results = "Error: ${e.message}"
            )
        }
    }
}
