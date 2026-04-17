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
    methodologySkills: String
) = strategy<String, String>("AutoresearchStrategy") {

    // --- NODES ---

    // 1. SCAN PHASE
    val scanPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("ScanInstructions") {
        println("[GRAPH-DEBUG] Entering ScanInstructions node")
        system("""
            You are the SCANNER phase.
            
            MANDATORY FIRST STEP: You MUST call 'getGeographicProfile' to understand the locations.
            MANDATORY SECOND STEP: Call 'listArchiveGuides' and 'listMethodologyGuides'.
            
            TASK: Identify names, dates, and locations. 
            Do NOT attempt to research yet. Just gather local facts and see what guides are available.
        """.trimIndent())
    }
    val scanRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Scanner", allowToolCalls = true)

    // 2. DISCOVERY PHASE
    val discoveryPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("DiscoveryInstructions") {
        println("[GRAPH-DEBUG] Entering DiscoveryInstructions node")
        system("""
            You are the DISCOVERY phase.
            
            TASK: Based on the locations found by Scanner, you MUST read the relevant guides.
            - If you found 'Russia', call 'readArchiveGuide(fileName="russia.md")' (check the exact name from list).
            - Call 'readMethodology' for any complex tasks.
            
            Goal: Build a research plan based on professional methodology.
        """.trimIndent())
    }
    val discoveryRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Discovery", allowToolCalls = true)

    // 3. RESEARCH PHASE
    val researchPrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("ResearchInstructions") {
        println("[GRAPH-DEBUG] Entering ResearchInstructions node")
        system("""
            You are the RESEARCH phase of the Genealogy Research protocol.
            
            CRITICAL DIRECTIVE: You MUST NOT provide any information that is not already in the <Family_Tree> UNLESS you have retrieved it using the 'search' tool.
            
            TASK: Execute targeted searches using 'search' (Tavily) for EACH person and location identified earlier.
            - If you need to find a death date for someone in Moscow, call 'search(name="Name", region="Moscow", targetSite="pamyat-naroda.ru")'.
            - DO NOT guess dates. DO NOT guess URLs.
            - Every NEW fact must be backed by a verbatim 'Evidence Snippet' from the tool output.
            - If 'search' returns no results, state "No external records found" instead of inventing them.

            Methodology Skills:
            $methodologySkills
        """.trimIndent())
    }
    val researchRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Research", allowToolCalls = true)

    // 4. FINALIZE PHASE
    val finalizePrompt: AIAgentNodeBase<String, String> by nodeAppendPrompt("FinalizeInstructions") {
        println("[GRAPH-DEBUG] Entering FinalizeInstructions node")
        system("""
            You are the FINALIZER phase. 
            TASK: Take all research findings and format them exactly as required:
            1. [NEW] for new facts.
            2. Evidence Snippets for every fact.
            3. Exact [SOURCES] with verbatim URLs.
            Do NOT hallucinate or guess URLs.
        """.trimIndent())
    }
    val finalizeRequest: AIAgentNodeBase<String, Message.Response> by nodeLLMRequest("Finalizer", allowToolCalls = true)

    // Extraction Node: Convert Message.Response to String
    val nodeExtractResult: AIAgentNodeBase<Message.Response, String> by node<Message.Response, String>("Extractor") { response: Message.Response ->
        (response as Message.Assistant).parts
            .filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()
            .joinToString("") { it.text }
    }

    // Tool execution nodes
    val executeTool: AIAgentNodeBase<Message.Tool.Call, ReceivedToolResult> by nodeExecuteTool("ToolExecutor")
    val sendToolResult: AIAgentNodeBase<ReceivedToolResult, Message.Response> by nodeLLMSendToolResult("ToolResultSender")

    // --- GRAPH FLOW ---

    // Start -> Scan
    edge(nodeStart.forwardTo<String>(scanPrompt))
    edge(scanPrompt.forwardTo<String>(scanRequest))
    
    // Scan -> Discovery (Transform Response -> String)
    edge(scanRequest.forwardTo<String>(discoveryPrompt).transformed { 
        val assistant = it as? Message.Assistant
        assistant?.parts?.filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()?.joinToString("") { t -> t.text } ?: ""
    })
    edge(discoveryPrompt.forwardTo<String>(discoveryRequest))

    // Discovery Loop (Tool calling)
    edge(discoveryRequest.forwardTo<Message.Tool.Call>(executeTool) onToolCall { true })
    edge(executeTool.forwardTo<ReceivedToolResult>(sendToolResult))
    // Return to Discovery request, transforming Tool Result Response back to String
    edge(sendToolResult.forwardTo<String>(discoveryRequest).transformed { 
        val assistant = it as? Message.Assistant
        assistant?.parts?.filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()?.joinToString("") { t -> t.text } ?: ""
    })
    
    // Discovery Done -> Research (Transform Response -> String)
    edge(discoveryRequest.forwardTo<String>(researchPrompt).onAssistantMessage { true }.transformed { 
        val assistant = it as? Message.Assistant
        assistant?.parts?.filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()?.joinToString("") { t -> t.text } ?: ""
    })
    edge(researchPrompt.forwardTo<String>(researchRequest))

    // Research Loop (Tool calling)
    edge(researchRequest.forwardTo<Message.Tool.Call>(executeTool) onToolCall { true })
    edge(executeTool.forwardTo<ReceivedToolResult>(sendToolResult))
    edge(sendToolResult.forwardTo<String>(researchRequest).transformed { 
        val assistant = it as? Message.Assistant
        assistant?.parts?.filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()?.joinToString("") { t -> t.text } ?: ""
    })

    // Research Done -> Finalize (Transform Response -> String)
    edge(researchRequest.forwardTo<String>(finalizePrompt).onAssistantMessage { true }.transformed { 
        val assistant = it as? Message.Assistant
        assistant?.parts?.filterIsInstance<ai.koog.prompt.message.ContentPart.Text>()?.joinToString("") { t -> t.text } ?: ""
    })
    edge(finalizePrompt.forwardTo<String>(finalizeRequest))

    // Finalize -> Extract -> Finish
    edge(finalizeRequest.forwardTo<Message.Response>(nodeExtractResult))
    edge(nodeExtractResult.forwardTo<String>(nodeFinish))
}
