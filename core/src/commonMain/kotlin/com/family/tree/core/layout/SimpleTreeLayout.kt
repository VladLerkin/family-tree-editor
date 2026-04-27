package com.family.tree.core.layout

import com.family.tree.core.geometry.Vec2
import com.family.tree.core.model.Family
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId

/**
 * Very basic top-down layout: place generations in rows, individuals in columns.
 * Returns map of IndividualId to position (in arbitrary units, e.g., pixels).
 */
object SimpleTreeLayout {
    data class Params(
        val xGap: Float = 180f,
        val yGap: Float = 140f,
        val nodeSize: Vec2 = Vec2(120f, 60f)
    )

    fun layout(individuals: List<Individual>, families: List<Family>, params: Params = Params()): Map<IndividualId, Vec2> {
        if (individuals.isEmpty()) return emptyMap()

        // 1. Create fast lookups
        val childIds = mutableSetOf<IndividualId>()
        val familiesByParent = mutableMapOf<IndividualId, MutableList<Family>>()
        
        families.forEach { fam ->
            childIds.addAll(fam.childrenIds)
            fam.husbandId?.let { familiesByParent.getOrPut(it) { mutableListOf() }.add(fam) }
            fam.wifeId?.let { familiesByParent.getOrPut(it) { mutableListOf() }.add(fam) }
        }

        // 2. Find roots (individuals who are not children in any family)
        var roots = individuals.map { it.id }.filter { it !in childIds }
        if (roots.isEmpty()) {
            roots = listOf(individuals.first().id)
        }

        // 3. BFS to layout nodes
        val positions = mutableMapOf<IndividualId, Vec2>()
        val visited = mutableSetOf<IndividualId>()
        var y = 0
        var frontier = roots

        while (frontier.isNotEmpty()) {
            var x = 0
            val nextIds = mutableListOf<IndividualId>()
            
            for (id in frontier) {
                if (id !in visited) {
                    positions[id] = Vec2(x * params.xGap, y * params.yGap)
                    visited.add(id)
                    x++
                    
                    // Add children to next generation
                    val parentFamilies = familiesByParent[id] ?: emptyList()
                    for (fam in parentFamilies) {
                        for (childId in fam.childrenIds) {
                            if (childId !in visited) {
                                nextIds.add(childId)
                            }
                        }
                    }
                }
            }
            
            frontier = nextIds.distinct()
            y++
        }
        
        return positions
    }
}
