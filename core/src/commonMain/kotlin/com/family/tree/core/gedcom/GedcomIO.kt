package com.family.tree.core.gedcom

import com.family.tree.core.ProjectData
import com.family.tree.core.platform.FileGateway

/**
 * GEDCOM file I/O utilities for KMP.
 * Integrates GedcomImporter/Exporter with platform-specific FileGateway.
 */
object GedcomIO {
    
    /**
     * Imports GEDCOM data from a file using platform-specific file picker.
     * Returns ProjectData on success, or null if cancelled or failed.
     */
    fun importFromFile(): ProjectData? {
        val bytes = FileGateway.pickOpen() ?: return null
        val content = bytes.decodeToString()
        return try {
            val importer = GedcomImporter()
            importer.importFromString(content)
        } catch (e: Exception) {
            println("GEDCOM import failed: ${e.message}")
            null
        }
    }
    
    /**
     * Imports GEDCOM data from a string.
     * Returns ProjectData on success, or null if parsing failed.
     */
    fun importFromString(content: String): ProjectData? {
        return try {
            val importer = GedcomImporter()
            importer.importFromString(content)
        } catch (e: Exception) {
            println("GEDCOM import failed: ${e.message}")
            null
        }
    }
    
    /**
     * Exports ProjectData to GEDCOM file using platform-specific file picker.
     * Returns true on success, false if cancelled or failed.
     */
    fun exportToFile(data: ProjectData): Boolean {
        return try {
            val exporter = GedcomExporter()
            val content = exporter.exportToString(data)
            val bytes = content.encodeToByteArray()
            FileGateway.pickSave(bytes)
        } catch (e: Exception) {
            println("GEDCOM export failed: ${e.message}")
            false
        }
    }
    
    /**
     * Exports ProjectData to GEDCOM string.
     * Returns GEDCOM content string on success, or null if failed.
     */
    fun exportToString(data: ProjectData): String? {
        return try {
            val exporter = GedcomExporter()
            exporter.exportToString(data)
        } catch (e: Exception) {
            println("GEDCOM export failed: ${e.message}")
            null
        }
    }
}
