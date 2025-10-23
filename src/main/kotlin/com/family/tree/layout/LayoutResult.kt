package com.family.tree.layout

import java.awt.geom.Point2D

/**
 * Stores layout positions for nodes by ID.
 */
public class LayoutResult {
    private val positions: MutableMap<String, Point2D> = LinkedHashMap()

    @JvmOverloads
    public fun setPosition(nodeId: String, x: Double, y: Double) {
        positions[nodeId] = Point2D.Double(x, y)
    }

    public fun getPosition(nodeId: String): Point2D? = positions[nodeId]

    public fun nodeIds(): Set<String> = java.util.Collections.unmodifiableSet(positions.keys)

    // Java interop compatibility
    @JvmName("getNodeIds")
    public fun getNodeIds(): Set<String> = nodeIds()
}
