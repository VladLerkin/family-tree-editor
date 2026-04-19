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
            
            ### AUTORESEARCH PROTOCOL (STRICT LINEAR WORKFLOW)
            You are an expert genealogist. You MUST follow this exact sequence of actions in order. Do nothing else.
            
            #### STEP 1: DETERMINE COUNTRY
            - Analyze the provided <Family_Tree> context.
            - Identify the geographical locations (countries) of the persons in the tree.
            
            #### STEP 2: FIND ARCHIVE GUIDE
            - Use the 'listArchiveGuides' tool to see the available region files.
            - Select the appropriate file for the identified country.
            - Use the 'readArchiveGuide' tool to read the selected file.
            
            #### STEP 3: IDENTIFY TARGET ANCESTOR & SOURCES
            - Find the list of recommended sources/links mentioned in the archive guide for that country.
            - Identify the OLDEST ancestor in the <Family_Tree> who belongs to that specific country.
            
            #### STEP 4: SEARCH
            - Use the 'search' tool to check ONLY this oldest ancestor on ALL the relevant links/databases mentioned in the archive guide. 
            - Use the 'targetSite' parameter to restrict the search to those exact sites.
            
            #### STEP 5: SHOW RESULTS & STOP
            - Present the results found for this oldest ancestor from the searched links.
            - Provide the exact URLs of the findings.
            - Stop immediately. Everything else is unnecessary. Do not perform any other searches or analyses.
        """.trimIndent()

        // The methodology instructions are now distributed across nodes in AutoresearchStrategy.
        // We only pass the base methodology skills to the strategy for reference in the Research phase.
        val methodologySkills = methodologyInstructions
        
        log("Instructions prepared. Creating Koog Strategy...")
        
        log("Connecting to AI Provider: ${config.provider}...")
        val aiClient = AiClientFactory.createClient(config)
        
        val tools = GenealogyTools(projectData, tavilyClient, tavilyKey, repoPath, aiClient, config, onLog = { log(it) })
        
        val modelAdapter = KoogModelAdapter(aiClient, config, onLog = { log(it) })
        
        log("Creating Koog Strategy...")
        val strategy = createAutoresearchStrategy(
            instructions = baseInstructions,
            familyTreeContext = treeMarkdown,
            methodologySkills = methodologySkills,
            onLog = { log(it) }
        )
        
        log("Building AIAgent...")
        val agent = AIAgent(
            promptExecutor = modelAdapter,
            agentConfig = AIAgentConfig(
                prompt = prompt("genealogy-researcher") {
                    system("""
                        You are a professional genealogist assistant. 
                        You are equipped with specialized tools for archive guides, family tree analysis, and internet search.
                        
                        PROTOCOL:
                        1. You MUST use tools to fetch data. DO NOT guess facts.
                        2. You MUST use the Function Calling API for tools. DO NOT write tool calls as text.
                        3. Always verify facts against the provided family tree context.
                    """.trimIndent())
                },
                model = LLModel(
                    provider = LLMProvider.OpenAI, 
                    id = config.model
                ),
                maxAgentIterations = 40
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
