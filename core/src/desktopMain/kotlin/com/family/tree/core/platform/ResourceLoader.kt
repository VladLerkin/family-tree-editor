package com.family.tree.core.platform

import kotlinx.serialization.json.*

actual class ResourceLoader actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    actual suspend fun listPromptFiles(repoPath: String): List<String> {
        return listDirectory(repoPath, "prompts")
    }

    actual suspend fun listDirectory(repoPath: String, subDir: String): List<String> {
        return try {
            val manifestContent = readResourceText("autoresearch-genealogy/manifest.json")
            if (manifestContent == null) return emptyList()
            val manifest = json.parseToJsonElement(manifestContent).jsonObject
            manifest[subDir]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun readFile(basePath: String, relativePath: String): String? {
        val path = if (basePath.isEmpty()) relativePath else "$basePath/$relativePath"
        // Remove 'files/' if it exists (legacy path)
        val cleanPath = path.removePrefix("files/").removePrefix("/")
        return readResourceText(cleanPath)
    }

    private fun readResourceText(path: String): String? {
        val cleanPath = path.removePrefix("./").removePrefix("/")
        println("[DEBUG_LOG] Desktop ResourceLoader: Probe started for '$cleanPath'")
        
        val classLoader = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader
        
        // Define probes: Standard Compose paths + raw path
        val probes = listOf(
            "composeResources/com.family.tree.ui/files/$cleanPath",
            "composeResources/com.family.tree.ui.generated.resources/files/$cleanPath",
            "composeResources/family_tree_kmp.ui.generated.resources/files/$cleanPath",
            "composeResources/files/$cleanPath",
            "composeResources/$cleanPath",
            "compose-resources/files/$cleanPath",
            "compose-resources/$cleanPath",
            "files/$cleanPath",
            cleanPath
        )
        
        for (probePath in probes) {
            println("[DEBUG_LOG] Desktop ResourceLoader: Trying probe '$probePath'")
            val stream = classLoader.getResourceAsStream(probePath) 
                ?: this::class.java.getResourceAsStream("/$probePath")
                ?: classLoader.getResourceAsStream("/$probePath")
            
            if (stream != null) {
                println("[DEBUG_LOG] Desktop ResourceLoader: SUCCESS! Found at '$probePath'")
                return stream.bufferedReader().use { it.readText() }
            }
        }
        
        // Final fallback: try to find anything that looks like our manifest
        println("[DEBUG_LOG] Desktop ResourceLoader: FAILED to find '$cleanPath' after all probes.")
        return null
    }
}
