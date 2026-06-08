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
    private val _selected = MutableStateFlow<IndividualId?>(null)
    val selected: StateFlow<IndividualId?> = _selected.asStateFlow()

    fun select(id: IndividualId?) {
        _selected.value = id
    }

    fun isSelected(id: IndividualId?): Boolean = id != null && _selected.value == id

    fun clear() { _selected.value = null }
}