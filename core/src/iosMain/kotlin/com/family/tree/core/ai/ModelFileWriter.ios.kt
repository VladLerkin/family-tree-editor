package com.family.tree.core.ai

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileHandle
import platform.Foundation.dataWithBytes
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.seekToEndOfFile
import platform.Foundation.truncateFileAtOffset
import platform.Foundation.writeData
import platform.Foundation.closeFile

actual class ModelFileWriter actual constructor() {
    @OptIn(ExperimentalForeignApi::class)
    actual fun writeChunk(absolutePath: String, bytes: ByteArray, append: Boolean) {
        val data = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(absolutePath)) {
            fileManager.createFileAtPath(absolutePath, data, null)
        } else {
            val fileHandle = NSFileHandle.fileHandleForWritingAtPath(absolutePath)
            if (fileHandle != null) {
                if (append) {
                    fileHandle.seekToEndOfFile()
                } else {
                    fileHandle.truncateFileAtOffset(0uL)
                }
                fileHandle.writeData(data)
                fileHandle.closeFile()
            }
        }
    }
    actual fun exists(absolutePath: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(absolutePath)
    }
}
