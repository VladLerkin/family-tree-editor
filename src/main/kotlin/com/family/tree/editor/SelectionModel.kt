package com.family.tree.editor

import java.lang.reflect.Method
import java.util.*

/**
 * Tracks selected entity IDs. Accepts either a String ID or an object exposing getId().
 */
class SelectionModel {
    interface BoundsProvider {
        /**
         * Returns a bounding rectangle (in layout coordinates) for a given node id.
         */
        fun getBounds(id: String): Rect?

        /**
         * Returns all selectable node ids.
         */
        fun getAllIds(): Set<String>
    }

    private val selectedIds: LinkedHashSet<String> = LinkedHashSet()
    private var boundsProvider: BoundsProvider? = null

    fun setBoundsProvider(provider: BoundsProvider?) {
        this.boundsProvider = provider
    }

    fun select(obj: Any) {
        Objects.requireNonNull(obj, "obj")
        val id = resolveId(obj)
        selectedIds.clear()
        selectedIds.add(id)
    }

    fun addToSelection(obj: Any) {
        Objects.requireNonNull(obj, "obj")
        selectedIds.add(resolveId(obj))
    }

    fun selectRange(rect: Rect?) {
        if (rect == null) return
        val provider = boundsProvider ?: return
        selectedIds.clear()
        for (id in provider.getAllIds()) {
            val b = provider.getBounds(id)
            if (b != null && rect.intersects(b)) {
                selectedIds.add(id)
            }
        }
    }

    fun clear() {
        selectedIds.clear()
    }

    fun isSelected(id: String): Boolean = selectedIds.contains(id)

    fun getSelectedIds(): Set<String> = Collections.unmodifiableSet(selectedIds)

    private fun resolveId(obj: Any): String {
        if (obj is String) return obj
        try {
            val m: Method = obj.javaClass.getMethod("getId")
            val valObj = m.invoke(obj)
            if (valObj is String) return valObj
        } catch (_: Exception) {
        }
        throw IllegalArgumentException("Selection requires String id or object exposing getId()")
    }
}
