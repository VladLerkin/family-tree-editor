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
 * Android implementation of RelRepository (same as Desktop, uses JVM ZIP).
 */
actual class RelRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual fun read(bytes: ByteArray): LoadedProject {
        var data: ProjectData? = null
        var layout: ProjectLayout? = null
        var meta: ProjectMetadata? = null

        ByteArrayInputStream(bytes).use { bais ->
            ZipInputStream(bais).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val content = zis.readBytes()
                    when (entry.name) {
                        RelFormat.DATA_JSON -> data = json.decodeFromString(content.decodeToString())
                        RelFormat.LAYOUT_JSON -> layout = json.decodeFromString(content.decodeToString())
                        RelFormat.META_JSON -> meta = json.decodeFromString(content.decodeToString())
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        return LoadedProject(
            data = data ?: ProjectData(),
            layout = layout,
            meta = meta
        )
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
