package com.family.tree.core.ai.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.family.tree.core.ProjectData
import com.family.tree.core.ai.AiClientFactory
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.ai.koog.KoogModelAdapter
import com.family.tree.core.export.MarkdownTreeExporter
import io.ktor.client.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException

class AgentService(
        private val settingsStorage: AiSettingsStorage,
        private val exporter: MarkdownTreeExporter,
        private val tavilyClient: TavilyClient,
        private val aiClientFactory: AiClientFactory,
        private val httpClient: HttpClient
) {

    private val _agentLogs = MutableStateFlow<List<String>>(emptyList())
    val agentLogs: StateFlow<List<String>> = _agentLogs.asStateFlow()

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

        val variables =
                mapOf(
                        "FAMILY_TREE" to treeMarkdown,
                        "VAULT_PATH" to "./vault", // Default or from settings
                        "DATE" to "2026-04-13" // Current date
                )

        val baseInstructions =
                if (template != null) {
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
        val contextHeader =
                if (!baseInstructions.contains(treeMarkdown)) {
                    "\n\n### PROJECT_CONTEXT\n<Family_Tree>\n$treeMarkdown\n</Family_Tree>\n"
                } else ""

        // Inject professional methodology skills with directive workflow
        val methodologyInstructions =
                """
            
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
            - Identify ancestors in the <Family_Tree>.
            - For individuals born 1890-1930 (USSR/Russia), prioritize 'searchPamyatNaroda'.
            - For ALL individuals in the tree, use 'searchFamilySearch' to find official vital records.
            
            #### STEP 4: SEARCH
            - Use 'searchPamyatNaroda' as THE PRIMARY TOOL for all valid candidates from USSR/Russia (born 1890-1930).
            - Use 'searchFamilySearch' for EVERYONE ELSE and as a secondary global search for all candidates. It is excellent for birth/marriage/death records.
            - Focus on finding service records, casualty records, and citations.
            - Only use general web search (Tavily) as a final resort or for broad historical context.
            
            #### STEP 5: SHOW RESULTS & STOP
            - Present the results found for the ancestor.
            - Provide the exact details and evidence snippets.
            - Stop immediately. Everything else is unnecessary. Do not perform any other searches or analyses.
        """.trimIndent()

        // The methodology instructions are now distributed across nodes in AutoresearchStrategy.
        // We only pass the base methodology skills to the strategy for reference in the Research
        // phase.
        val methodologySkills = methodologyInstructions

        log("Instructions prepared. Creating Koog Strategy...")

        log("Connecting to AI Provider: ${config.provider}...")
        val aiClient = aiClientFactory.createClient(config)

        val tools =
                GenealogyTools(
                        projectData,
                        tavilyClient,
                        httpClient,
                        tavilyKey,
                        repoPath,
                        aiClient,
                        config,
                        config.pamyatNarodaCookies,
                        config.familySearchCookies,
                        onLog = { log(it) }
                )

        val modelAdapter = KoogModelAdapter(aiClient, config, onLog = { log(it) })

        log("Creating Koog Strategy...")
        val strategy =
                createAutoresearchStrategy(
                        instructions = baseInstructions,
                        familyTreeContext = treeMarkdown,
                        methodologySkills = methodologySkills,
                        onLog = { log(it) }
                )

        log("Building AIAgent...")
        log("DEBUG: Pamyat Naroda Cookies length from config: ${config.pamyatNarodaCookies.length}")
        val agent =
                AIAgent(
                        promptExecutor = modelAdapter,
                        agentConfig =
                                AIAgentConfig(
                                        prompt =
                                                prompt("genealogy-researcher") {
                                                    system(
                                                            """
                        You are a professional genealogist assistant. 
                        You are equipped with specialized tools for archive guides, family tree analysis, and internet search.
                        
                        PROTOCOL:
                        1. ARCHIVE-FIRST: You MUST prioritize specialized archive tools (like Pamyat Naroda) OVER general web search for eligible candidates.
                        2. You MUST use tools to fetch data. DO NOT guess facts.
                        3. You MUST use the Function Calling API for tools. DO NOT write tool calls as text.
                        4. Always verify facts against the provided family tree context.
                    """.trimIndent()
                                                    )
                                                },
                                        model =
                                                LLModel(
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
            val response =
                    agent.run(
                            agentInput =
                                    "Analyze the family tree and proceed with the research goals."
                    )
            log("Agent execution completed successfully! Result length: ${response.length}")

            return AgentProposal(
                    promptName = promptName,
                    taskDescription = "Processed ${promptName} with Koog Graph Strategy.",
                    results = response
            )
        } catch (e: Exception) {
            val message = e.message ?: "Unknown error"
            val isTimeout = message.contains("given number of steps")
            val isCancellation = e is CancellationException

            if (isTimeout || isCancellation) {
                val reason = if (isTimeout) "Iteration limit reached (40 steps)" else "User cancelled execution"
                log("⚠️ [GRACEFUL STOP] $reason. Synthesizing results...")

                return withContext(NonCancellable) {
                    val logsContext = _agentLogs.value.joinToString("\n").takeLast(10000)
                    val synthesisPrompt = """
                        The genealogy research agent was interrupted ($reason).
                        Based on the following logs of its activities, please summarize the findings, 
                        extracted facts, and potential sources discovered so far.
                        If nothing meaningful was found, please state that.
                        
                        LOGS:
                        $logsContext
                    """.trimIndent()

                    val fallbackResult = try {
                        aiClient.sendPrompt(synthesisPrompt, config)
                    } catch (ex: Exception) {
                        "Research stopped: $reason. Additionally, result synthesis failed: ${ex.message}\n\nCheck the console logs for partial findings."
                    }

                    AgentProposal(
                            promptName = promptName,
                            taskDescription = "Research partially completed ($reason)",
                            results = fallbackResult
                    )
                }
            }

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
