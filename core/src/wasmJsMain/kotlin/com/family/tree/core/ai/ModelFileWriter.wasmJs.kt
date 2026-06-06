package com.family.tree.core.ai

actual class ModelFileWriter actual constructor() {
    actual fun writeChunk(absolutePath: String, bytes: ByteArray, append: Boolean) {
        // No-op for Wasm
    }
    actual fun exists(absolutePath: String): Boolean {
        return false
    }
}
