package com.family.tree.core.ai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Web (WasmJs) implementation of AiSettingsStorage using localStorage.
 * TODO: Implement proper localStorage access when kotlinx-browser supports wasmJs
 */
actual class AiSettingsStorage {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val storageKey = "ai_settings"
    
    actual fun saveConfig(config: AiConfig) {
        try {
            val jsonString = json.encodeToString(config)
            // TODO: Use localStorage when available
            println("[DEBUG_LOG] AiSettingsStorage.wasmJs: Config save not implemented yet")
        } catch (e: Exception) {
            println("Failed to save AI config: ${e.message}")
        }
    }
    
    actual fun loadConfig(): AiConfig {
        return try {
            // TODO: Use localStorage when available
            println("[DEBUG_LOG] AiSettingsStorage.wasmJs: Config load not implemented yet")
            AiConfig()
        } catch (e: Exception) {
            println("Failed to load AI config: ${e.message}")
            AiConfig()
        }
    }
    
    actual fun clearConfig() {
        try {
            // TODO: Use localStorage when available
            println("[DEBUG_LOG] AiSettingsStorage.wasmJs: Config clear not implemented yet")
        } catch (e: Exception) {
            println("Failed to clear AI config: ${e.message}")
        }
    }
}
