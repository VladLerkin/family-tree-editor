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

        // naive generation inference: those who are not children of anyone -> gen 0; their children -> gen 1; etc.
        val childIds = families.flatMap { it.childrenIds }.toSet()
        val roots = individuals.filter { it.id !in childIds }
            .ifEmpty { listOf(individuals.first()) }

        val positions = mutableMapOf<IndividualId, Vec2>()
        val visited = mutableSetOf<IndividualId>()
        var y = 0
        var frontier = roots
        while (frontier.isNotEmpty()) {
            var x = 0
            frontier.forEach { ind ->
                positions[ind.id] = Vec2(x * params.xGap, y * params.yGap)
                visited += ind.id
                x++
            }
            // next generation: collect children of frontier parents
            val nextIds = families.flatMap { fam ->
                val parentInFrontier = listOfNotNull(fam.husbandId, fam.wifeId).any { it in frontier.map { it.id }.toSet() }
                if (parentInFrontier) fam.childrenIds else emptyList()
            }.distinct().filter { it !in visited }
            frontier = nextIds.mapNotNull { id -> individuals.find { it.id == id } }
            y++
        }
        return positions
    }
}
