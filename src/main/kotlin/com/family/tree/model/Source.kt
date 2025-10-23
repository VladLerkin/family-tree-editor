package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import com.family.tree.util.DirtyObservableList
import java.util.UUID

/**
 * Kotlin version of Source. Uses DirtyObservableList for attributes; setters mark DirtyFlag.
 */
class Source {
    val id: String
    var title: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var abbreviation: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var text: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var publicationFacts: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var agency: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var repositoryId: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var callNumber: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    val attributes: MutableList<GedcomAttribute>

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = id
        this.attributes = DirtyObservableList()
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("title") title: String?,
        @JsonProperty("abbreviation") abbreviation: String?,
        @JsonProperty("text") text: String?,
        @JsonProperty("publicationFacts") publicationFacts: String?,
        @JsonProperty("agency") agency: String?,
        @JsonProperty("repositoryId") repositoryId: String?,
        @JsonProperty("callNumber") callNumber: String?,
        @JsonProperty("attributes") attributes: List<GedcomAttribute>?
    ) {
        this.id = id ?: UUID.randomUUID().toString()
        this.title = title
        this.abbreviation = abbreviation
        this.text = text
        this.publicationFacts = publicationFacts
        this.agency = agency
        this.repositoryId = repositoryId
        this.callNumber = callNumber
        this.attributes = if (attributes != null) DirtyObservableList(attributes) else DirtyObservableList()
    }
}
