package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.layout.ProjectLayout

/**
 * Repository for reading/writing .rel files (ZIP with JSON entries).
 * Platform-specific implementations handle ZIP and file I/O.
 */
expect class RelRepository() {
    /**
     * Read .rel file from bytes and decode into ProjectData, layout, and metadata.
     */
    fun read(bytes: ByteArray): LoadedProject
    
    /**
     * Write ProjectData, layout, and metadata into .rel format and return bytes.
     */
    fun write(data: ProjectData, layout: ProjectLayout?, meta: ProjectMetadata?): ByteArray
}

/**
 * Result of loading a .rel file.
 */
data class LoadedProject(
    val data: ProjectData,
    val layout: ProjectLayout?,
    val meta: ProjectMetadata?
)
