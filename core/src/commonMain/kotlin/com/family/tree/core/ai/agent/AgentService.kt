package com.family.tree.core.ai.agent

import com.family.tree.core.ProjectData
import com.family.tree.core.ai.AiClientFactory
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.koog.KoogModelAdapter
import ai.koog.agents.core.AIAgent
import ai.koog.agents.core.tool.ToolRegistry
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

        // The methodology instructions are now distributed across nodes in AutoresearchStrategy.
        // We only pass the base methodology skills to the strategy for reference in the Research phase.
        val methodologySkills = methodologyInstructions
        
        log("Instructions prepared. Creating Koog Strategy...")
        
        val tools = GenealogyTools(projectData, tavilyClient, tavilyKey, repoPath, onLog = { log(it) })
        
        log("Connecting to AI Provider: ${config.provider}...")
        val aiClient = AiClientFactory.createClient(config)
        val modelAdapter = KoogModelAdapter(aiClient, config)
        
        val strategy = createAutoresearchStrategy(
            instructions = baseInstructions,
            familyTreeContext = treeMarkdown,
            methodologySkills = methodologySkills
        )
        
        val agent = AIAgent(
            model = modelAdapter,
            toolRegistry = ToolRegistry(tools),
            onLog = { log(it) }
        )
        
        log("Starting autonomous agent execution using Koog Graph Strategy...")
        
        try {
            val response = agent.execute(strategy, input = Unit)
            log("Agent execution completed successfully!")
            
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
