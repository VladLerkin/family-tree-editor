package com.family.tree.model

import com.family.tree.util.DirtyFlag
import java.util.UUID

class Tag {
    val id: String = UUID.randomUUID().toString()
    var name: String? = null
        set(value) {
            field = value
            DirtyFlag.setModified()
        }
}
