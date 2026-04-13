package com.family.tree.core.platform

/**
 * Platform-specific resource loader for external genealogy data.
 * Uses standard platform APIs (ClassLoader, NSBundle, Fetch) for cross-platform access.
 */
expect class ResourceLoader() {
    /**
     * Reads all prompts from the /autoresearch-genealogy/prompts directory.
     * Uses a manifest.json file for file discovery.
     */
    suspend fun listPromptFiles(repoPath: String): List<String>

    /**
     * Reads the content of a specific file.
     */
    suspend fun readFile(basePath: String, relativePath: String): String?

    /**
     * Lists all files in a specific subdirectory of the project.
     * Uses manifest.json for discovery.
     */
    suspend fun listDirectory(repoPath: String, subDir: String): List<String>
}
