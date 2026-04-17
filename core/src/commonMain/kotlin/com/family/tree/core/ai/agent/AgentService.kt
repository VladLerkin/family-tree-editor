package com.family.tree.core.ai.agent

import com.family.tree.core.ProjectData
import com.family.tree.core.ai.AiClientFactory
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.koog.KoogModelAdapter
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt

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
        clearLogs() // CRITICAL: Clear logs for the new run
        log("Starting research for: $promptName...")
        
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
            - **Consult Standards**: Use 'listReferenceGuides' and 'readReferenceGuide' (e.g., 'gedcom-guide.md') to ensure your data mapping follows professional standards.
            
            #### STEP 2: DISCOVER ARCHIVES & EXAMPLES
            - Once regions are identified, you MUST use 'listArchiveGuides' to see available guides for different countries/regions.
            - **Mandatory Discovery**: You cannot guess filenames. Always use the list tools first.
            - **Learn from Examples**: If the task is complex, use 'listExamples' and 'readExample' to see how professional genealogists handled similar cases (e.g., 'dna-to-genealogy-mapping.md').
            - Match the identified regions with the available guides (e.g., use 'russia-ukraine.md' for both Russia and Ukraine).
            - Then use 'readArchiveGuide' with the **exact filename** found in the listing.
            
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
                - **Evidence Snippet**: For every NEW fact, you MUST include a short quote (verbatim snippet) in your thinking process or as a sub-bullet to prove its existence in the source.
                - **DRACONIAN URL POLICY**: You are strictly prohibited from constructing, guessing, or "cleaning up" deep-link URLs. 
                    1. You MUST copy-paste the exact URL from the 'URL' field of the tool result verbatim. 
                    2. **Placeholder Ban**: Using placeholder IDs like `123456789`, `abcdef`, `0000`, or `1111` in a URL is a CRITICAL FAILURE and will result in task termination. 
                    3. **No ID Fabrication**: If a result (e.g., from Find a Grave) is a search results page or a home page, you MUST use that exact URL. You are strictly FORBIDDEN from attempting to construct a memorial URL or person-details URL if the tool did not provide it.
                    4. **Zero Tolerance**: Any deviation from the verbatim URL returned by the tool is considered a hallucination.
                - **NO DOMAIN HALLUCINATION**: You are strictly forbidden from inventing plausible-sounding domain names (e.g., `history-tbilisi.com`, `georgianarchives.com`) if they were not explicitly present in the 'URL' field of a tool result. 
                - **Source Attribution**: Format each finding: `[NEW] Birth Date: 12 May 1890 [Source #X: Title](URL)`. Replace X with the actual Source number from the tool output.
                - Clearly mark what is NEW and what is a CORRECTION of existing data.
            3. **[SOURCES]**: Mandatory section at the end listing ALL URLs found during the research. Use ONLY the verbatim URLs from the search results. Copy-paste them exactly as they appeared in the tool output.
            4. **Next Steps**: Recommended further searches if no external data was found.
            
            **AUTO-STOPPING POLICY**: Do NOT stop until you have attempted at least one specific search for EACH geographic region identified in Step 1. Use your high iteration limit to be exhaustive.
        """.trimIndent()

        // The methodology instructions are now distributed across nodes in AutoresearchStrategy.
        // We only pass the base methodology skills to the strategy for reference in the Research phase.
        val methodologySkills = methodologyInstructions
        
        log("Instructions prepared. Creating Koog Strategy...")
        
        val tools = GenealogyTools(projectData, tavilyClient, tavilyKey, repoPath, onLog = { log(it) })
        
        log("Connecting to AI Provider: ${config.provider}...")
        val aiClient = AiClientFactory.createClient(config)
        val modelAdapter = KoogModelAdapter(aiClient, config, onLog = { log(it) })
        
        log("Creating Koog Strategy...")
        val strategy = createAutoresearchStrategy(
            instructions = baseInstructions,
            familyTreeContext = treeMarkdown,
            methodologySkills = methodologySkills
        )
        
        log("Building AIAgent...")
        val agent = AIAgent(
            promptExecutor = modelAdapter,
            agentConfig = AIAgentConfig(
                prompt = prompt("genealogy-researcher") {
                    system("You are a professional genealogist assistant.")
                },
                model = LLModel(
                    provider = LLMProvider.OpenAI, 
                    id = config.model ?: "gpt-4o"
                ),
                maxAgentIterations = 25
            ),
            strategy = strategy,
            toolRegistry = createToolRegistry(tools),
            id = "agent-${promptName.lowercase().replace(" ", "-")}",
            installFeatures = {}
        )
        
        log("Starting autonomous agent execution...")
        
        try {
            val response = agent.run(agentInput = "Analyze the family tree and proceed with the research goals.")
            log("Agent execution completed successfully! Result length: ${response.length}")
            
            return AgentProposal(
                promptName = promptName,
                taskDescription = "Processed ${promptName} with Koog Graph Strategy.",
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

internal expect fun createToolRegistry(tools: GenealogyTools): ToolRegistry
