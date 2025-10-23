package com.family.tree.storage

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.family.tree.model.ProjectLayout
import com.family.tree.model.ProjectMetadata
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectRepository {
    class ProjectData {
        @JvmField var individuals: MutableList<com.family.tree.model.Individual> = com.family.tree.util.DirtyObservableList()
        @JvmField var families: MutableList<com.family.tree.model.Family> = com.family.tree.util.DirtyObservableList()
        @JvmField var relationships: MutableList<com.family.tree.model.Relationship> = com.family.tree.util.DirtyObservableList()
        @JvmField var tags: MutableList<com.family.tree.model.Tag> = com.family.tree.util.DirtyObservableList()
        @JvmField var sources: MutableList<com.family.tree.model.Source> = com.family.tree.util.DirtyObservableList()
        @JvmField var repositories: MutableList<com.family.tree.model.Repository> = com.family.tree.util.DirtyObservableList()
        @JvmField var submitters: MutableList<com.family.tree.model.Submitter> = com.family.tree.util.DirtyObservableList()
    }

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerKotlinModule()
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)

    @Throws(IOException::class)
    fun write(path: Path, data: ProjectData?, layout: ProjectLayout?, meta: ProjectMetadata?) {
        // Ensure parent directory exists
        val dir = path.toAbsolutePath().parent
        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir)
        }

        // Update metadata modified timestamp
        if (meta != null) {
            meta.modifiedAt = Instant.now()
        }

        // Write to a temporary file first
        val tmp = Files.createTempFile(dir ?: path.toAbsolutePath().parent, path.fileName.toString() + ".", ".tmp")
        var success = false
        try {
            Files.newOutputStream(tmp).use { os ->
                ZipOutputStream(os).use { zos ->
                    writeEntry(zos, ProjectFormat.DATA_JSON, data)
                    writeEntry(zos, ProjectFormat.LAYOUT_JSON, layout)
                    writeEntry(zos, ProjectFormat.META_JSON, meta)
                    success = true
                }
            }
        } catch (e: IOException) {
            Files.deleteIfExists(tmp)
            throw e
        }

        if (success) {
            // Rotate backups before replacing the project file
            BackupService.rotateBackups(path)
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (ex: AtomicMoveNotSupportedException) {
                // Fallback if filesystem doesn't support atomic moves
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
            }
            // Mark project as clean after successful save
            com.family.tree.util.DirtyFlag.clear()
        }
    }

    @Throws(IOException::class)
    private fun writeEntry(zos: ZipOutputStream, name: String, payload: Any?) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        objectMapper.writeValue(zos, payload)
        zos.closeEntry()
    }

    @Throws(IOException::class)
    fun read(path: Path): LoadedProject {
        var data: ProjectData? = null
        var layout: ProjectLayout? = null
        var meta: ProjectMetadata? = null

        Files.newInputStream(path).use { `is` ->
            ZipInputStream(`is`).use { zis ->
                var entry: ZipEntry?
                while (true) {
                    entry = zis.nextEntry ?: break
                    when (entry!!.name) {
                        ProjectFormat.DATA_JSON -> data = objectMapper.readValue(zis, ProjectData::class.java)
                        ProjectFormat.LAYOUT_JSON -> layout = objectMapper.readValue(zis, ProjectLayout::class.java)
                        ProjectFormat.META_JSON -> meta = objectMapper.readValue(zis, ProjectMetadata::class.java)
                        else -> {}
                    }
                    zis.closeEntry()
                }
            }
        }
        com.family.tree.util.DirtyFlag.clear()
        // Ensure lists are dirty-observable after load
        if (data != null) {
            data!!.individuals = com.family.tree.util.DirtyObservableList(data!!.individuals)
            data!!.families = com.family.tree.util.DirtyObservableList(data!!.families)
            data!!.relationships = com.family.tree.util.DirtyObservableList(data!!.relationships)
            data!!.tags = com.family.tree.util.DirtyObservableList(data!!.tags)
            data!!.sources = com.family.tree.util.DirtyObservableList(data!!.sources)
            data!!.repositories = com.family.tree.util.DirtyObservableList(data!!.repositories)
            data!!.submitters = com.family.tree.util.DirtyObservableList(data!!.submitters)
        }
        com.family.tree.util.DirtyFlag.clear()
        return LoadedProject(data, layout, meta)
    }

    data class LoadedProject(val data: ProjectData?, val layout: ProjectLayout?, val meta: ProjectMetadata?)
}
