package com.family.tree.core.editor

import com.family.tree.core.model.IndividualId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Minimal selection model (single select for now).
 * Can be extended to multi-select and callbacks later.
 */
class SelectionModel {
    val selected: StateFlow<IndividualId?>
        field = MutableStateFlow<IndividualId?>(null)

    fun select(id: IndividualId?) {
        selected.value = id
    }

    fun isSelected(id: IndividualId?): Boolean = id != null && selected.value == id

    fun clear() { selected.value = null }
}