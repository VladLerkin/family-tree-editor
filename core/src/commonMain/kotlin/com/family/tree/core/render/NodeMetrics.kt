package com.family.tree.core.render

/**
 * Basic node metrics used by renderers. Values are in logical units (before scale),
 * callers may scale them according to current zoom.
 */
data class NodeMetrics(
    val width: Float = 120f,
    val height: Float = 60f,
    val cornerRadius: Float = 6f,
)
