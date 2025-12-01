package com.family.tree.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Family model compatible with JavaFX .rel format (Jackson JSON).
 */
@Serializable
data class Family(
    @SerialName("id")
    val id: FamilyId,
    @SerialName("husbandId")
    val husbandId: IndividualId? = null,
    @SerialName("wifeId")
    val wifeId: IndividualId? = null,
    @SerialName("childrenIds")
    val childrenIds: List<IndividualId> = emptyList(),
    @SerialName("tags")
    val tags: List<Tag> = emptyList(),
    @SerialName("notes")
    val notes: List<Note> = emptyList(),
    @SerialName("media")
    val media: List<MediaAttachment> = emptyList(),
    @SerialName("events")
    val events: List<GedcomEvent> = emptyList(),
    @SerialName("sources")
    val sources: List<SourceCitation> = emptyList()
)