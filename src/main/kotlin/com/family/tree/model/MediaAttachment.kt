package com.family.tree.model

import com.family.tree.util.DirtyFlag
import java.util.UUID

class MediaAttachment {
    val id: String = UUID.randomUUID().toString()
    var fileName: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var relativePath: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
}
