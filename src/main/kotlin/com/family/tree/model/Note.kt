package com.family.tree.model

import com.family.tree.util.DirtyFlag
import com.family.tree.util.DirtyObservableList
import java.util.UUID

class Note {
    val id: String = UUID.randomUUID().toString()
    var text: String? = null
        set(value) {
            field = value
            DirtyFlag.setModified()
        }
    val sources: MutableList<SourceCitation> = DirtyObservableList()
}
