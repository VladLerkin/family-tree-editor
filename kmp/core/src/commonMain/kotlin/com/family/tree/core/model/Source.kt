package com.family.tree.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Source(
    @SerialName("id")
    val id: SourceId = SourceId.generate(),
    @SerialName("title")
    val title: String = "",
    @SerialName("abbreviation")
    val abbreviation: String = "",
    @SerialName("text")
    val text: String = "",
    @SerialName("publicationFacts")
    val publicationFacts: String = "",
    @SerialName("agency")
    val agency: String = "",
    @SerialName("repositoryId")
    val repositoryId: String = "",
    @SerialName("callNumber")
    val callNumber: String = "",
    @SerialName("attributes")
    val attributes: List<GedcomAttribute> = emptyList()
)
