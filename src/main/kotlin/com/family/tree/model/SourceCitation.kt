package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import java.util.UUID

/**
 * Kotlin version of SourceCitation.
 */
class SourceCitation {
    val id: String
    var sourceId: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var page: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var text: String? = null
        set(value) { field = value; DirtyFlag.setModified() }

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = id
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("sourceId") sourceId: String?,
        @JsonProperty("page") page: String?,
        @JsonProperty("text") text: String?
    ) {
        this.id = id ?: UUID.randomUUID().toString()
        this.sourceId = sourceId
        this.page = page
        this.text = text
    }
}
