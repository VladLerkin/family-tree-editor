package com.family.tree.model

import com.family.tree.util.DirtyFlag
import java.time.Instant

class ProjectMetadata {
    var version: String = "0.1"
        set(value) { field = value; DirtyFlag.setModified() }
    var createdAt: Instant = Instant.now()
        set(value) { field = value; DirtyFlag.setModified() }
    var modifiedAt: Instant = Instant.now()
        set(value) { field = value; DirtyFlag.setModified() }
}
