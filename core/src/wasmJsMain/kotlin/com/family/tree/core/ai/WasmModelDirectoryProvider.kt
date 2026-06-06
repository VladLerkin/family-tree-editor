package com.family.tree.core.ai

class WasmModelDirectoryProvider : ModelDirectoryProvider {
    override fun getDirectory(): String {
        // In-memory or indexedDB, standard file paths don't apply, return a pseudo-path
        return "/models"
    }
}
