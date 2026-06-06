package com.family.tree.core.ai

import java.io.File

class DesktopModelDirectoryProvider : ModelDirectoryProvider {
    override fun getDirectory(): String {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".family-tree-editor/models")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir.absolutePath
    }
}
