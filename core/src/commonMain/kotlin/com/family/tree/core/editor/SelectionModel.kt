package com.family.tree.core.editor

import com.family.tree.core.model.IndividualId

/**
 * Minimal selection model (single select for now).
 * Can be extended to multi-select and callbacks later.
 */
class SelectionModel {
    var selected: IndividualId? = null
        private set

    fun select(id: IndividualId?) {
        selected = id
    }

    fun isSelected(id: IndividualId?): Boolean = id != null && selected == id

    fun clear() { selected = null }
}