package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import com.family.tree.util.DirtyObservableList
import java.util.Objects
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
class Individual {
    val id: String
    var firstName: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var lastName: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var gender: Gender? = null
        set(value) { field = value; DirtyFlag.setModified() }
    val events: MutableList<GedcomEvent>
    val sources: MutableList<SourceCitation>
    val notes: MutableList<Note>
    val media: MutableList<MediaAttachment>
    val tags: MutableList<Tag>

    constructor(firstName: String, lastName: String, gender: Gender) : this(
        UUID.randomUUID().toString(), firstName, lastName, gender
    )

    constructor(id: String, firstName: String, lastName: String, gender: Gender) {
        this.id = Objects.requireNonNull(id, "id")
        this.firstName = Objects.requireNonNull(firstName, "firstName")
        this.lastName = Objects.requireNonNull(lastName, "lastName")
        this.gender = Objects.requireNonNull(gender, "gender")
        this.events = DirtyObservableList<GedcomEvent>()
        this.sources = DirtyObservableList<SourceCitation>()
        this.notes = DirtyObservableList<Note>()
        this.media = DirtyObservableList<MediaAttachment>()
        this.tags = DirtyObservableList<Tag>()
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("firstName") firstName: String?,
        @JsonProperty("lastName") lastName: String?,
        @JsonProperty("gender") gender: Gender?,
        @JsonProperty("events") events: List<GedcomEvent>?,
        @JsonProperty("sources") sources: List<SourceCitation>?,
        @JsonProperty("notes") notes: List<Note>?,
        @JsonProperty("media") media: List<MediaAttachment>?,
        @JsonProperty("tags") tags: List<Tag>?
    ) {
        this.id = id ?: throw NullPointerException("id")
        this.firstName = Objects.requireNonNull(firstName, "firstName")
        this.lastName = Objects.requireNonNull(lastName, "lastName")
        this.gender = Objects.requireNonNull(gender, "gender")
        this.events = if (events != null) DirtyObservableList<GedcomEvent>(events) else DirtyObservableList()
        this.sources = if (sources != null) DirtyObservableList<SourceCitation>(sources) else DirtyObservableList()
        this.notes = if (notes != null) DirtyObservableList<Note>(notes) else DirtyObservableList()
        this.media = if (media != null) DirtyObservableList<MediaAttachment>(media) else DirtyObservableList()
        this.tags = if (tags != null) DirtyObservableList<Tag>(tags) else DirtyObservableList()
    }
}
