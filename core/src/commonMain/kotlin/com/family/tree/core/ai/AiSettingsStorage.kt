package com.family.tree.core.ai

/**
 * Platform-specific storage for AI settings.
 * Implementations use SharedPreferences (Android), UserDefaults (iOS), or Preferences (Desktop).
 */
expect class AiSettingsStorage() {
    /**
     * Saves AI configuration.
     */
    fun saveConfig(config: AiConfig)
    
    /**
     * Loads saved AI configuration, or returns default if not found.
     */
    fun loadConfig(): AiConfig
    
    /**
     * Clears saved AI configuration.
     */
    fun clearConfig()
}
