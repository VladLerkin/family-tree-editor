package com.family.tree.core

import com.family.tree.core.model.Family
import com.family.tree.core.model.Individual
import com.family.tree.core.model.Source
import com.family.tree.core.model.Tag
import kotlinx.serialization.Serializable

/**
 * Minimal in-memory project state for KMP branch.
 */
@Serializable
data class ProjectData(
    val individuals: List<Individual> = emptyList(),
    val families: List<Family> = emptyList(),
    val sources: List<Source> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val metadata: ProjectMetadata = ProjectMetadata(),
    val viewport: Viewport = Viewport()
)
