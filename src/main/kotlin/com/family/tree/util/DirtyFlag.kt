package com.family.tree.util

/**
 * Global dirty flag indicating that in-memory project state has been modified
 * and needs persistence. Thread visibility is ensured via @Volatile.
 */
object DirtyFlag {
    @Volatile
    private var modified: Boolean = false

    @JvmStatic
    fun isModified(): Boolean = modified

    @JvmStatic
    fun setModified() {
        modified = true
    }

    @JvmStatic
    fun clear() {
        modified = false
    }
}
