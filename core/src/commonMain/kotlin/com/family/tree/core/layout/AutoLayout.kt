package com.family.tree.core.layout

import com.family.tree.core.ProjectData
import com.family.tree.core.model.Family
import com.family.tree.core.model.IndividualId
import kotlin.math.max
import kotlin.math.min

private const val SPACING_Y = 220.0
private const val NODE_WIDTH = 120.0
private const val FAMILY_WIDTH = 10.0
private const val DIST_SPOUSE_FAMILY = 5.0
private const val DIST_BETWEEN_BLOCKS = 50.0

private class LayerNode(val id: String, val isFamily: Boolean) {
    var localX: Double = 0.0 // relative to block
}

private class LayerBlock(val id: String, val generation: Int) {
    val nodes = mutableListOf<LayerNode>()
    var x: Double = 0.0 // Absolute X
    var width: Double = 0.0
    var idealX: Double = 0.0
    
    // Which families in the PREVIOUS generation are parents of this block?
    // (Used for top-down placement)
    val parentFamilyIds = mutableSetOf<String>()
    
    // Which individuals/families in the NEXT generation are children of this block?
    // (Used for bottom-up centering)
    val childIds = mutableSetOf<String>()
    
    fun addNode(nodeId: String, isFamily: Boolean) {
        val node = LayerNode(nodeId, isFamily)
        val w = if (isFamily) FAMILY_WIDTH else NODE_WIDTH
        if (nodes.isNotEmpty()) {
            width += DIST_SPOUSE_FAMILY
        }
        node.localX = width
        nodes.add(node)
        width += w
    }
}

/**
 * Automatically calculates a hierarchical layout for a family tree using a layered (Sugiyama-like) DAG approach.
 */
fun calculateAutoLayout(projectData: ProjectData): ProjectLayout {
    if (projectData.individuals.isEmpty()) return ProjectLayout()

    // 1. Find Weakly Connected Components
    val adj = mutableMapOf<String, MutableSet<String>>()
    for (ind in projectData.individuals) {
        adj[ind.id.value] = mutableSetOf()
    }
    for (fam in projectData.families) {
        val members = mutableListOf<String>()
        fam.husbandId?.let { members.add(it.value) }
        fam.wifeId?.let { members.add(it.value) }
        members.addAll(fam.childrenIds.map { it.value })
        
        for (i in members.indices) {
            for (j in i + 1 until members.size) {
                adj.getOrPut(members[i]) { mutableSetOf() }.add(members[j])
                adj.getOrPut(members[j]) { mutableSetOf() }.add(members[i])
            }
        }
    }
    
    val visited = mutableSetOf<String>()
    val components = mutableListOf<List<String>>()
    
    for (ind in projectData.individuals) {
        val id = ind.id.value
        if (id !in visited) {
            val comp = mutableListOf<String>()
            val queue = mutableListOf(id)
            visited.add(id)
            
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                comp.add(curr)
                val neighbors = adj[curr] ?: emptySet()
                for (n in neighbors) {
                    if (n !in visited) {
                        visited.add(n)
                        queue.add(n)
                    }
                }
            }
            components.add(comp)
        }
    }
    
    // 2. Layout each component independently
    val allPositions = mutableMapOf<String, NodePos>()
    
    class ComponentBounds(
        val positions: Map<String, NodePos>,
        val minX: Double,
        val maxX: Double,
        val minY: Double,
        val maxY: Double
    )
    
    val componentBoundsList = mutableListOf<ComponentBounds>()
    
    for (compIds in components) {
        val compSet = compIds.toSet()
        val compIndividuals = projectData.individuals.filter { it.id.value in compSet }
        val compFamilies = projectData.families.filter { fam -> 
            (fam.husbandId?.value in compSet) || (fam.wifeId?.value in compSet) || fam.childrenIds.any { it.value in compSet }
        }
        
        val compProject = ProjectData(compIndividuals, compFamilies)
        val positions = layoutSingleComponent(compProject)
        
        if (positions.isEmpty()) continue
        
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        
        for ((id, pos) in positions) {
            // Include node width/height in bounding box
            val isFamily = compFamilies.any { it.id.value == id }
            val w = if (isFamily) FAMILY_WIDTH else NODE_WIDTH
            val h = if (isFamily) FAMILY_WIDTH else 60.0 // approx height
            
            if (pos.x < minX) minX = pos.x
            if (pos.x + w > maxX) maxX = pos.x + w
            if (pos.y < minY) minY = pos.y
            if (pos.y + h > maxY) maxY = pos.y + h
        }
        
        componentBoundsList.add(ComponentBounds(positions, minX, maxX, minY, maxY))
    }
    
    // 3. 2D Bin Packing - arrange clusters in a checkerboard grid
    componentBoundsList.sortByDescending { it.maxY - it.minY }
    
    val MAX_ROW_WIDTH = 3000.0
    val COMPONENT_GAP = 150.0
    
    var currentX = 0.0
    var currentY = 0.0
    var rowMaxHeight = 0.0
    
    for (comp in componentBoundsList) {
        val compWidth = comp.maxX - comp.minX
        val compHeight = comp.maxY - comp.minY
        
        if (currentX + compWidth > MAX_ROW_WIDTH && currentX > 0) {
            currentX = 0.0
            currentY += rowMaxHeight + COMPONENT_GAP
            rowMaxHeight = 0.0
        }
        
        val offsetX = currentX - comp.minX
        val offsetY = currentY - comp.minY
        
        for ((id, pos) in comp.positions) {
            allPositions[id] = NodePos(pos.x + offsetX, pos.y + offsetY)
        }
        
        currentX += compWidth + COMPONENT_GAP
        rowMaxHeight = max(rowMaxHeight, compHeight)
    }

    return ProjectLayout(
        zoom = 1.0,
        viewOriginX = 0.0,
        viewOriginY = 0.0,
        nodePositions = allPositions,
        positionsAreCenters = true
    )
}

private fun layoutSingleComponent(projectData: ProjectData): Map<String, NodePos> {
    val nodePositions = mutableMapOf<String, NodePos>()
    
    val childrenToFamily = mutableMapOf<IndividualId, Family>()
    val spouseToFamilies = mutableMapOf<IndividualId, MutableList<Family>>()
    
    for (family in projectData.families) {
        family.husbandId?.let { spouseToFamilies.getOrPut(it) { mutableListOf() }.add(family) }
        family.wifeId?.let { spouseToFamilies.getOrPut(it) { mutableListOf() }.add(family) }
        for (childId in family.childrenIds) {
            childrenToFamily[childId] = family
        }
    }
    
    // 1. Assign generations using BFS (Relative Generation Assignment)
    val generations = mutableMapOf<String, Int>()
    val unassigned = projectData.individuals.map { it.id.value }.toMutableSet()

    while (unassigned.isNotEmpty()) {
        val startNode = unassigned.first()
        val queue = mutableListOf(startNode)
        generations[startNode] = 0
        unassigned.remove(startNode)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            val currGen = generations[curr]!!

            // Find all relatives of curr
            val spouses = mutableListOf<String>()
            val children = mutableListOf<String>()
            val parents = mutableListOf<String>()
            val siblings = mutableListOf<String>()

            val currIndId = IndividualId(curr)
            val spouseFams = spouseToFamilies[currIndId] ?: emptyList()
            for (fam in spouseFams) {
                val h = fam.husbandId?.value
                val w = fam.wifeId?.value
                if (h != null && h != curr) spouses.add(h)
                if (w != null && w != curr) spouses.add(w)
                for (c in fam.childrenIds) {
                    children.add(c.value)
                }
            }

            val childFam = childrenToFamily[currIndId]
            if (childFam != null) {
                childFam.husbandId?.value?.let { parents.add(it) }
                childFam.wifeId?.value?.let { parents.add(it) }
                for (sibId in childFam.childrenIds) {
                    val sib = sibId.value
                    if (sib != curr) {
                        siblings.add(sib)
                    }
                }
            }

            for (s in spouses) {
                if (s !in generations) {
                    generations[s] = currGen
                    queue.add(s)
                    unassigned.remove(s)
                }
            }
            for (sib in siblings) {
                if (sib !in generations) {
                    generations[sib] = currGen
                    queue.add(sib)
                    unassigned.remove(sib)
                }
            }
            for (c in children) {
                if (c !in generations) {
                    generations[c] = currGen + 1
                    queue.add(c)
                    unassigned.remove(c)
                }
            }
            for (p in parents) {
                if (p !in generations) {
                    generations[p] = currGen - 1
                    queue.add(p)
                    unassigned.remove(p)
                }
            }
        }
    }

    // Assign families their generations
    for (family in projectData.families) {
        val hGen = family.husbandId?.let { generations[it.value] }
        val wGen = family.wifeId?.let { generations[it.value] }
        if (hGen != null || wGen != null) {
            generations[family.id.value] = max(hGen ?: wGen ?: 0, wGen ?: hGen ?: 0)
        } else {
            // No spouses, look at children
            val childGen = family.childrenIds.firstOrNull()?.let { generations[it.value] }
            if (childGen != null) {
                generations[family.id.value] = childGen - 1
            } else {
                generations[family.id.value] = 0
            }
        }
    }

    // Shift generations so the minimum is 0
    if (generations.isNotEmpty()) {
        val minGen = generations.values.minOrNull() ?: 0
        for ((id, gen) in generations) {
            generations[id] = gen - minGen
        }
    }
    
    val maxGen = generations.values.maxOrNull() ?: 0
    val blocksByGen = mutableMapOf<Int, MutableList<LayerBlock>>()
    val blockOfNode = mutableMapOf<String, LayerBlock>()
    
    // 2. Build blocks for each generation
    for (gen in 0..maxGen) {
        val genBlocks = mutableListOf<LayerBlock>()
        
        val individualsInGen = projectData.individuals.filter { generations[it.id.value] == gen }.map { it.id.value }.toSet()
        val familiesInGen = projectData.families.filter { generations[it.id.value] == gen }.map { it.id.value }.toSet()
        
        val unvisited = mutableSetOf<String>()
        unvisited.addAll(individualsInGen)
        unvisited.addAll(familiesInGen)
        
        // Build adjacency list for this generation
        val adj = mutableMapOf<String, MutableList<String>>()
        for (famId in familiesInGen) {
            val fam = projectData.families.find { it.id.value == famId }!!
            val hId = fam.husbandId?.value
            val wId = fam.wifeId?.value
            if (hId != null && hId in individualsInGen) {
                adj.getOrPut(famId) { mutableListOf() }.add(hId)
                adj.getOrPut(hId) { mutableListOf() }.add(famId)
            }
            if (wId != null && wId in individualsInGen) {
                adj.getOrPut(famId) { mutableListOf() }.add(wId)
                adj.getOrPut(wId) { mutableListOf() }.add(famId)
            }
        }
        
        var blockCounter = 0
        while (unvisited.isNotEmpty()) {
            val startNode = unvisited.first()
            val component = mutableListOf<String>()
            val queue = mutableListOf(startNode)
            unvisited.remove(startNode)
            
            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                component.add(curr)
                val neighbors = adj[curr] ?: emptyList()
                for (n in neighbors) {
                    if (n in unvisited) {
                        unvisited.remove(n)
                        queue.add(n)
                    }
                }
            }
            
            val block = LayerBlock("b_${gen}_${blockCounter++}", gen)
            
            // Sort component nodes to place individuals and families in a logical linear order (e.g. H1 -> F1 -> W -> F2 -> H2)
            val sortedComponent = if (component.size > 1) {
                val localAdj = mutableMapOf<String, MutableSet<String>>()
                for (nodeId in component) {
                    localAdj[nodeId] = mutableSetOf()
                }
                for (nodeId in component) {
                    val neighbors = adj[nodeId] ?: emptyList()
                    for (n in neighbors) {
                        if (n in localAdj) {
                            localAdj[nodeId]!!.add(n)
                        }
                    }
                }
                
                // Find a leaf node (degree == 1) to start DFS. Prefer individuals over families for cleaner start.
                val start = component.minByOrNull { nodeId ->
                    val deg = localAdj[nodeId]?.size ?: 0
                    val isFam = nodeId in familiesInGen
                    val priority = if (deg == 1) 0 else 1
                    val typePriority = if (isFam) 1 else 0
                    priority * 10 + typePriority
                } ?: component.first()
                
                val visitedNodes = mutableSetOf<String>()
                val orderedNodes = mutableListOf<String>()
                
                fun dfsSort(u: String) {
                    visitedNodes.add(u)
                    orderedNodes.add(u)
                    val neighbors = localAdj[u] ?: emptySet()
                    for (v in neighbors) {
                        if (v !in visitedNodes) {
                            dfsSort(v)
                        }
                    }
                }
                
                dfsSort(start)
                
                // Safety fallback: if anything wasn't visited (should not happen for connected comp)
                for (nodeId in component) {
                    if (nodeId !in visitedNodes) {
                        orderedNodes.add(nodeId)
                    }
                }
                orderedNodes
            } else {
                component
            }

            for (nodeId in sortedComponent) {
                val isFamily = nodeId in familiesInGen
                block.addNode(nodeId, isFamily)
                blockOfNode[nodeId] = block
                
                // Track parent relationships for Top-Down placement
                if (!isFamily) {
                    val childOfFam = childrenToFamily[IndividualId(nodeId)]
                    if (childOfFam != null) {
                        block.parentFamilyIds.add(childOfFam.id.value)
                    }
                } else {
                    val fam = projectData.families.find { it.id.value == nodeId }!!
                    for (c in fam.childrenIds) {
                        block.childIds.add(c.value)
                    }
                }
            }
            genBlocks.add(block)
        }
        blocksByGen[gen] = genBlocks
    }
    // 3. Build Spanning Tree and layout recursively
    //    This uses RELATIVE Y positions (not global gen*SPACING_Y bands)
    //    so different subtrees end up at different vertical positions
    
    class TreeNode(val block: LayerBlock) {
        val children = mutableListOf<TreeNode>()
        var x = 0.0
        var y = 0.0
        var subtreeWidth = 0.0
    }
    
    val treeNodeOf = mutableMapOf<LayerBlock, TreeNode>()
    for (gen in 0..maxGen) {
        for (block in blocksByGen[gen] ?: emptyList()) {
            treeNodeOf[block] = TreeNode(block)
        }
    }
    
    // Build undirected block connection graph
    val allBlocks = blocksByGen.values.flatten()
    val blockNeighbors = mutableMapOf<LayerBlock, MutableSet<LayerBlock>>()
    for (block in allBlocks) {
        val parentBlocks = allBlocks.filter { parentBlock ->
            block.parentFamilyIds.any { pfId ->
                parentBlock.nodes.any { it.id == pfId }
            }
        }
        for (pb in parentBlocks) {
            blockNeighbors.getOrPut(block) { mutableSetOf() }.add(pb)
            blockNeighbors.getOrPut(pb) { mutableSetOf() }.add(block)
        }
    }

    // Build a single spanning tree of the entire connected component using BFS
    val rootBlock = allBlocks.minByOrNull { it.generation } ?: allBlocks.first()
    val visitedBlocks = mutableSetOf<LayerBlock>()
    val queue = mutableListOf<LayerBlock>()
    
    visitedBlocks.add(rootBlock)
    queue.add(rootBlock)
    
    while (queue.isNotEmpty()) {
        val currBlock = queue.removeFirst()
        val currTn = treeNodeOf[currBlock]!!
        val neighbors = blockNeighbors[currBlock] ?: emptySet()
        
        for (neighbor in neighbors) {
            if (neighbor !in visitedBlocks) {
                visitedBlocks.add(neighbor)
                queue.add(neighbor)
                
                val neighborTn = treeNodeOf[neighbor]!!
                currTn.children.add(neighborTn)
            }
        }
    }
    
    // Safety check: if there are any disconnected blocks (should not happen), attach them to root
    val rootTn = treeNodeOf[rootBlock]!!
    for (block in allBlocks) {
        if (block !in visitedBlocks) {
            visitedBlocks.add(block)
            rootTn.children.add(treeNodeOf[block]!!)
        }
    }

    fun sortChildrenRecursively(tn: TreeNode) {
        tn.children.sortBy { childTn ->
            val parentFamId = childTn.block.parentFamilyIds.find { pfId ->
                tn.block.nodes.any { it.id == pfId }
            }
            if (parentFamId != null) {
                tn.block.nodes.indexOfFirst { it.id == parentFamId }
            } else {
                999
            }
        }
        for (c in tn.children) {
            sortChildrenRecursively(c)
        }
    }
    sortChildrenRecursively(rootTn)
    
    fun shiftSubtree(node: TreeNode, offset: Double) {
        node.x += offset
        for (c in node.children) {
            shiftSubtree(c, offset)
        }
    }

    fun getSubtreeHeight(node: TreeNode): Int {
        if (node.children.isEmpty()) return 1
        return 1 + node.children.maxOf { getSubtreeHeight(it) }
    }

    fun isSingleSibling(n: TreeNode): Boolean {
        return n.children.isEmpty() && n.block.nodes.size == 1 && !n.block.nodes[0].isFamily
    }

    fun hasFamilyOrChildren(n: TreeNode): Boolean {
        return n.block.nodes.any { it.isFamily } || n.children.isNotEmpty()
    }

    fun getChildRefX(c: TreeNode, parentBlock: LayerBlock): Double {
        val childNode = c.block.nodes.find { it.id in parentBlock.childIds && !it.isFamily }
        return if (childNode != null) {
            c.x + childNode.localX + NODE_WIDTH / 2.0
        } else {
            c.x + c.block.width / 2.0
        }
    }

    fun getBlockRefX(block: LayerBlock): Double {
        val parentCards = block.nodes.filter { !it.isFamily }
        return if (parentCards.size == 1) {
            parentCards.first().localX + NODE_WIDTH / 2.0
        } else if (parentCards.size >= 2) {
            (parentCards.first().localX + parentCards.last().localX + NODE_WIDTH) / 2.0
        } else {
            block.width / 2.0
        }
    }

    class SubtreeLayoutResult(
        val leftContour: MutableMap<Int, Double>,
        val rightContour: MutableMap<Int, Double>
    )

    fun layoutSubtree(node: TreeNode, baselineStagger: Double, localStagger: Double): SubtreeLayoutResult {
        val accumulatedStagger = baselineStagger + localStagger
        val baseStartY = node.block.generation * SPACING_Y
        node.y = baseStartY + accumulatedStagger

        val leftContour = mutableMapOf<Int, Double>()
        val rightContour = mutableMapOf<Int, Double>()

        if (node.children.isEmpty()) {
            node.x = 0.0
            val contourStartY = (node.block.generation * SPACING_Y + accumulatedStagger).toInt()
            val endY = (node.block.generation * SPACING_Y + accumulatedStagger + SPACING_Y).toInt()
            for (y in contourStartY until endY) {
                leftContour[y] = 0.0
                rightContour[y] = node.block.width
            }
            return SubtreeLayoutResult(leftContour, rightContour)
        }

        val downwardChildren = mutableListOf<TreeNode>()
        val upwardChildren = mutableListOf<TreeNode>()
        val sameGenChildren = mutableListOf<TreeNode>()

        for (c in node.children) {
            if (c.block.generation > node.block.generation) {
                downwardChildren.add(c)
            } else if (c.block.generation < node.block.generation) {
                upwardChildren.add(c)
            } else {
                sameGenChildren.add(c)
            }
        }

        val childResMap = mutableMapOf<TreeNode, SubtreeLayoutResult>()
        var familyIndex = 0
        val parentHasSpouses = node.block.nodes.any { !it.isFamily }
        for (c in node.children) {
            val siblingShift = if (c.block.generation > node.block.generation && parentHasSpouses && hasFamilyOrChildren(c)) {
                val shift = if (familyIndex % 2 == 0) {
                    70.0
                } else {
                    130.0
                }
                familyIndex++
                shift
            } else {
                0.0
            }
            val res = layoutSubtree(c, baselineStagger + localStagger, siblingShift)
            childResMap[c] = res
        }

        val combinedLeft = mutableMapOf<Int, Double>()
        val combinedRight = mutableMapOf<Int, Double>()

        val horizontalGroup = downwardChildren + sameGenChildren
        for ((idx, c) in horizontalGroup.withIndex()) {
            val res = childResMap[c]!!
            if (idx == 0) {
                val shiftAmount = -c.x
                c.x = 0.0
                if (shiftAmount != 0.0) {
                    shiftSubtree(c, shiftAmount)
                }
                for ((y, lx) in res.leftContour) {
                    combinedLeft[y] = lx + shiftAmount
                }
                for ((y, rx) in res.rightContour) {
                    combinedRight[y] = rx + shiftAmount
                }
            } else {
                var minOffset = 0.0
                val keys = res.leftContour.keys.intersect(combinedRight.keys)
                val currentGap = if (isSingleSibling(c) && isSingleSibling(horizontalGroup[idx - 1])) {
                    20.0
                } else {
                    DIST_BETWEEN_BLOCKS
                }
                for (y in keys) {
                    val required = combinedRight[y]!! + currentGap - res.leftContour[y]!!
                    if (required > minOffset) {
                        minOffset = required
                    }
                }
                val finalOffset = minOffset
                shiftSubtree(c, finalOffset)
                
                for ((y, lx) in res.leftContour) {
                    val shiftedLx = lx + finalOffset
                    combinedLeft[y] = min(combinedLeft[y] ?: Double.MAX_VALUE, shiftedLx)
                }
                for ((y, rx) in res.rightContour) {
                    val shiftedRx = rx + finalOffset
                    combinedRight[y] = max(combinedRight[y] ?: -Double.MAX_VALUE, shiftedRx)
                }
            }
        }

        val parentRefX = getBlockRefX(node.block)

        var parentX = if (downwardChildren.isNotEmpty()) {
            val firstChild = downwardChildren.first()
            val lastChild = downwardChildren.last()
            val firstChildRefX = getChildRefX(firstChild, node.block)
            val lastChildRefX = getChildRefX(lastChild, node.block)
            val childrenCenter = (firstChildRefX + lastChildRefX) / 2.0
            childrenCenter - parentRefX
        } else {
            0.0
        }
        
        val originalParentX = parentX
        
        val baseStartYInt = (node.block.generation * SPACING_Y + accumulatedStagger).toInt()
        val parentEndY = (node.block.generation * SPACING_Y + accumulatedStagger + SPACING_Y).toInt()
        for (y in baseStartYInt until parentEndY) {
            val rightLimit = combinedRight[y]
            if (rightLimit != null && parentX < rightLimit + DIST_BETWEEN_BLOCKS) {
                parentX = rightLimit + DIST_BETWEEN_BLOCKS
            }
        }

        val shiftAmount = parentX - originalParentX
        if (shiftAmount > 0.0) {
            for (c in horizontalGroup) {
                shiftSubtree(c, shiftAmount)
            }
            val shiftedLeft = mutableMapOf<Int, Double>()
            for ((y, x) in combinedLeft) {
                shiftedLeft[y] = x + shiftAmount
            }
            combinedLeft.clear()
            combinedLeft.putAll(shiftedLeft)

            val shiftedRight = mutableMapOf<Int, Double>()
            for ((y, x) in combinedRight) {
                shiftedRight[y] = x + shiftAmount
            }
            combinedRight.clear()
            combinedRight.putAll(shiftedRight)
        }
        
        node.x = parentX

        // Position and shift upward parent subtrees
        for (c in upwardChildren) {
            val res = childResMap[c]!!
            val childNodeInNode = node.block.nodes.find { it.id in c.block.childIds && !it.isFamily }
            val targetCenterX = if (childNodeInNode != null) {
                node.x + childNodeInNode.localX + NODE_WIDTH / 2.0
            } else {
                node.x + node.block.width / 2.0
            }
            
            val downwardD = c.children.filter { it.block.generation > c.block.generation }
            val targetParentX = if (downwardD.isEmpty()) {
                targetCenterX - getBlockRefX(c.block)
            } else {
                // Compute min/max of downward children positions RELATIVE to current c.x
                val allChildRelX = downwardD.map { getChildRefX(it, c.block) - c.x }
                val minChildRelX = allChildRelX.minOrNull()!!
                val maxChildRelX = allChildRelX.maxOrNull()!!
                val subtreeCenterRel = (minChildRelX + maxChildRelX) / 2.0
                val parentRefX = getBlockRefX(c.block)
                // Condition: which side is targetCenterX on after shift?
                // Determined by comparing parentRefX (relative) with subtreeCenterRel (relative):
                //   parentRefX <= subtreeCenterRel => targetCenterX will be left of downward span
                //   parentRefX >  subtreeCenterRel => targetCenterX will be right of downward span
                if (parentRefX <= subtreeCenterRel) {
                    targetCenterX + maxChildRelX - 2.0 * parentRefX
                } else {
                    targetCenterX + minChildRelX - 2.0 * parentRefX
                }
            }
            var shift = targetParentX - c.x
            
            // Avoid collision with already merged contours in c's generation range
            var minCollisionOffset = 0.0
            for ((y, lx) in res.leftContour) {
                val shiftedLx = lx + shift
                val rightLimit = combinedRight[y]
                if (rightLimit != null && shiftedLx < rightLimit + DIST_BETWEEN_BLOCKS) {
                    val req = rightLimit + DIST_BETWEEN_BLOCKS - shiftedLx
                    if (req > minCollisionOffset) {
                        minCollisionOffset = req
                    }
                }
            }
            shift += minCollisionOffset
            
            shiftSubtree(c, shift)
            
            // Merge shifted c contour
            for ((y, lx) in res.leftContour) {
                val shiftedLx = lx + shift
                combinedLeft[y] = min(combinedLeft[y] ?: Double.MAX_VALUE, shiftedLx)
            }
            for ((y, rx) in res.rightContour) {
                val shiftedRx = rx + shift
                combinedRight[y] = max(combinedRight[y] ?: -Double.MAX_VALUE, shiftedRx)
            }
        }

        // Add parent block to contours
        for (y in baseStartYInt until parentEndY) {
            combinedLeft[y] = min(combinedLeft[y] ?: Double.MAX_VALUE, parentX)
            combinedRight[y] = max(combinedRight[y] ?: -Double.MAX_VALUE, parentX + node.block.width)
        }

        var minSubtreeX = Double.MAX_VALUE
        for (x in combinedLeft.values) {
            if (x < minSubtreeX) minSubtreeX = x
        }
        if (minSubtreeX != 0.0 && minSubtreeX != Double.MAX_VALUE) {
            node.x -= minSubtreeX
            for (c in node.children) {
                shiftSubtree(c, -minSubtreeX)
            }
            val normLeft = mutableMapOf<Int, Double>()
            val normRight = mutableMapOf<Int, Double>()
            for ((y, x) in combinedLeft) normLeft[y] = x - minSubtreeX
            for ((y, x) in combinedRight) normRight[y] = x - minSubtreeX
            return SubtreeLayoutResult(normLeft, normRight)
        }

        return SubtreeLayoutResult(combinedLeft, combinedRight)
    }

    // Layout the single root tree
    layoutSubtree(rootTn, 0.0, 0.0)

    // 5. Finalize Coordinates from TreeNode positions
    for (tn in treeNodeOf.values) {
        for (node in tn.block.nodes) {
            val leftX = tn.x + node.localX
            nodePositions[node.id] = NodePos(leftX, tn.y)
        }
    }
    
    return nodePositions
}
