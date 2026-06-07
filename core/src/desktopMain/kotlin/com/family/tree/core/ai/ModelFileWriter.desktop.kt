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
}
