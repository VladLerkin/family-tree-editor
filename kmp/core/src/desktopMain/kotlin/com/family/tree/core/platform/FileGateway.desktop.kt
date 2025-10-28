package com.family.tree.core.platform

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files

actual object FileGateway {
    actual fun pickOpen(): ByteArray? {
        val fd = FileDialog(null as Frame?, "Open Project JSON", FileDialog.LOAD)
        fd.isVisible = true
        val dir = fd.directory ?: return null
        val file = fd.file ?: return null
        val selected = File(dir, file)
        return try {
            Files.readAllBytes(selected.toPath())
        } catch (_: Exception) {
            null
        }
    }

    actual fun pickSave(data: ByteArray): Boolean {
        val fd = FileDialog(null as Frame?, "Save Project JSON", FileDialog.SAVE)
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val selected = File(dir, file)
        return try {
            Files.write(selected.toPath(), data)
            true
        } catch (_: Exception) {
            false
        }
    }
}
