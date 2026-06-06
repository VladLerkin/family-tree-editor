package com.family.tree.core.ai

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

class IosModelDirectoryProvider : ModelDirectoryProvider {
    override fun getDirectory(): String {
        val paths = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsDirectory = paths.firstOrNull()?.toString() ?: ""
        // Remove "file://" prefix if it exists
        val cleanPath = if (documentsDirectory.startsWith("file://")) {
            documentsDirectory.substring(7)
        } else {
            documentsDirectory
        }
        return "$cleanPath/models"
    }
}
