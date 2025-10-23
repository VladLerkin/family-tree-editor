package com.family.tree.model

import com.family.tree.util.DirtyFlag

class Relationship {
    enum class Type { SPOUSE_TO_FAMILY, FAMILY_TO_CHILD }

    var type: Type? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var fromId: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var toId: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
}
