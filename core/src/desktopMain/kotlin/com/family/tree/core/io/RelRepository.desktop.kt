package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.layout.ProjectLayout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Desktop (JVM) implementation of RelRepository using java.util.zip.
 */
actual class RelRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual fun read(bytes: ByteArray): LoadedProject {
        println("[DEBUG_LOG] RelRepository.read: Starting to read ${bytes.size} bytes")
        var data: ProjectData? = null
        var layout: ProjectLayout? = null
        var meta: ProjectMetadata? = null

        try {
            ByteArrayInputStream(bytes).use { bais ->
                ZipInputStream(bais).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    println("[DEBUG_LOG] RelRepository.read: Starting ZIP entry iteration")
                    while (entry != null) {
                        val entryName = entry.name
                        val content = zis.readBytes()
                        println("[DEBUG_LOG] RelRepository.read: Found entry '$entryName', size=${content.size} bytes")
                        try {
                            when (entryName) {
                                RelFormat.DATA_JSON -> {
                                    println("[DEBUG_LOG] RelRepository.read: Parsing data.json...")
                                    data = json.decodeFromString(content.decodeToString())
                                    println("[DEBUG_LOG] RelRepository.read: data.json parsed - individuals=${data?.individuals?.size}, families=${data?.families?.size}")
                                }
                                RelFormat.LAYOUT_JSON -> {
                                    println("[DEBUG_LOG] RelRepository.read: Parsing layout.json...")
                                    layout = json.decodeFromString(content.decodeToString())
                                    println("[DEBUG_LOG] RelRepository.read: layout.json parsed")
                                }
                                RelFormat.META_JSON -> {
                                    println("[DEBUG_LOG] RelRepository.read: Parsing meta.json...")
                                    meta = json.decodeFromString(content.decodeToString())
                                    println("[DEBUG_LOG] RelRepository.read: meta.json parsed")
                                }
                                else -> println("[DEBUG_LOG] RelRepository.read: Skipping unknown entry '$entryName'")
                            }
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] RelRepository.read: ERROR parsing entry '$entryName':")
                            e.printStackTrace()
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    println("[DEBUG_LOG] RelRepository.read: Finished ZIP entry iteration")
                }
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.read: ERROR reading ZIP:")
            e.printStackTrace()
        }

        val result = LoadedProject(
            data = data ?: ProjectData(),
            layout = layout,
            meta = meta
        )
        println("[DEBUG_LOG] RelRepository.read: Returning LoadedProject with data=${result.data.individuals.size} individuals, ${result.data.families.size} families")
        return result
    }

    actual fun write(data: ProjectData, layout: ProjectLayout?, meta: ProjectMetadata?): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Write data.json
            zos.putNextEntry(ZipEntry(RelFormat.DATA_JSON))
            zos.write(json.encodeToString(data).encodeToByteArray())
            zos.closeEntry()

            // Write layout.json
            if (layout != null) {
                zos.putNextEntry(ZipEntry(RelFormat.LAYOUT_JSON))
                zos.write(json.encodeToString(layout).encodeToByteArray())
                zos.closeEntry()
            }

            // Write meta.json
            if (meta != null) {
                zos.putNextEntry(ZipEntry(RelFormat.META_JSON))
                zos.write(json.encodeToString(meta).encodeToByteArray())
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
