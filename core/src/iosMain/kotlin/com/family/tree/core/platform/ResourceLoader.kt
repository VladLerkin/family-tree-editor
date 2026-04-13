package com.family.tree.core.platform

import kotlinx.serialization.json.*
import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi

actual class ResourceLoader actual constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    actual suspend fun listPromptFiles(repoPath: String): List<String> {
        return listDirectory(repoPath, "prompts")
    }

    actual suspend fun listDirectory(repoPath: String, subDir: String): List<String> {
        println("[DEBUG_LOG] iOS ResourceLoader: listDirectory(repoPath=$repoPath, subDir=$subDir)")
        return try {
            val manifestContent = readResourceText("autoresearch-genealogy/manifest.json")
            if (manifestContent == null) {
                println("[DEBUG_LOG] iOS ResourceLoader: manifest.json not found!")
                return emptyList()
            }
            val manifest = json.parseToJsonElement(manifestContent).jsonObject
            val results = manifest[subDir]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            println("[DEBUG_LOG] iOS ResourceLoader: Found ${results.size} items in manifest[$subDir]")
            results
        } catch (e: Exception) {
            println("[DEBUG_LOG] iOS ResourceLoader: ERROR listing directory: ${e.message}")
            emptyList()
        }
    }

    actual suspend fun readFile(basePath: String, relativePath: String): String? {
        val path = if (basePath.isEmpty()) relativePath else "$basePath/$relativePath"
        val cleanPath = path.removePrefix("files/").removePrefix("/")
        return readResourceText(cleanPath)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readResourceText(path: String): String? {
        // Normalize path: Remove leading "./", "/", and redundant slashes
        var cleanPath = path
        while (cleanPath.startsWith("./")) cleanPath = cleanPath.substring(2)
        while (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1)
        
        val parts = cleanPath.split("/")
        val lastPart = parts.last()
        val name = lastPart.substringBeforeLast(".")
        val extension = if (lastPart.contains(".")) lastPart.substringAfterLast(".") else ""
        val subDir = if (parts.size > 1) parts.dropLast(1).joinToString("/") else null
        
        println("[DEBUG_LOG] iOS ResourceLoader: Probe started for '$name.$extension' (original: $path)")

        val fm = NSFileManager.defaultManager()
        val allBundles = (listOf(NSBundle.mainBundle) + NSBundle.allBundles.mapNotNull { it as? NSBundle }).distinct()
        
        for (bundle in allBundles) {
            val bundlePath = bundle.bundlePath
            println("[DEBUG_LOG] iOS ResourceLoader: Scanning bundle '${bundle.bundleIdentifier ?: "unknown"}' at $bundlePath")

            // 1. Standard probe
            val probes = mutableListOf<String?>()
            probes.add("composeResources/com.family.tree.ui/files/$cleanPath")
            if (subDir != null) {
                probes.add(subDir)
                probes.add("compose-resources/$subDir")
                probes.add("compose-resources/files/$subDir")
            }
            probes.add("compose-resources/files")
            probes.add("compose-resources")
            probes.add(null) 
            
            for (probeDir in probes) {
                val filePath = bundle.pathForResource(name, extension, probeDir)
                if (filePath != null) {
                    println("[DEBUG_LOG] iOS ResourceLoader: SUCCESS (Standard)! Found '$name.$extension' in directory '$probeDir'")
                    return NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
                }
            }

            // 2. Deep search via FileManager fallback
            println("[DEBUG_LOG] iOS ResourceLoader: Standard probes failed in bundle. Starting deep scan for suffix: $cleanPath")
            val enumerator = fm.enumeratorAtPath(bundlePath)
            while (true) {
                val relativePath = enumerator?.nextObject() as? String ?: break
                if (relativePath.endsWith(cleanPath)) {
                    val fullPath = "$bundlePath/$relativePath"
                    println("[DEBUG_LOG] iOS ResourceLoader: SUCCESS (Deep Search)! Found at $fullPath")
                    return NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null)
                }
            }
        }

        println("[DEBUG_LOG] iOS ResourceLoader: CRITICAL FAILED to find '$name.$extension' at path '$path' after deep search in all bundles.")
        return null
    }
}
