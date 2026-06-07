package com.family.tree.core.ai

import android.content.Context

class AndroidModelDirectoryProvider(private val context: Context) : ModelDirectoryProvider {
    override fun getDirectory(): String {
        return context.filesDir.absolutePath + "/models"
    }
    override val isAndroid: Boolean = true
}
