package com.family.tree.core.platform

import kotlinx.serialization.json.*

actual class ResourceLoader actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Cache for the manifest content to avoid re-reading it multiple times
        private var cachedManifestContent: String? = null
        
        // Cache the last successful prefix to prioritize it in future probes
        private var successfulPrefix: String? = null
    }

    actual suspend fun listPromptFiles(repoPath: String): List<String> {
        return listDirectory(repoPath, "prompts")
    }

    actual suspend fun listDirectory(repoPath: String, subDir: String): List<String> {
        return try {
            val manifestContent = cachedManifestContent ?: readResourceText("autoresearch-genealogy/manifest.json")
            if (manifestContent == null) return emptyList()
            
            // Successfully read, cache it
            cachedManifestContent = manifestContent
            
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
        
        // Use the successful prefix first if we have one
        successfulPrefix?.let { prefix ->
            val probePath = if (prefix.endsWith("/")) "$prefix$cleanPath" else "$prefix/$cleanPath"
            val stream = tryResolve(classLoader, probePath)
            if (stream != null) {
                // println("[DEBUG_LOG] Desktop ResourceLoader: Fast-match SUCCESS at '$probePath'")
                return stream.bufferedReader().use { it.readText() }
            }
        }

        // Define probes: Standard Compose paths + raw path
        val probePrefixes = listOf(
            "composeResources/com.family.tree.ui/files/",
            "composeResources/com.family.tree.ui.generated.resources/files/",
            "composeResources/family_tree_kmp.ui.generated.resources/files/",
            "composeResources/files/",
            "composeResources/",
            "compose-resources/files/",
            "compose-resources/",
            "files/",
            ""
        )
        
        for (prefix in probePrefixes) {
            val probePath = if (prefix.isEmpty()) cleanPath else "$prefix$cleanPath"
            println("[DEBUG_LOG] Desktop ResourceLoader: Trying probe '$probePath'")
            val stream = tryResolve(classLoader, probePath)
            
            if (stream != null) {
                println("[DEBUG_LOG] Desktop ResourceLoader: SUCCESS! Found at '$probePath'")
                // Cache the prefix part (everything before the cleanPath)
                successfulPrefix = probePath.removeSuffix(cleanPath)
                return stream.bufferedReader().use { it.readText() }
            }
        }
        
        println("[DEBUG_LOG] Desktop ResourceLoader: FAILED to find '$cleanPath' after all probes.")
        return null
    }

    private fun tryResolve(classLoader: ClassLoader, path: String): java.io.InputStream? {
        return classLoader.getResourceAsStream(path) 
            ?: this::class.java.getResourceAsStream("/$path")
            ?: classLoader.getResourceAsStream("/$path")
    }
}
