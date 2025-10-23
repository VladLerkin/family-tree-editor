package com.family.tree.layout

import com.family.tree.model.Family
import com.family.tree.model.Individual
import com.family.tree.render.NodeMetrics
import com.family.tree.storage.ProjectRepository

/**
 * Computes a basic layered layout for a family.tree graph.
 * - Individuals with no parents are placed on the top layer.
 * - Spouses are aligned adjacently on the same layer; a family node is placed between them.
 * - Children are placed on the next layer centered under their family.
 * This is a simplified version of the layered DAG algorithm and can be refined further.
 */
class TreeLayoutEngine(
    private val metrics: NodeMetrics = NodeMetrics(),
    private val hGap: Double = 40.0,
    private val vGap: Double = 80.0
) {

    fun computeLayout(data: ProjectRepository.ProjectData?): LayoutResult {
        val result = LayoutResult()
        if (data == null) return result

        val individuals: MutableMap<String, Individual> = linkedMapOf()
        val families: MutableMap<String, Family> = linkedMapOf()
        for (i in data.individuals) individuals[i.id] = i
        for (f in data.families) families[f.id] = f

        // Build child -> families mapping and find individuals with parents
        val individualsWithParents: MutableSet<String> = HashSet()
        val familyChildren: MutableMap<String, MutableList<String>> = HashMap()
        val personFamilies: MutableMap<String, MutableList<String>> = HashMap() // person -> families where spouse
        for (f in families.values) {
            familyChildren[f.id] = ArrayList(f.childrenIds)
            f.husbandId?.let { personFamilies.computeIfAbsent(it) { ArrayList() }.add(f.id) }
            f.wifeId?.let { personFamilies.computeIfAbsent(it) { ArrayList() }.add(f.id) }
            for (cid in f.childrenIds) individualsWithParents.add(cid)
        }

        // Roots: individuals without parents
        val roots = ArrayList<String>()
        for (id in individuals.keys) {
            if (!individualsWithParents.contains(id)) roots.add(id)
        }
        if (roots.isEmpty()) roots.addAll(individuals.keys) // fallback

        // Assign layers: BFS from roots; spouses stay on same layer
        val layerByNode: MutableMap<String, Int> = HashMap() // includes individuals and families
        val queue: ArrayDeque<String> = ArrayDeque(roots)
        for (r in roots) layerByNode[r] = 0

        while (queue.isNotEmpty()) {
            val pid = queue.removeFirst()
            val layer = layerByNode.getOrDefault(pid, 0)

            // Families where this person is a spouse
            for (famId in personFamilies.getOrDefault(pid, emptyList())) {
                val fam = families[famId]
                if (!layerByNode.containsKey(famId)) {
                    layerByNode[famId] = layer // place family on same layer as spouses
                }
                // Ensure spouse stays on same layer
                if (fam != null) {
                    if (fam.husbandId != null && !layerByNode.containsKey(fam.husbandId)) {
                        layerByNode[fam.husbandId!!] = layer
                        queue.addLast(fam.husbandId!!)
                    }
                    if (fam.wifeId != null && !layerByNode.containsKey(fam.wifeId)) {
                        layerByNode[fam.wifeId!!] = layer
                        queue.addLast(fam.wifeId!!)
                    }
                    // Place children at next layer
                    for (cid in familyChildren.getOrDefault(famId, emptyList())) {
                        if (!layerByNode.containsKey(cid)) {
                            layerByNode[cid] = layer + 1
                            queue.addLast(cid)
                        }
                    }
                }
            }
        }

        // Group nodes by layer
        val layers: MutableMap<Int, MutableList<String>> = java.util.TreeMap()
        for ((id, lay) in layerByNode) {
            layers.computeIfAbsent(lay) { ArrayList() }.add(id)
        }

        // Place nodes horizontally within each layer
        for ((layer, nodeIds) in layers) {
            // Separate individuals and families in this layer for ordering
            val fams = ArrayList<String>()
            val placed = HashSet<String>()
            for (id in nodeIds) {
                if (families.containsKey(id)) fams.add(id)
            }

            val y = layer * (metrics.getHeight("any") + vGap) // uniform height assumption
            var cursorX = 0.0

            // Place families with spouses together
            for (famId in fams) {
                val f = families[famId] ?: continue
                val a = f.husbandId
                val b = f.wifeId

                // Husband
                if (a != null && individuals.containsKey(a)) {
                    val wA = metrics.getWidth(a)
                    result.setPosition(a, cursorX, y)
                    placed.add(a)
                    cursorX += wA + hGap
                }
                // Family node centered between spouses if both present, else just placed at current cursor
                val famW = metrics.getWidth(famId)
                val famX: Double = if (a != null && b != null) {
                    val wA = metrics.getWidth(a)
                    val wB = metrics.getWidth(b)
                    val aX = result.getPosition(a)?.x ?: cursorX - (if (a != null) metrics.getWidth(a) + hGap else 0.0)
                    val bX = aX + wA + hGap // where wife will be placed
                    (aX + wA + bX) / 2.0 - famW / 2.0
                } else {
                    cursorX
                }
                result.setPosition(famId, famX, y)
                placed.add(famId)

                // Wife
                if (b != null && individuals.containsKey(b)) {
                    val wB = metrics.getWidth(b)
                    val bX = kotlin.math.max(cursorX, (result.getPosition(famId)?.x ?: cursorX) + famW / 2.0 + hGap / 2.0)
                    result.setPosition(b, bX, y)
                    placed.add(b)
                    cursorX = bX + wB + hGap
                }
            }

            // Place remaining individuals not part of families on this layer
            for (id in nodeIds) {
                if (placed.contains(id)) continue
                if (individuals.containsKey(id)) {
                    val w = metrics.getWidth(id)
                    result.setPosition(id, cursorX, y)
                    cursorX += w + hGap
                    placed.add(id)
                }
            }
        }

        // Place children under each family on the next layer, centered
        for (f in families.values) {
            val famId = f.id
            val famLayer = layerByNode[famId] ?: continue
            val famX = result.getPosition(famId)?.x ?: 0.0
            val famW = metrics.getWidth(famId)
            val midX = famX + famW / 2.0

            val children = familyChildren.getOrDefault(famId, emptyList())
            if (children.isEmpty()) continue

            val childLayer = famLayer + 1
            val y = childLayer * (metrics.getHeight("any") + vGap)

            // Compute total width and start x so that the group is centered under family
            var totalW = 0.0
            for (cid in children) totalW += metrics.getWidth(cid)
            val totalGaps = hGap * (children.size - 1)
            val startX = midX - (totalW + totalGaps) / 2.0

            var x = startX
            for (cid in children) {
                if (!individuals.containsKey(cid)) continue
                result.setPosition(cid, x, y)
                x += metrics.getWidth(cid) + hGap
            }
        }

        return result
    }
}
