package com.family.tree.core

import kotlinx.serialization.Serializable

/**
 * Minimal project metadata persisted with KMP JSON and prepared for
 * future .rel alignment. Timestamps are epoch millis to avoid extra deps.
 */
@Serializable
data class ProjectMetadata(
    val name: String = "",
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L,
    val formatVersion: Int = 1
)