package com.family.tree.core.platform

import kotlinx.serialization.json.*
import org.koin.core.context.GlobalContext
import android.content.Context

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
        
        val context = GlobalContext.get().get<Context>()
        
        // Define probes: Standard UI module assets + Compose resources fallbacks
        val probes = listOf(
            "files/$cleanPath", // Mapped via Gradle from commonMain/composeResources/files/
            "composeResources/com.family.tree.ui/files/$cleanPath",
            "composeResources/family_tree_kmp.ui.generated.resources/files/$cleanPath",
            "composeResources/files/$cleanPath",
            cleanPath
        )
        
        for (probePath in probes) {
            try {
                context.assets.open(probePath).use { stream ->
                    println("[DEBUG_LOG] Android ResourceLoader: SUCCESS! Found at '$probePath'")
                    return stream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                // Continue to next probe
                println("[DEBUG_LOG] Android ResourceLoader: Probe missed '$probePath' - ${e.message}")
            }
        }
        
        println("[DEBUG_LOG] Android ResourceLoader: FAILED to find '$cleanPath' in assets after all probes.")
        return null
    }
}


