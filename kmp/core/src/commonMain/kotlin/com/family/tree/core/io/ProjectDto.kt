package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.Viewport
import com.family.tree.core.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal JSON DTOs to persist/load current KMP state while keeping structure close
 * to domain for future migration to .rel.
 */
@Serializable
data class ProjectDto(
    val individuals: List<IndividualDto> = emptyList(),
    val families: List<FamilyDto> = emptyList(),
    val metadata: ProjectMetadataDto = ProjectMetadataDto(),
    val viewport: ViewportDto = ViewportDto()
) {
    fun toDomain(): ProjectData = ProjectData(
        individuals = individuals.map { it.toDomain() },
        families = families.map { it.toDomain() },
        metadata = metadata.toDomain(),
        viewport = viewport.toDomain()
    )

    companion object {
        fun fromDomain(data: ProjectData): ProjectDto = ProjectDto(
            individuals = data.individuals.map { IndividualDto.fromDomain(it) },
            families = data.families.map { FamilyDto.fromDomain(it) },
            metadata = ProjectMetadataDto.fromDomain(data.metadata),
            viewport = ViewportDto.fromDomain(data.viewport)
        )
    }
}

@Serializable
data class ProjectMetadataDto(
    val name: String = "",
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L,
    val formatVersion: Int = 1
) {
    fun toDomain(): ProjectMetadata = ProjectMetadata(
        name = name,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        formatVersion = formatVersion
    )

    companion object {
        fun fromDomain(m: ProjectMetadata) = ProjectMetadataDto(
            name = m.name,
            createdAt = m.createdAt,
            modifiedAt = m.modifiedAt,
            formatVersion = m.formatVersion
        )
    }
}

@Serializable
data class ViewportDto(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    fun toDomain(): Viewport = Viewport(scale = scale, panX = panX, panY = panY)

    companion object {
        fun fromDomain(v: Viewport) = ViewportDto(scale = v.scale, panX = v.panX, panY = v.panY)
    }
}

@Serializable
data class IndividualDto(
    val id: String,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    val birthYear: Int? = null,
    val deathYear: Int? = null
) {
    fun toDomain(): Individual = Individual(
        id = IndividualId(id),
        firstName = firstName,
        lastName = lastName,
        birthYear = birthYear,
        deathYear = deathYear
    )

    companion object {
        fun fromDomain(ind: Individual) = IndividualDto(
            id = ind.id.value,
            firstName = ind.firstName,
            lastName = ind.lastName,
            birthYear = ind.birthYear,
            deathYear = ind.deathYear
        )
    }
}

@Serializable
data class FamilyDto(
    val id: String,
    val husbandId: String? = null,
    val wifeId: String? = null,
    val childrenIds: List<String> = emptyList()
) {
    fun toDomain(): Family = Family(
        id = FamilyId(id),
        husbandId = husbandId?.let { IndividualId(it) },
        wifeId = wifeId?.let { IndividualId(it) },
        childrenIds = childrenIds.map { IndividualId(it) }
    )

    companion object {
        fun fromDomain(fam: Family) = FamilyDto(
            id = fam.id.value,
            husbandId = fam.husbandId?.value,
            wifeId = fam.wifeId?.value,
            childrenIds = fam.childrenIds.map { it.value }
        )
    }
}
