package com.family.tree.render

/**
 * Provides default node metrics; can be replaced with text-aware sizing.
 */
open class NodeMetrics(
    private val defaultWidth: Double = 120.0,
    private val defaultHeight: Double = 60.0
) {
    open fun getWidth(nodeId: String): Double = defaultWidth
    open fun getHeight(nodeId: String): Double = defaultHeight
}
