package com.family.tree.core.ai

/**
 * Interface for providing the platform-specific directory where local AI models should be stored.
 */
interface ModelDirectoryProvider {
    /**
     * Returns the absolute path to the directory for local models.
     */
    fun getDirectory(): String
}
