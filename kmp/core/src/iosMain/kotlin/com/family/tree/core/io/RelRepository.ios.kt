package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.layout.ProjectLayout

/**
 * iOS implementation of RelRepository.
 * TODO: Implement ZIP handling using Kotlin/Native compatible libraries.
 */
actual class RelRepository {
    actual fun read(bytes: ByteArray): LoadedProject {
        // Stub implementation for iOS
        println("[DEBUG_LOG] RelRepository.read: iOS implementation not yet available")
        return LoadedProject(
            data = ProjectData(),
            layout = null,
            meta = null
        )
    }

    actual fun write(data: ProjectData, layout: ProjectLayout?, meta: ProjectMetadata?): ByteArray {
        // Stub implementation for iOS
        println("[DEBUG_LOG] RelRepository.write: iOS implementation not yet available")
        return ByteArray(0)
    }
}
