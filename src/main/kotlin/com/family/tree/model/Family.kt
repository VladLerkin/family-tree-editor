package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import com.family.tree.util.DirtyObservableList
import java.util.Objects
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
class Family {
    val id: String
    var husbandId: String? = null
        @JsonProperty("husbandId")
        set(value) { field = value; DirtyFlag.setModified() }
    var wifeId: String? = null
        @JsonProperty("wifeId")
        set(value) { field = value; DirtyFlag.setModified() }
    val childrenIds: MutableList<String>
    val events: MutableList<GedcomEvent>
    val sources: MutableList<SourceCitation>
    val notes: MutableList<Note>
    val media: MutableList<MediaAttachment>
    val tags: MutableList<Tag>

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = Objects.requireNonNull(id, "id")
        this.childrenIds = DirtyObservableList()
        this.events = DirtyObservableList()
        this.sources = DirtyObservableList()
        this.notes = DirtyObservableList()
        this.media = DirtyObservableList()
        this.tags = DirtyObservableList()
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("husbandId") husbandId: String?,
        @JsonProperty("wifeId") wifeId: String?,
        @JsonProperty("childrenIds") childrenIds: List<String>?,
        @JsonProperty("events") events: List<GedcomEvent>?,
        @JsonProperty("sources") sources: List<SourceCitation>?,
        @JsonProperty("notes") notes: List<Note>?,
        @JsonProperty("media") media: List<MediaAttachment>?,
        @JsonProperty("tags") tags: List<Tag>?
    ) {
        this.id = id ?: throw NullPointerException("id")
        this.husbandId = husbandId
        this.wifeId = wifeId
        this.childrenIds = if (childrenIds != null) DirtyObservableList(childrenIds) else DirtyObservableList()
        this.events = if (events != null) DirtyObservableList(events) else DirtyObservableList()
        this.sources = if (sources != null) DirtyObservableList(sources) else DirtyObservableList()
        this.notes = if (notes != null) DirtyObservableList(notes) else DirtyObservableList()
        this.media = if (media != null) DirtyObservableList(media) else DirtyObservableList()
        this.tags = if (tags != null) DirtyObservableList(tags) else DirtyObservableList()
    }
}
