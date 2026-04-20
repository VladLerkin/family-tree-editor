package com.family.tree.core.ai.agent

import ai.koog.agents.core.dsl.builder.*
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.prompt.message.Message

/**
 * Autoresearch Genealogy Strategy implemented as a multi-node Koog Graph.
 */
fun createAutoresearchStrategy(
    instructions: String,
    familyTreeContext: String,
    methodologySkills: String,
    onLog: (String) -> Unit = {}
) = strategy<String, String>("AutoresearchStrategy") {

    // --- NODES ---

    // 1. SCAN PHASE
    val scanPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("ScanInstructions") {
        onLog("🔍 [GRAPH] Entering SCAN phase")
        system("""
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
            
            If you do not call 'listArchiveGuides', you will not know which sources to search, and the process will fail.
        """.trimIndent())
    }
    val scanRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Scanner", allowToolCalls = true)

    // 2. DISCOVERY PHASE
    val discoveryPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("DiscoveryInstructions") {
        onLog("🔎 [GRAPH] Entering DISCOVERY phase")
        system("""
            You are the DISCOVERY phase of the Genealogy Research protocol.
            
            CRITICAL REQUIREMENT: You MUST use the results of 'listArchiveGuides' from the history to select which guides to read.
            
            STRICT FORBIDDEN ACTION: Do NOT write tool calls as text. Use the Function Calling API.
            Do NOT guess or invent filenames. You MUST look at the verbatim output of 'listArchiveGuides' and use the EXACT filename provided (e.g., 'russia.md').
            
            TASK: Read ALL relevant guides for the regions found in Scan phase.
            - Call 'readArchiveGuide(fileName="...")' for EVERY available country.
            - Call 'readMethodology' for research protocols.
            
            Do NOT finish this phase until you have read the content of all relevant guides.
            Once you have all data, build a research plan. DO NOT START specialized searches YET.
        """.trimIndent())
    }
    val discoveryRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Discovery", allowToolCalls = true)

    // 3. RESEARCH PHASE
    val researchPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("ResearchInstructions") {
        onLog("🌐 [GRAPH] Entering RESEARCH phase")
        system("""
            You are the RESEARCH phase of the Genealogy Research protocol.
            
            STRICT FORBIDDEN ACTION: Do NOT write tool calls as text. Use the Function Calling API.
            
            TASK: Execute targeted searches for EACH person and location identified in the research plan.
            - WWII ARCHIVES FILTER: You MUST use 'searchPamyatNaroda' and 'searchOBDMemorial' ONLY for individuals from the USSR/Russia born approximately between 1880 and 1930. 
            - For individuals born before 1880 or after 1930, do NOT use WWII military tools.
            - Use 'searchFamilyTree' to verify local facts.
            - Every NEW fact must be backed by a verbatim 'Evidence Snippet' from the tool output.
            
            Methodology Skills:
            $methodologySkills
            
            **AUTO-STOPPING POLICY**: Do NOT stop until you have attempted at least one specific search for EACH geographic region identified in the plan.
        """.trimIndent())
    }
    val researchRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Research", allowToolCalls = true)

    // 4. FINALIZE PHASE
    val finalizePrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("FinalizeInstructions") {
        onLog("📝 [GRAPH] Entering FINALIZE phase")
        system("""
            You are the FINALIZER phase of the Genealogy Research protocol. 
            
            TASK: Take all research findings from the conversation history and the original <Family_Tree> and format them into a comprehensive final report. 
            
            IMPORTANT: 
            - Tools are now DISABLED. Do NOT attempt to use tools.
            - Provide the final results in text NOW. 
            - Use the following format:
            1. [NEW] for new facts (not in the original tree).
            2. Evidence Snippets for every fact.
            3. Exact [SOURCES] with verbatim URLs.
            
            Do NOT hallucinate or guess URLs.
        """.trimIndent())
    }
    val finalizeRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Finalizer", allowToolCalls = false)

    // Extraction Node: Convert Message.Response to String
    val nodeExtractResult: AIAgentNodeBase<Message.Response, String> by node<Message.Response, String>("Extractor") { response: Message.Response ->
        val text = (response as Message.Assistant).parts
            .filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()
            .joinToString("") { it.text }
        onLog("Final extraction result length: ${text.length}")
        text
    }

    // Phase-specific Tool execution nodes
    val scanExecuteTool: AIAgentNodeBase<Message.Tool.Call, ReceivedToolResult> by nodeExecuteTool("ScanToolExecutor")
    val scanSendToolResult: AIAgentNodeBase<ReceivedToolResult, Message.Response> by nodeLLMSendToolResult("ScanToolResultSender")

    val discoveryExecuteTool: AIAgentNodeBase<Message.Tool.Call, ReceivedToolResult> by nodeExecuteTool("DiscoveryToolExecutor")
    val discoverySendToolResult: AIAgentNodeBase<ReceivedToolResult, Message.Response> by nodeLLMSendToolResult("DiscoveryToolResultSender")

    val researchExecuteTool: AIAgentNodeBase<Message.Tool.Call, ReceivedToolResult> by nodeExecuteTool("ResearchToolExecutor")
    val researchSendToolResult: AIAgentNodeBase<ReceivedToolResult, Message.Response> by nodeLLMSendToolResult("ResearchToolResultSender")

    // --- GRAPH FLOW ---

    // Start -> Scan
    edge(nodeStart.forwardTo<String>(scanPrompt))
    edge(scanPrompt.forwardTo<String>(scanRequest))

    // Scan Loop (Tool calling)
    edge(scanRequest.forwardTo<Message.Tool.Call>(scanExecuteTool) onToolCall { true })
    edge(scanExecuteTool.forwardTo<ReceivedToolResult>(scanSendToolResult))
    edge(scanSendToolResult.forwardTo<String>(scanRequest).transformed { it.asString() })

    // Helper to detect if a message looks like a halluncinated tool list instead of a real response
    fun isSubstantiveResponse(it: Message.Response): Boolean {
        val text = it.asString().lowercase()
        // If it starts with a tool-like list but has no other content, it's probably hallucinated tools
        if (text.contains("geographicprofile") || text.contains("listarchiveguides") || text.contains("readarchiveguide")) {
            if (text.length < 200) return false // Too short to be a real analysis
        }
        return true
    }

    // Scan Done -> Discovery (Transform Response -> String)
    edge(scanRequest.forwardTo<String>(discoveryPrompt).onAssistantMessage { isSubstantiveResponse(it) }.transformed {
        "### RESULTS FROM SCANNER\n${it.asString()}"
    })
    edge(discoveryPrompt.forwardTo<String>(discoveryRequest))

    // Discovery Loop (Tool calling)
    edge(discoveryRequest.forwardTo<Message.Tool.Call>(discoveryExecuteTool) onToolCall { true })
    edge(discoveryExecuteTool.forwardTo<ReceivedToolResult>(discoverySendToolResult))
    // Return to Discovery request, transforming Tool Result Response back to String
    edge(discoverySendToolResult.forwardTo<String>(discoveryRequest).transformed { it.asString() })
    
    // Discovery Done -> Research (Transform Response -> String)
    edge(discoveryRequest.forwardTo<String>(researchPrompt).onAssistantMessage { isSubstantiveResponse(it) }.transformed { 
        "### RESEARCH PLAN TO EXECUTE\n${it.asString()}"
    })
    edge(researchPrompt.forwardTo<String>(researchRequest))

    // Research Loop (Tool calling)
    edge(researchRequest.forwardTo<Message.Tool.Call>(researchExecuteTool) onToolCall { true })
    edge(researchExecuteTool.forwardTo<ReceivedToolResult>(researchSendToolResult))
    edge(researchSendToolResult.forwardTo<String>(researchRequest).transformed { it.asString() })

    // Research Done -> Finalize (Transform Response -> String)
    edge(researchRequest.forwardTo<String>(finalizePrompt).onAssistantMessage { isSubstantiveResponse(it) }.transformed { 
        "### RESEARCH FINDINGS\n${it.asString()}"
    })
    edge(finalizePrompt.forwardTo<String>(finalizeRequest))
    
    // Check if we also need to pass the original tree context to Finalize via history or user message.
    // It's already in the system prompt of researchPrompt, but finalizePrompt is a new system prompt.
    // In Koog, history is preserved.

    // Finalize -> Extract -> Finish
    edge(finalizeRequest.forwardTo<Message.Response>(nodeExtractResult))
    edge(nodeExtractResult.forwardTo<String>(nodeFinish))
}

/**
 * Extension to extract text content from a Message.Response or Message.
 */
private fun Any?.asString(): String {
    val message = this as? Message.Response ?: return ""
    val assistant = message as? Message.Assistant ?: return ""
    return assistant.parts
        .filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()
        .joinToString("") { it.text }
}
