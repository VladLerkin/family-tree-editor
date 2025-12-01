package com.family.tree.core.layout

import kotlinx.serialization.Serializable

/**
 * Layout information for a project: zoom, view origin, and individual node positions.
 */
@Serializable
data class ProjectLayout(
    val zoom: Double = 1.0,
    val viewOriginX: Double = 0.0,
    val viewOriginY: Double = 0.0,
    val nodePositions: Map<String, NodePos> = emptyMap(),
    val positionsAreCenters: Boolean = false
)

@Serializable
data class NodePos(
    val x: Double = 0.0,
    val y: Double = 0.0
)
