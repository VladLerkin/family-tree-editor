package com.family.tree.core.ai.agent

import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.dsl.builder.*
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart

/** Autoresearch Genealogy Strategy implemented as a multi-node Koog Graph. */
fun createAutoresearchStrategy(
        instructions: String,
        familyTreeContext: String,
        methodologySkills: String,
        accumulatedFindings: MutableList<String>,
        onLog: (String) -> Unit = {}
) =
        strategy<String, String>("AutoresearchStrategy") {

                // --- SHARED STATE (Scope-local) ---

                // --- NODES ---

                // 1. SCAN PHASE
                val scanPrompt: AIAgentNodeBase<String, String> by
                        nodeAppendPrompt("ScanInstructions") {
                                onLog("🔍 [GRAPH] Entering SCAN phase")
                                system(
                                        """
            You are the SCANNER phase of the Genealogy Research protocol.
            
            RESEARCH GOAL:
            $instructions
            
            CURRENT FAMILY TREE CONTEXT:
            <Family_Tree>
            $familyTreeContext
            </Family_Tree>
            
            DRACONIAN DIRECTIVE (MANDATORY): 
            1. You MUST call ALL of these three tools in your first turn: 'getGeographicProfile', 'listArchiveGuides', and 'listMethodologyGuides'.
            2. You are STRICTLY FORBIDDEN from calling the 'searchFamilyTree' tool during this phase. 
            3. Do NOT provide any text analysis until all three tools have returned results.
            
            AGE-FILTERING & TOOLS PROTOCOL: 
            - WWII ARCHIVES: Identify all persons in the tree born between 1890 and 1930 WHO were born or lived in the USSR / Russian Empire regions (e.g., Russia, Ukraine, Belarus, Baltics, Caucasus, Central Asia). ONLY these persons are eligible for 'searchPamyatNaroda'. Do NOT call this tool for individuals born or living in America, Western Europe, or other non-Soviet regions.
            - GLOBAL SEARCH: Use 'queryFamilySearch' for ALL individuals. This tool automatically performs a DUAL search (Historical Records + Family Tree).
            - IMPORTANT: Survival after 1945 or a late peacetime death date (e.g., 1980s) does NOT disqualify a person from WWII research. 
            - Many veterans are found in the 1985 Jubilee Award database. Always plan 'searchPamyatNaroda' for them if they fit the birth year range.
            - For individuals born BEFORE 1880 or AFTER 1935, focus on 'queryFamilySearch'.
            - Use 'generalWebSearch' ONLY for historical context or specific archive URLs.
            
            ANTI-LOOP RULE: 
            - NEVER call 'searchPamyatNaroda' for a person if you have already received a "Skipped" or "Outside range" result for them.
            - If the history already contains results from 'listArchiveGuides' and 'listMethodologyGuides', do NOT call them again.
            - Once you have the profiles and guide lists, synthesize your analysis and finish this phase.
            
            If you do not call 'listArchiveGuides', you will not know which sources to search, and the process will fail.
        """.trimIndent()
                                )
                        }
                val scanRequest: AIAgentNodeBase<String, Message.Assistant> by
                        nodeLLMRequest("Scanner")

                // 2. DISCOVERY PHASE
                val discoveryPrompt: AIAgentNodeBase<String, String> by
                        nodeAppendPrompt("DiscoveryInstructions") {
                                onLog("🔎 [GRAPH] Entering DISCOVERY phase")
                                system(
                                        """
            You are the DISCOVERY phase of the Genealogy Research protocol.
            
            CRITICAL REQUIREMENT: You MUST use the results of 'listArchiveGuides' from the history to select which guides to read.
            
            STRICT FORBIDDEN ACTION: Do NOT write tool calls as text. Use the Function Calling API.
            Do NOT guess or invent filenames. You MUST look at the verbatim output of 'listArchiveGuides' and use the EXACT filename provided (e.g., 'russia.md').
            
            TASK: Read ALL relevant guides for the regions found in Scan phase.
            - Call 'readArchiveGuide(fileName="...")' for EVERY available country.
            - Call 'readMethodology' for research protocols.
            
            Do NOT finish this phase until you have read the content of all relevant guides.
            Once you have all data, build a research plan. DO NOT START specialized searches YET.
            
            STRICT PLANNING RULE: 
            - For individuals born 1890-1930 (USSR/Russia), your FIRST search step MUST be 'searchPamyatNaroda'.
            - For ALL individuals, perform search with 'queryFamilySearch'. This tool is ESSENTIAL for finding parents and family connections.
            - Do NOT plan any other searches (e.g. Find a Grave, Ancestry, General Search) until these specialized tools are searched.
        """.trimIndent()
                                )
                        }
                val discoveryRequest: AIAgentNodeBase<String, Message.Assistant> by
                        nodeLLMRequest("Discovery")

                // 3. RESEARCH PHASE
                val researchPrompt: AIAgentNodeBase<String, String> by
                        nodeAppendPrompt("ResearchInstructions") {
                                onLog("🌐 [GRAPH] Entering RESEARCH phase")
                                system(
                                        """
            You are the RESEARCH phase of the Genealogy Research protocol.
            
            STRICT FORBIDDEN ACTION: Do NOT write tool calls as text. Use the Function Calling API.
            
            TASK: Execute targeted searches for EACH person and location identified in the research plan.
            - WWII ARCHIVES: Use 'searchPamyatNaroda' ONLY for individuals who were born or lived in the USSR / Russian Empire regions (e.g., Russia, Ukraine, Belarus, Baltics, Caucasus, Central Asia) born approximately 1890-1930. Never call this tool for individuals born or living in America, Western Europe, or other non-Soviet regions. Always pass the 'birthPlace' parameter to the tool.
            - GLOBAL ARCHIVES: For EVERY person, you MUST call 'queryFamilySearch'. Skipping it is a CRITICAL FAILURE. It automatically searches both records and user trees.
                3. NAME PROTOCOL: Surnames and First Names for MEN are treated as EXACT. To maximize matches for men, provide ONLY the first name in 'firstName' (omit patronymic). For WOMEN, First Name is EXACT. You can search by her maiden name as 'lastName' AND her husband's surname as 'spouseLastName' with 'exactMatch=true' to search by husband's surname variant or maiden name exactly. Always pass the 'gender' parameter.
            - COMMON NAMES: If the person has a very common last name (Smith, Jones, Ivanov, Kuznetsov, etc.), you MUST set exactMatch=true. 
            - SURVIVOR POLICY: Do NOT skip people who survived the war for WWII research. 
            - PRIORITY: Archive tools (Pamyat Naroda, FamilySearch) MUST be performed BEFORE any general web searches.
            - Do NOT use 'generalWebSearch' as a substitute if specialized tools apply.
            - Use 'searchFamilyTree' to verify local facts.
            - Every NEW fact must be backed by a verbatim 'Evidence Snippet' from the tool output.
            
            Methodology Skills:
            $methodologySkills
            
            **ANTI-LOOP & AUTO-STOPPING POLICY**: 
            1. Attempt a MAXIMUM of 2 searches per person (e.g. one broad, one exact).
            2. If you find NO records after 2 attempts, ACCEPT the negative result, STOP searching for that person, and move on.
            3. Do NOT fall into an infinite loop of calling 'generalWebSearch' or 'queryFamilySearch' with slightly tweaked parameters.
            4. Once all persons have been searched at least once (or skipped if irrelevant), you MUST FINISH this phase by providing a substantive summary WITHOUT making any tool calls.
        """.trimIndent()
                                )
                        }
                val researchRequest: AIAgentNodeBase<String, Message.Assistant> by
                        nodeLLMRequest("Research")

                // 4. FINALIZE PHASE
                val finalizePrompt: AIAgentNodeBase<String, String> by
                        nodeAppendPrompt("FinalizeInstructions") {
                                onLog("📝 [GRAPH] Entering FINALIZE phase")
                                val findingsText =
                                        if (accumulatedFindings.isEmpty()) {
                                                "No specific military records found during this run."
                                        } else {
                                                accumulatedFindings.joinToString("\n\n")
                                        }

                                system(
                                        """
            You are the FINALIZER phase of the Genealogy Research protocol. 
            
            TASK: Take all research findings from the conversation history and the original <Family_Tree> and format them into a comprehensive final report. 
            
            ### ACCUMULATED SEARCH EVIDENCE (DO NOT MISS THIS):
            The following records were found and stored in your research memory:
            $findingsText
            
            IMPORTANT: 
            - You MUST include ALL the individual records listed above in your report. Do not skip names.
            - Tools are now DISABLED. Do NOT attempt to use tools.
            - Provide the final results in text NOW. 
            - Use the following format:
            1. [NEW] for new facts (not in the original tree).
            2. Evidence Snippets for every fact.
            3. Exact [SOURCES] with verbatim URLs.
            
            Do NOT hallucinate or guess URLs.
        """.trimIndent()
                                )
                        }
                val finalizeRequest: AIAgentNodeBase<String, Message.Assistant> by
                        nodeLLMRequestWithoutTools("Finalizer")

                // Extraction Node: Convert Message.Assistant to String
                val nodeExtractResult: AIAgentNodeBase<Message.Assistant, String> by
                        node<Message.Assistant, String>("Extractor") { response: Message.Assistant ->
                                val text =
                                        response.parts.filterIsInstance<MessagePart.Text>()
                                                .joinToString("") { it.text }
                                onLog("Final extraction result length: ${text.length}")
                                text
                        }

                // Helper to collect findings from tool results in any phase
                fun collectFindings(result: ai.koog.agents.core.dsl.extension.ReceivedToolResults): ai.koog.agents.core.dsl.extension.ReceivedToolResults {
                        result.toolResults.forEach { singleResult ->
                                val content = singleResult.output
                                if (content.contains("Consolidated Military Record") ||
                                                 content.contains("military records extracted") ||
                                                 content.contains("NEW fact") ||
                                                 content.contains("FamilySearch") ||
                                                 content.contains("Pamyat Naroda") ||
                                                 (content.contains("Birth Date") &&
                                                          content.length > 50)
                                ) {
                                        val taggedContent = when {
                                                content.contains("FamilySearch") -> "[FamilySearch] $content"
                                                content.contains("Pamyat Naroda") || content.contains("Consolidated Military Record") -> "[Pamyat Naroda] $content"
                                                else -> content
                                        }
                                        if (accumulatedFindings.none { it.take(20) == taggedContent.take(20) }) {
                                                accumulatedFindings.add(taggedContent)
                                                onLog("📦 [STORAGE] Stored finding in accumulator (Total: ${accumulatedFindings.size})")
                                        }
                                }
                        }
                        return result
                }

                val scanFindingsCollector by node("ScanFindingsCollector") { r: ai.koog.agents.core.dsl.extension.ReceivedToolResults -> collectFindings(r) }
                val discoveryFindingsCollector by node("DiscoveryFindingsCollector") { r: ai.koog.agents.core.dsl.extension.ReceivedToolResults -> collectFindings(r) }
                val researchFindingsCollector by node("ResearchFindingsCollector") { r: ai.koog.agents.core.dsl.extension.ReceivedToolResults -> collectFindings(r) }

                // Phase-specific Tool execution nodes
                val scanExecuteTool by nodeExecuteTools("ScanToolExecutor")
                val scanSendToolResult by nodeLLMSendToolResults("ScanToolResultSender")

                val discoveryExecuteTool by nodeExecuteTools("DiscoveryToolExecutor")
                val discoverySendToolResult by nodeLLMSendToolResults("DiscoveryToolResultSender")

                val researchExecuteTool by nodeExecuteTools("ResearchToolExecutor")
                val researchSendToolResult by nodeLLMSendToolResults("ResearchToolResultSender")

                // --- GRAPH FLOW ---

                // Start -> Scan
                edge(nodeStart.forwardTo<String>(scanPrompt))
                edge(scanPrompt.forwardTo<String>(scanRequest))

                // Scan Loop (Tool calling)
                edge(scanRequest.forwardTo(scanExecuteTool).onToolCalls { true })
                edge(scanExecuteTool.forwardTo(scanFindingsCollector))
                edge(scanFindingsCollector.forwardTo(scanSendToolResult))
                edge(scanSendToolResult.forwardTo(scanExecuteTool).onToolCalls { true })
                edge(
                        scanSendToolResult.forwardTo<String>(scanRequest)
                                .onCondition { !it.parts.any { it is MessagePart.Tool.Call } }
                                .transformed { it.asString() }
                )

                // Helper to detect if a message looks like a halluncinated tool list instead of a
                // real
                // response
                fun isSubstantiveResponse(it: Message.Assistant): Boolean {
                        // If there are tool calls, we must execute them in the current phase first
                        if (it.parts.any { it is MessagePart.Tool.Call }) {
                                return false
                        }
                        val text = it.asString().lowercase()
                        // If it starts with a tool-like list but has no other content, it's
                        // probably
                        // hallucinated tools
                        if (text.contains("geographicprofile") ||
                                        text.contains("listarchiveguides") ||
                                        text.contains("readarchiveguide")
                        ) {
                                if (text.length < 200)
                                        return false // Too short to be a real analysis
                        }
                        return true
                }

                // Scan Done -> Discovery (Transform Response -> String)
                edge(
                        scanRequest
                                .forwardTo<String>(discoveryPrompt)
                                .onCondition { isSubstantiveResponse(it) }
                                .transformed { "### RESULTS FROM SCANNER\n${it.asString()}" }
                )
                edge(discoveryPrompt.forwardTo<String>(discoveryRequest))

                // Discovery Loop (Tool calling)
                edge(
                        discoveryRequest.forwardTo(
                                discoveryExecuteTool
                        ).onToolCalls { true }
                )
                edge(discoveryExecuteTool.forwardTo(discoveryFindingsCollector))
                edge(discoveryFindingsCollector.forwardTo(discoverySendToolResult))
                edge(discoverySendToolResult.forwardTo(discoveryExecuteTool).onToolCalls { true })
                // Return to Discovery request, transforming Tool Result Response back to String
                edge(
                        discoverySendToolResult.forwardTo<String>(discoveryRequest)
                                .onCondition { !it.parts.any { it is MessagePart.Tool.Call } }
                                .transformed { it.asString() }
                )

                // Discovery Done -> Research (Transform Response -> String)
                edge(
                        discoveryRequest
                                .forwardTo<String>(researchPrompt)
                                .onCondition { isSubstantiveResponse(it) }
                                .transformed { "### RESEARCH PLAN TO EXECUTE\n${it.asString()}" }
                )
                edge(researchPrompt.forwardTo<String>(researchRequest))

                // Research Loop (Execute -> Collect -> Send Result)
                edge(
                        researchRequest.forwardTo(researchExecuteTool).onToolCalls {
                                true
                        }
                )
                edge(researchExecuteTool.forwardTo(researchFindingsCollector))
                edge(researchFindingsCollector.forwardTo(researchSendToolResult))
                edge(researchSendToolResult.forwardTo(researchExecuteTool).onToolCalls { true })
                edge(
                        researchSendToolResult.forwardTo<String>(researchRequest)
                                .onCondition { !it.parts.any { it is MessagePart.Tool.Call } }
                                .transformed { it.asString() }
                )

                // Research Done -> Finalize (Transform Response -> String)
                edge(
                        researchRequest
                                .forwardTo<String>(finalizePrompt)
                                .onCondition { isSubstantiveResponse(it) }
                                .transformed { "### RESEARCH FINDINGS\n${it.asString()}" }
                )
                edge(finalizePrompt.forwardTo<String>(finalizeRequest))

                // Check if we also need to pass the original tree context to Finalize via history
                // or
                // user message.
                // It's already in the system prompt of researchPrompt, but finalizePrompt is a new
                // system prompt.
                // In Koog, history is preserved.

                // Finalize -> Extract -> Finish
                edge(finalizeRequest.forwardTo<Message.Assistant>(nodeExtractResult))
                edge(nodeExtractResult.forwardTo<String>(nodeFinish))
        }

/** Extension to extract text content from a Message.Response or Message. */
private fun Any?.asString(): String {
        val assistant = this as? Message.Assistant ?: return ""
        return assistant.parts.filterIsInstance<MessagePart.Text>()
                .joinToString("") { it.text }
}
