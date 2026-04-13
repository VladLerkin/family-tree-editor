package com.family.tree.core.platform

import kotlinx.serialization.json.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.js.*

actual class ResourceLoader actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(Js)

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

    private suspend fun readResourceText(path: String): String? {
        return try {
            val response = client.get(path)
            if (response.status.value in 200..299) {
                response.bodyAsText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
