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
        val cleanPath = path.removePrefix("files/").removePrefix("/")
        return readResourceText(cleanPath)
    }

    private fun readResourceText(path: String): String? {
        val cleanPath = path.removePrefix("./").removePrefix("/")
        println("[DEBUG_LOG] Android ResourceLoader: Probe started for '$cleanPath'")
        
        // Define probes: Standard Compose paths + Project specific + Android assets + raw path
        val probes = listOf(
            "composeResources/com.family.tree.ui/files/$cleanPath",
            "composeResources/family_tree_kmp.ui.generated.resources/files/$cleanPath",
            "composeResources/com.family.tree.ui.generated.resources/files/$cleanPath",
            "composeResources/ui.generated.resources/files/$cleanPath",
            "assets/composeResources/family_tree_kmp.ui.generated.resources/files/$cleanPath",
            "assets/composeResources/com.family.tree.ui.generated.resources/files/$cleanPath",
            "assets/composeResources/ui.generated.resources/files/$cleanPath",
            "composeResources/files/$cleanPath",
            "assets/composeResources/files/$cleanPath",
            "assets/files/$cleanPath",
            "files/$cleanPath",
            "composeResources/$cleanPath",
            "assets/$cleanPath",
            cleanPath
        )
        
        val classLoader = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader
        
        for (probePath in probes) {
            println("[DEBUG_LOG] Android ResourceLoader: Trying probe '$probePath'")
            
            // Try different ways to get the stream
            val stream = classLoader.getResourceAsStream(probePath)
                ?: this::class.java.classLoader.getResourceAsStream(probePath)
                ?: this::class.java.getResourceAsStream("/$probePath")
                ?: this::class.java.getResourceAsStream(probePath)
                
            if (stream != null) {
                println("[DEBUG_LOG] Android ResourceLoader: SUCCESS! Found at '$probePath'")
                return try {
                    stream.bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    println("[DEBUG_LOG] Android ResourceLoader: Error reading stream: ${e.message}")
                    null
                }
            }
        }
        
        println("[DEBUG_LOG] Android ResourceLoader: FAILED to find '$cleanPath' after all probes.")
        return null
    }
}
