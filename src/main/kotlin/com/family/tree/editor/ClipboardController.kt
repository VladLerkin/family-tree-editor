package com.family.tree.editor

import java.util.*

/**
 * Clipboard operations using a simple data adapter.
 * Operates on IDs provided by SelectionModel.
 */
class ClipboardController(
    private val selectionModel: SelectionModel,
    private val dataAdapter: DataAdapter
) {
    init {
        Objects.requireNonNull(selectionModel, "selectionModel")
        Objects.requireNonNull(dataAdapter, "dataAdapter")
    }

    interface DataAdapter {
        /**
         * Create a duplicated entity based on the source ID and return the new entity ID.
         */
        fun duplicate(sourceEntityId: String): String

        /**
         * Remove (cut) an entity by ID.
         */
        fun remove(entityId: String)
    }

    private val clipboardIds: MutableList<String> = ArrayList()

    fun copy(): List<String> {
        clipboardIds.clear()
        clipboardIds.addAll(selectionModel.getSelectedIds())
        return java.util.List.copyOf(clipboardIds)
    }

    fun cut(): List<String> {
        clipboardIds.clear()
        val ids = selectionModel.getSelectedIds()
        clipboardIds.addAll(ids)
        for (id in ids) {
            dataAdapter.remove(id)
        }
        selectionModel.clear()
        return java.util.List.copyOf(clipboardIds)
    }

    fun paste(): List<String> {
        val newIds: MutableList<String> = ArrayList()
        for (id in clipboardIds) {
            newIds.add(dataAdapter.duplicate(id))
        }
        return newIds
    }
}
