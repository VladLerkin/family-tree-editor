package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import com.family.tree.util.DirtyObservableList
import java.util.UUID

/**
 * Represents any GEDCOM event (BIRT, DEAT, BURI, MARR, ADOP, RESI, etc.)
 * with support for date, place, sources, notes, and custom attributes.
 */
class GedcomEvent {
    val id: String
    var type: String? = null // BIRT, DEAT, BURI, MARR, ADOP, RESI, etc.
        set(value) { field = value; DirtyFlag.setModified() }
    var date: String? = null // Free-form date string as in GEDCOM
        set(value) { field = value; DirtyFlag.setModified() }
    var place: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    val sources: MutableList<SourceCitation>
    val notes: MutableList<Note>
    val attributes: MutableList<GedcomAttribute>

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = id
        this.sources = DirtyObservableList()
        this.notes = DirtyObservableList()
        this.attributes = DirtyObservableList()
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("type") type: String?,
        @JsonProperty("date") date: String?,
        @JsonProperty("place") place: String?,
        @JsonProperty("sources") sources: List<SourceCitation>?,
        @JsonProperty("notes") notes: List<Note>?,
        @JsonProperty("attributes") attributes: List<GedcomAttribute>?
    ) {
        this.id = id ?: UUID.randomUUID().toString()
        this.type = type
        this.date = date
        this.place = place
        this.sources = if (sources != null) DirtyObservableList(sources) else DirtyObservableList()
        this.notes = if (notes != null) DirtyObservableList(notes) else DirtyObservableList()
        this.attributes = if (attributes != null) DirtyObservableList(attributes) else DirtyObservableList()
    }
}
