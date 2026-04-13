package com.family.tree.core.ai.agent

import com.family.tree.core.platform.ResourceLoader
import kotlinx.serialization.Serializable

@Serializable
data class PromptTemplate(
    val id: String,
    val name: String,
    val description: String,
    val instructions: String
) {
    /**
     * Replaces placeholders like {{VARIABLE}} or [VARIABLE] with actual data.
     */
    fun fillTemplate(variables: Map<String, String>): String {
        var result = instructions
        variables.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
            result = result.replace("[$key]", value)
        }
        return result
    }

    companion object {
        suspend fun loadExternalPrompts(repoPath: String): List<PromptTemplate> {
            val loader = ResourceLoader()
            val files = loader.listPromptFiles(repoPath)
            return files.mapNotNull { filePath ->
                val content = if (filePath.startsWith("/")) {
                    loader.readFile("", filePath)
                } else {
                    // Prepend 'prompts/' because the filenames in the manifest are relative to the prompts dir
                    loader.readFile("$repoPath/prompts", filePath)
                } ?: return@mapNotNull null
                parsePromptFile(filePath, content)
            }
        }

        private fun parsePromptFile(path: String, content: String): PromptTemplate? {
            val lines = content.lines()
            if (lines.isEmpty()) return null
            
            val name = lines.first().removePrefix("#").trim()
            val description = lines.drop(1).firstOrNull { it.isNotBlank() } ?: "No description provided."
            
            // Generate a clean ID from filename
            val id = path.substringAfterLast("/").substringBeforeLast(".md")
            
            return PromptTemplate(
                id = id,
                name = name,
                description = description,
                instructions = content
            )
        }
    }
}
