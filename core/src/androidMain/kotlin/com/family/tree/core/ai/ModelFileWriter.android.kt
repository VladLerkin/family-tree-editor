package com.family.tree.core.ai

import java.io.File
import java.io.FileOutputStream

actual class ModelFileWriter actual constructor() {
    actual fun writeChunk(absolutePath: String, bytes: ByteArray, append: Boolean) {
        FileOutputStream(File(absolutePath), append).use { it.write(bytes) }
    }
    actual fun exists(absolutePath: String): Boolean {
        return File(absolutePath).exists()
    }
}
