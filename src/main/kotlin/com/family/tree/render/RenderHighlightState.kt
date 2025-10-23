package com.family.tree.render

import java.util.*
import java.util.Collections.unmodifiableSet

/**
 * Holds the current set of selected ids for rendering highlight.
 * This is a simple global bridge between selection sources and renderer.
 */
object RenderHighlightState {
    @Volatile
    private var selectedIds: Set<String> = emptySet()

    @JvmStatic
    fun setSelectedIds(ids: Set<String>?) {
        selectedIds = if (ids == null || ids.isEmpty()) {
            emptySet()
        } else {
            unmodifiableSet(HashSet(ids))
        }
    }

    @JvmStatic
    fun getSelectedIds(): Set<String> = selectedIds
}
