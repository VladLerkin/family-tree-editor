package com.family.tree.model

import com.family.tree.util.DirtyFlag
import java.time.LocalDate

class Event {
    var type: String? = null // e.g., BIRTH, DEATH, MARRIAGE
        set(value) { field = value; DirtyFlag.setModified() }
    var date: LocalDate? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var place: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
}
