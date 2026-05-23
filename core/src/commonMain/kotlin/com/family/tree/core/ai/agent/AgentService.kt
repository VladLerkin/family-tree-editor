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
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AgentService(
        private val settingsStorage: AiSettingsStorage,
        private val exporter: MarkdownTreeExporter,
        private val tavilyClient: TavilyClient,
        private val aiClientFactory: AiClientFactory,
        private val httpClient: HttpClient
) {

        suspend fun testOpenAiDirect(apiKey: String): String {
                try {
                        println("AgentService: testOpenAiDirect() starting...")
                        val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                                header("Authorization", "Bearer $apiKey")
                                header("Content-Type", "application/json")
                                setBody("""
                                        {
                                                "model": "gpt-4o-mini",
                                                "messages": [
                                                        {"role": "user", "content": "Hello!"}
                                                ]
                                        }
                                """.trimIndent())
                        }
                        val text = response.bodyAsText()
                        return "Status: ${response.status}, Response: $text"
                } catch (e: Exception) {
                        return "Error: ${e.message}"
                }
        }
        suspend fun testOpenAiPayload(apiKey: String): String {
                try {
                        println("AgentService: testOpenAiPayload() starting...")
                        val payload = """
{
  "model": "gpt-4o-mini",
  "temperature": 0.7,
  "max_tokens": 4000,
  "messages": [
    {
      "role": "system",
      "content": "PROTOCOL:\n- Use 'searchPamyatNaroda' for WWII veterans (1890-1930, USSR/Russia).\n- Use 'queryFamilySearch' for BOTH vital records and relatives (dual search).\n- Always provide evidence snippets."
    },
    {
      "role": "system",
      "content": "            You are the SCANNER phase of the Genealogy Research protocol.\n            \n            RESEARCH GOAL:\n            Identify target ancestors in the family tree and search for them.\n            \n            CURRENT FAMILY TREE CONTEXT:\n            <Family_Tree>\n            ## Family Tree Summary\n- Total Individuals: 3\n- Total Families: 1\n- Geographic Scope: New York, USA (1 mentions), Los Angeles, USA (1 mentions), Boston, USA (1 mentions), Chicago, USA (1 mentions)\n\n## Ancestry View\n\nBob Doe (b. 5 MAY 1975, Chicago, USA)\n├── John Doe (b. 1 JAN 1950, d. 15 MAR 2020, New York, USA)\n└── Jane Doe (b. 10 FEB 1952, Boston, USA)\n\n            </Family_Tree>\n            \n            DRACONIAN DIRECTIVE (MANDATORY): \n            1. You MUST call ALL of these three tools in your first turn: 'getGeographicProfile', 'listArchiveGuides', and 'listMethodologyGuides'.\n            2. You are STRICTLY FORBIDDEN from calling the 'searchFamilyTree' tool during this phase. \n            3. Do NOT provide any text analysis until all three tools have returned results.\n            \n            AGE-FILTERING & TOOLS PROTOCOL: \n            - WWII ARCHIVES: Identify all persons in the tree born between 1890 and 1930. ONLY these persons are eligible for 'searchPamyatNaroda'.\n            - GLOBAL SEARCH: Use 'queryFamilySearch' for ALL individuals. This tool automatically performs a DUAL search (Historical Records + Family Tree).\n            - IMPORTANT: Survival after 1945 or a late peacetime death date (e.g., 1980s) does NOT disqualify a person from WWII research. \n            - Many veterans are found in the 1985 Jubilee Award database. Always plan 'searchPamyatNaroda' for them if they fit the birth year range.\n            - For individuals born BEFORE 1880 or AFTER 1935, focus on 'queryFamilySearch'.\n            - Use 'generalWebSearch' ONLY for historical context or specific archive URLs.\n            \n            ANTI-LOOP RULE: \n            - NEVER call 'searchPamyatNaroda' for a person if you have already received a \"Skipped\" or \"Outside range\" result for them.\n            - If the history already contains results from 'listArchiveGuides' and 'listMethodologyGuides', do NOT call them again.\n            - Once you have the profiles and guide lists, synthesize your analysis and finish this phase.\n            \n            If you do not call 'listArchiveGuides', you will not know which sources to search, and the process will fail."
    },
    {
      "role": "user",
      "content": "Analyze the family tree and proceed with the research goals."
    },
    {
      "role": "assistant",
      "content": "",
      "tool_calls": [
        {
          "id": "call_1qHfL45wKaI2aOcXZ3wjRcqa",
          "type": "function",
          "function": {
            "name": "Getasummaryofalluniquegeographiclocationscoun_4plu97",
            "arguments": "{}"
          }
        },
        {
          "id": "call_vVtRq1PgNXpTd21h99Wzz6T6",
          "type": "function",
          "function": {
            "name": "Listallavailablearchiveguidesforcountriesandr_1hz3gu",
            "arguments": "{}"
          }
        },
        {
          "id": "call_mJDQh8ySKBuHLPf1pzIdaY2u",
          "type": "function",
          "function": {
            "name": "Listallavailableprofessionalmethodologyworkfl_1o3far8",
            "arguments": "{}"
          }
        }
      ]
    },
    {
      "role": "tool",
      "content": "\"The family tree contains the following locations: Boston, USA, Chicago, USA, Los Angeles, USA, New York, USA\"",
      "tool_call_id": "call_1qHfL45wKaI2aOcXZ3wjRcqa"
    },
    {
      "role": "tool",
      "content": "\"Available archive guides: README.md, african-american.md, australia-nz.md, austria.md, canada.md, england-wales.md, france.md, germany.md, hungary.md, ireland.md, italy.md, jewish-genealogy.md, mexico-latin-america.md, netherlands.md, norway.md, poland.md, russia-ukraine.md, scotland.md, spain-portugal.md, sweden.md, usa-census.md, usa-colonial.md, usa-immigration.md, usa-vital-records.md\"",
      "tool_call_id": "call_vVtRq1PgNXpTd21h99Wzz6T6"
    },
    {
      "role": "tool",
      "content": "\"Available methodology guides: discrepancy-resolution.md, document-triage.md, getting-started.md, new-ancestor-intake.md, ocr-pipeline.md, oral-history-protocol.md, phase-planning.md\"",
      "tool_call_id": "call_mJDQh8ySKBuHLPf1pzIdaY2u"
    }
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "CRITICALDoNOTuseforspecificpersonsearchifpers_95hzlg",
        "description": "CRITICAL: Do NOT use for specific person search if person is eligible for specialized archives (e.g. WWII, USSR 1880-1930). Use ONLY for finding archive URLs, general history, or if specialized tools fail or do not exist.",
        "parameters": {
          "type": "object",
          "properties": {
            "name": { "type": "string" },
            "birth_location": { "type": "string" },
            "life_span": { "type": "string" },
            "query": { "type": "string" },
            "region": { "type": "string" },
            "targetSite": { "type": "string" }
          },
          "required": ["name", "birth_location", "life_span", "query", "region", "targetSite"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Getasummaryofalluniquegeographiclocationscoun_4plu97",
        "description": "Get a summary of all unique geographic locations (countries, regions) found in the family tree.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Listallavailablearchiveguidesforcountriesandr_1hz3gu",
        "description": "List all available archive guides for countries and regions.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Listallavailableresearchexamplesshowinghowtoa_rja3kd",
        "description": "List all available research examples showing how to apply methodology.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Listallavailableprofessionalmethodologyworkfl_1o3far8",
        "description": "List all available professional methodology workflow guides.",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Listallavailablereferenceguidesstandardconven_8nz9c",
        "description": "List all available reference guides (standard conventions, glossaries).",
        "parameters": {
          "type": "object",
          "properties": {}
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "SearchFamilySearchThistoolautomaticallyperfor_1hdc423",
        "description": "Search FamilySearch. This tool automatically performs a DUAL search: 1. HISTORICAL RECORDS (official documents) and 2. FAMILY TREE (relative links). PROTOCOL: 1. Always pass 'gender'. 2. For MEN, both names are EXACT; provide ONLY first name in 'firstName'. 3. For WOMEN, First Name is EXACT, Surname is FUZZY.",
        "parameters": {
          "type": "object",
          "properties": {
            "firstName": { "type": "string" },
            "lastName": { "type": "string" },
            "birthYear": { "type": "string" },
            "birthPlace": { "type": "string" },
            "deathYear": { "type": "string" },
            "deathPlace": { "type": "string" },
            "gender": { "type": "string" },
            "exactMatch": { "type": "string" }
          },
          "required": ["firstName", "lastName", "birthYear", "birthPlace", "deathYear", "deathPlace", "gender", "exactMatch"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Readanarchiveguideforaspecificcountryorregion_oc8pmw",
        "description": "Read an archive guide for a specific country or region (e.g., 'norway.md', 'usa-census.md').",
        "parameters": {
          "type": "object",
          "properties": {
            "fileName": { "type": "string" }
          },
          "required": ["fileName"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Readaspecificresearchexampleegtree-expansion_14fvnjg",
        "description": "Read a specific research example (e.g., 'tree-expansion-session.md') to see professional protocols in action.",
        "parameters": {
          "type": "object",
          "properties": {
            "fileName": { "type": "string" }
          },
          "required": ["fileName"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Readaspecificmethodologyworkflowguideegdiscre_1eh3oib",
        "description": "Read a specific methodology workflow guide (e.g., 'discrepancy-resolution.md').",
        "parameters": {
          "type": "object",
          "properties": {
            "fileName": { "type": "string" }
          },
          "required": ["fileName"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Readaspecificreferenceguideeggedcom-guidemdco_k10gn7",
        "description": "Read a specific reference guide (e.g., 'gedcom-guide.md', 'confidence-tiers.md').",
        "parameters": {
          "type": "object",
          "properties": {
            "fileName": { "type": "string" }
          },
          "required": ["fileName"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "Searchforspecificindividualsorfactsinthelocal_63c1py",
        "description": "Search for specific individuals or facts in the local family tree by name or location keywords.",
        "parameters": {
          "type": "object",
          "properties": {
            "query": { "type": "string" }
          },
          "required": ["query"]
        }
      }
    },
    {
      "type": "function",
      "function": {
        "name": "MANDATORYPRIMARYTOOLforUSSRRussia1890-1930Sea_1t0mafl",
        "description": "[MANDATORY PRIMARY TOOL for USSR/Russia 1890-1930] Search the 'Pamyat Naroda' WWII database for Soviet military records. Target individuals born ~1880-1930 who could have served in 1941-1945.",
        "parameters": {
          "type": "object",
          "properties": {
            "firstName": { "type": "string" },
            "lastName": { "type": "string" },
            "patronymic": { "type": "string" },
            "birthYear": { "type": "string" }
          },
          "required": ["firstName", "lastName", "patronymic", "birthYear"]
        }
      }
    }
  ]
}
                        """.trimIndent()
                        val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                                header("Authorization", "Bearer $apiKey")
                                header("Content-Type", "application/json")
                                setBody(payload)
                        }
                        val text = response.bodyAsText()
                        return "Status: ${response.status}, Response: $text"
                } catch (e: Exception) {
                        return "Error: ${e.message}"
                }
        }

        private val _agentLogs = MutableStateFlow<List<String>>(emptyList())
        val agentLogs: StateFlow<List<String>> = _agentLogs.asStateFlow()

        fun loadConfig() = settingsStorage.loadConfig()

        fun clearLogs() {
                _agentLogs.value = emptyList()
        }

        private fun log(message: String) {
                _agentLogs.value = _agentLogs.value + message
                println("[AGENT-LOG] $message")
        }

        suspend fun loadAvailablePrompts(): List<PromptTemplate> {
                val config = settingsStorage.loadConfig()
                val path = config.autoresearchRepoPath
                println(
                        "[DEBUG_LOG] AgentService: loadAvailablePrompts() called with repoPath: $path"
                )
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
                        log(
                                "WARNING: Tavily API Key is not set in AI Config. Web search might fail."
                        )
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
                                        instructions =
                                                instructions.replace("{{$k}}", v).replace("[$k]", v)
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
            - For ALL individuals in the tree, you MUST call 'queryFamilySearch' (it automatically searches BOTH Historical Records and Family Tree).
                2. Skipping 'queryFamilySearch' is a CRITICAL FAILURE as it contains parents/family links.
            
            #### STEP 4: SEARCH
            - Use 'searchPamyatNaroda' as THE PRIMARY TOOL for all valid candidates from USSR/Russia (born 1890-1930).
            - Use 'queryFamilySearch' for ALL individuals (it automatically queries both Records and Tree in one call). 
                1. searchType='HISTORICAL_RECORDS': To find official birth/marriage/death records.
                2. searchType='FAMILY_TREE': To find parents, children, and spouses already linked by other users.
            - NAME PROTOCOL: Always include the patronymic (if known) in the 'firstName' parameter. Do NOT cut it off. (Example: firstName='Иван Иванович', lastName='Иванов').
            - Focus on finding service records, casualty records, citations, and lineage expansions.
            - Only use general web search (Tavily) as a final resort or for broad historical context.
            
            #### STEP 5: SHOW RESULTS & STOP
            - Present the results found for the ancestor.
            - Provide the exact details and evidence snippets.
            - Stop immediately. Everything else is unnecessary. Do not perform any other searches or analyses.
        """.trimIndent()

                // The methodology instructions are now distributed across nodes in
                // AutoresearchStrategy.
                // We only pass the base methodology skills to the strategy for reference in the
                // Research
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
                log(
                        "DEBUG: Pamyat Naroda Cookies length from config: ${config.pamyatNarodaCookies.length}"
                )
                val agent =
                        AIAgent(
                                promptExecutor = modelAdapter,
                                agentConfig =
                                        AIAgentConfig(
                                                prompt =
                                                        prompt("genealogy-researcher") {
                                                                system(
                                                                        """
                        PROTOCOL:
                        - Use 'searchPamyatNaroda' for WWII veterans (1890-1930, USSR/Russia).
                        - Use 'queryFamilySearch' for BOTH vital records and relatives (dual search).
                        - Always provide evidence snippets.
                    """.trimIndent()
                                                                )
                                                        },
                                                model =
                                                        LLModel(
                                                                provider = LLMProvider.OpenAI,
                                                                id = config.model
                                                        ),
                                                maxAgentIterations = 50
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
                        log(
                                "Agent execution completed successfully! Result length: ${response.length}"
                        )

                        return AgentProposal(
                                promptName = promptName,
                                taskDescription =
                                        "Processed ${promptName} with Koog Graph Strategy.",
                                results = response
                        )
                } catch (e: Exception) {
                        val message = e.message ?: "Unknown error"
                        val isTimeout = message.contains("given number of steps")
                        val isCancellation = e is CancellationException

                        if (isTimeout || isCancellation) {
                                val reason =
                                        if (isTimeout) "Iteration limit reached (50 steps)"
                                        else "User cancelled execution"
                                log("⚠️ [GRACEFUL STOP] $reason. Synthesizing results...")

                                return withContext(NonCancellable) {
                                        val logsContext =
                                                _agentLogs.value.joinToString("\n").takeLast(10000)
                                        val synthesisPrompt =
                                                """
                        The genealogy research agent was interrupted ($reason).
                        Based on the following logs of its activities, please summarize the findings, 
                        extracted facts, and potential sources discovered so far.
                        If nothing meaningful was found, please state that.
                        
                        LOGS:
                        $logsContext
                    """.trimIndent()

                                        val fallbackResult =
                                                try {
                                                        aiClient.sendPrompt(synthesisPrompt, config)
                                                } catch (ex: Exception) {
                                                        "Research stopped: $reason. Additionally, result synthesis failed: ${ex.message}\n\nCheck the console logs for partial findings."
                                                }

                                        AgentProposal(
                                                promptName = promptName,
                                                taskDescription =
                                                        "Research partially completed ($reason)",
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
