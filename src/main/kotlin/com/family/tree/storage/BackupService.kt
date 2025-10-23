package com.family.tree.storage

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Kotlin version of BackupService. Static-style API preserved via @JvmStatic.
 */
object BackupService {
    @JvmStatic
    @Throws(IOException::class)
    fun rotateBackups(projectFilePath: Path) {
        rotateBackups(projectFilePath, 5)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun rotateBackups(projectFilePath: Path, maxBackups: Int) {
        if (maxBackups <= 0) return

        // Delete the oldest backup if present
        val oldest = backupPath(projectFilePath, maxBackups)
        if (Files.exists(oldest)) {
            Files.delete(oldest)
        }

        // Shift existing backups: .bakN-1 -> .bakN down to .bak1
        for (i in maxBackups - 1 downTo 1) {
            val src = backupPath(projectFilePath, i)
            if (Files.exists(src)) {
                val dst = backupPath(projectFilePath, i + 1)
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        // Create new .bak1 from the current project file
        if (Files.exists(projectFilePath)) {
            val firstBackup = backupPath(projectFilePath, 1)
            Files.copy(projectFilePath, firstBackup, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun backupPath(original: Path, index: Int): Path {
        val fileName = original.fileName.toString()
        val backupName = "$fileName.bak$index"
        return original.resolveSibling(backupName)
    }
}
