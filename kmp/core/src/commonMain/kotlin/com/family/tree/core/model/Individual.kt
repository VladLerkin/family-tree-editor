package com.family.tree.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal cross-platform domain model.
 * Keep it UI-agnostic and serialization-friendly.
 * Compatible with JavaFX .rel format (Jackson JSON).
 */
@Serializable
data class Individual(
    @SerialName("id")
    val id: IndividualId,
    @SerialName("firstName")
    val firstName: String = "",
    @SerialName("lastName")
    val lastName: String = "",
    @SerialName("gender")
    val gender: Gender? = null,
    @SerialName("birthYear")
    val birthYear: Int? = null,
    @SerialName("deathYear")
    val deathYear: Int? = null,
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
) {
    val displayName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}
