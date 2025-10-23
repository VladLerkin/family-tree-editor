package com.family.tree.model

import com.family.tree.util.DirtyFlag
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

class ProjectLayout {
    class NodePos {
        var x: Double = 0.0
        var y: Double = 0.0
    }

    var zoom: Double = 1.0
        set(value) { field = value; DirtyFlag.setModified() }

    var viewOriginX: Double = 0.0
        set(value) { field = value; DirtyFlag.setModified() }

    var viewOriginY: Double = 0.0
        set(value) { field = value; DirtyFlag.setModified() }

    val nodePositions: MutableMap<String, NodePos> = HashMap()

    /** If true, nodePositions are centers; UI should convert to top-left once and then reset this flag */
    @JsonProperty("positionsAreCenters")
    @JsonAlias("isPositionsAreCenters")
    var positionsAreCenters: Boolean = false
        set(value) { field = value; DirtyFlag.setModified() }

    // Java interop compatibility for boolean bean getter
    fun isPositionsAreCenters(): Boolean = positionsAreCenters
}
