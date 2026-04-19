package com.family.tree.core.platform

import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ResourceLoader actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private var cachedManifestContent: String? = null
        private var successfulPrefix: String? = null
    }

    actual suspend fun listPromptFiles(repoPath: String): List<String> {
        return listDirectory(repoPath, "prompts")
    }

    actual suspend fun listDirectory(repoPath: String, subDir: String): List<String> {
        return try {
            val manifestContent = cachedManifestContent ?: readResourceText("autoresearch-genealogy/manifest.json")
            if (manifestContent == null) return emptyList()
            
            cachedManifestContent = manifestContent
            
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

    private suspend fun readResourceText(path: String): String? = withContext(Dispatchers.IO) {
        val cleanPath = path.removePrefix("./").removePrefix("/")
        
        val classLoader = Thread.currentThread().contextClassLoader ?: this@ResourceLoader::class.java.classLoader
        
        successfulPrefix?.let { prefix ->
            val probePath = if (prefix.endsWith("/")) "$prefix$cleanPath" else "$prefix/$cleanPath"
            val stream = tryResolve(classLoader, probePath)
            if (stream != null) {
                return@withContext stream.bufferedReader().use { it.readText() }
            }
        }

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
            val stream = tryResolve(classLoader, probePath)
            
            if (stream != null) {
                println("[DEBUG_LOG] Desktop ResourceLoader: Found resource at '$probePath'")
                successfulPrefix = probePath.removeSuffix(cleanPath)
                return@withContext stream.bufferedReader().use { it.readText() }
            }
        }
        
        println("[DEBUG_LOG] Desktop ResourceLoader: FAILED to find '$cleanPath'")
        null
    }

    private fun tryResolve(classLoader: ClassLoader, path: String): java.io.InputStream? {
        return classLoader.getResourceAsStream(path) 
            ?: this::class.java.getResourceAsStream("/$path")
            ?: classLoader.getResourceAsStream("/$path")
    }
}
