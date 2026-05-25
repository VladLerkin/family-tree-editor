package com.family.tree.desktop

import com.family.tree.core.di.initKoin
import com.family.tree.core.di.platformModule
import com.family.tree.core.ai.agent.AgentService
import com.family.tree.core.gedcom.GedcomTest
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

fun main() {
    println("Initializing Koin...")
    initKoin(additionalModules = listOf(platformModule))
    
    val koin = GlobalContext.get()
    val agentService = koin.get<AgentService>()
    
    val config = agentService.loadConfig()
    println("Loaded configuration:")
    println("  Provider: ${config.provider}")
    println("  Model: ${config.model}")
    println("  OpenAI API Key: ${if (config.getApiKeyForProvider().isBlank()) "EMPTY" else "PRESENT"}")
    println("  Tavily API Key: ${if (config.tavilyApiKey.isBlank()) "EMPTY" else "PRESENT"}")
    println("  Repo Path: ${config.autoresearchRepoPath}")
    
    // Set environment variable fallback if keys are empty in preferences
    val openAiKey = config.getApiKeyForProvider().ifBlank { System.getenv("OPENAI_API_KEY") ?: "" }
    val tavilyKey = config.tavilyApiKey.ifBlank { System.getenv("TAVILY_API_KEY") ?: "" }
    
    if (config.getApiKeyForProvider().isBlank() && openAiKey.isNotBlank()) {
        println("Updating OpenAI API Key from environment...")
        // Save back if empty in preferences
        val settingsStorage = koin.get<com.family.tree.core.ai.AiSettingsStorage>()
        settingsStorage.saveConfig(config.copy(openaiApiKey = openAiKey, tavilyApiKey = tavilyKey))
    }
    
    val projectData = GedcomTest.createTestData()
    println("Created test family tree with ${projectData.individuals.size} individuals and ${projectData.families.size} families.")
    
    runBlocking(kotlinx.coroutines.Dispatchers.Default) {
        println("Running the actual Autoresearch Genealogy agent prompt...")
        try {
            val proposal = agentService.runAutoresearchPrompt(
                projectData = projectData,
                promptName = "Scan",
                promptInstructions = "Identify target ancestors in the family tree and search for them."
            )
            println("Agent execution completed!")
            println("Proposal results:\n${proposal.results}")
        } catch (e: Exception) {
            println("Agent execution threw exception: ${e.message}")
            e.printStackTrace()
        }
    }
}
