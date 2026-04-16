package com.family.tree.core.ai.agent

import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.edge
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.llm.LLMMessage
import ai.koog.agents.core.llm.LLMRole

/**
 * Autoresearch Genealogy Strategy implemented as a multi-node Koog Graph.
 */
fun createAutoresearchStrategy(
    instructions: String,
    familyTreeContext: String,
    methodologySkills: String // The big methodology block we have now
) = strategy<Unit, String>("AutoresearchStrategy") {

    // 1. SCAN PHASE: Analyze local context
    val nodeScan by nodeLLMRequest("Scanner") {
        system("""
            You are the SCANNER phase of the Genealogy Research protocol.
            
            TASK: Analyze the provided <Family_Tree> context and identify specifically:
            - Names of individuals in scope.
            - Their birth/death years.
            - Geographic locations mentioned.
            
            Produce a brief research plan mentioning which regions need archive guide lookup.
            
            CONTEXT:
            $familyTreeContext
        """.trimIndent())
    }

    // 2. DISCOVERY PHASE: Consult guides
    val nodeDiscovery by nodeLLMRequest("Discovery") {
        system("""
            You are the DISCOVERY phase of the Genealogy Research protocol.
            
            TASK: Use tools to find relevant archive guides and methodology guides for the regions identified in the SCAN phase.
            - MUST use 'listArchiveGuides' and 'readArchiveGuide'.
            - MUST use 'listMethodologyGuides' and 'readMethodology'.
            
            Do NOT perform web searches yet. Only gather local archival and methodological knowledge.
        """.trimIndent())
    }

    // 3. RESEARCH PHASE: Execute targeted searches
    val nodeResearch by nodeLLMRequest("Research") {
        system("""
            You are the RESEARCH phase of the Genealogy Research protocol.
            
            TASK: Execute targeted searches using 'search' (Tavily).
            - Apply the methodology found in DISCOVERY.
            - Follow the professional standards (Tiered Source hierarchy).
            - Use Cyrillic for Russian/Ukrainian/Belarusian ancestors.
            
            Methodology Skills:
            $methodologySkills
        """.trimIndent())
    }

    // 4. FINALIZE PHASE: Format the proposal
    val nodeFinalize by nodeLLMRequest("Finalizer") {
        system("""
            You are the FINALIZER phase. 
            
            TASK: Take all research findings and format them exactly as required:
            1. [NEW] for new facts.
            2. Evidence Snippets for every fact.
            3. Exact [SOURCES] with verbatim URLs.
            
            Do NOT hallucinate or guess URLs.
        """.trimIndent())
    }

    // Tool execution nodes (Predefined)
    val executeTool by nodeExecuteTool("ToolExecutor")
    val sendToolResult by nodeLLMSendToolResult("ToolResultSender")

    // --- GRAPH FLOW ---

    // Start -> Scan -> Discovery
    edge(nodeStart forwardTo nodeScan)
    edge(nodeScan forwardTo nodeDiscovery)

    // Discovery Loop (Tool calling)
    edge(nodeDiscovery forwardTo executeTool onToolCall { true })
    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo nodeDiscovery)
    
    // Discovery Done -> Research
    edge(nodeDiscovery forwardTo nodeResearch onToolCall { false })

    // Research Loop (Tool calling)
    edge(nodeResearch forwardTo executeTool onToolCall { true })
    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo nodeResearch)

    // Research Done -> Finalize
    edge(nodeResearch forwardTo nodeFinalize onToolCall { false })

    // Finalize -> Finish
    edge(nodeFinalize forwardTo nodeFinish)
}
