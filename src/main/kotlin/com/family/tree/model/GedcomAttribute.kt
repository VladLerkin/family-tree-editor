package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import java.util.UUID

/**
 * Represents a generic GEDCOM attribute (tag-value pair with optional sub-attributes).
 * Used for custom tags or attributes like PEDI, AGE, CAUS, etc.
 */
class GedcomAttribute {
    val id: String
    var tag: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var value: String? = null
        set(value) { field = value; DirtyFlag.setModified() }

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = id
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("tag") tag: String?,
        @JsonProperty("value") value: String?
    ) {
        this.id = id ?: UUID.randomUUID().toString()
        this.tag = tag
        this.value = value
    }
}
