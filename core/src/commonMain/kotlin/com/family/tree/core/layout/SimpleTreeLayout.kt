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
        val individualsWithParents = mutableSetOf<IndividualId>()
        val familiesByParent = mutableMapOf<IndividualId, MutableList<Family>>()
        val individualIds = individuals.map { it.id }.toSet()
        
        families.forEach { fam ->
            val hasParentInTree = (fam.husbandId in individualIds) || (fam.wifeId in individualIds)
            if (hasParentInTree) {
                individualsWithParents.addAll(fam.childrenIds)
            }
            fam.husbandId?.let { familiesByParent.getOrPut(it) { mutableListOf() }.add(fam) }
            fam.wifeId?.let { familiesByParent.getOrPut(it) { mutableListOf() }.add(fam) }
        }

        val positions = mutableMapOf<IndividualId, Vec2>()
        val visited = mutableSetOf<IndividualId>()
        val currentXByY = mutableMapOf<Int, Int>()

        // 2. BFS to layout nodes, ensuring ALL individuals are visited
        while (visited.size < individuals.size) {
            val unvisited = individuals.filter { it.id !in visited }
            
            // Roots are unvisited individuals who don't have parents present in the tree
            var roots = unvisited.filter { it.id !in individualsWithParents }.map { it.id }
            
            if (roots.isEmpty()) {
                // Break cycle or handle orphaned components by picking the first unvisited
                roots = listOf(unvisited.first().id)
            }

            var y = 0
            var frontier = roots

            while (frontier.isNotEmpty()) {
                val nextIds = mutableListOf<IndividualId>()
                
                for (id in frontier) {
                    if (id !in visited) {
                        val x = currentXByY.getOrPut(y) { 0 }
                        positions[id] = Vec2(x * params.xGap, y * params.yGap)
                        currentXByY[y] = x + 1
                        visited.add(id)
                        
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
        }
        
        return positions
    }
}
