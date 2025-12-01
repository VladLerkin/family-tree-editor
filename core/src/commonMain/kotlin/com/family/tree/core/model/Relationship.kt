package com.family.tree.core.model

/**
 * Parent-child or spouse relationship simplified for initial rendering.
 */
data class Relationship(
    val id: RelationshipId,
    val from: IndividualId,
    val to: IndividualId,
    val type: Type
) {
    enum class Type { PARENT_CHILD, SPOUSES }
}
