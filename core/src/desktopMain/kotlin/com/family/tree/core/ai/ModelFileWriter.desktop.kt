package com.family.tree.core.ai

import java.io.File
import java.io.FileOutputStream

actual class ModelFileWriter actual constructor() {
    actual fun writeChunk(absolutePath: String, bytes: ByteArray, append: Boolean) {
        val file = File(absolutePath)
        file.parentFile?.mkdirs()
        FileOutputStream(file, append).use { it.write(bytes) }
    }
    actual fun exists(absolutePath: String): Boolean {
        return File(absolutePath).exists()
    }
    actual fun delete(absolutePath: String): Boolean {
        return File(absolutePath).delete()
    }
    actual fun length(absolutePath: String): Long {
        return File(absolutePath).length()
    }
    actual fun rename(from: String, to: String): Boolean {
        return File(from).renameTo(File(to))
    }
}
