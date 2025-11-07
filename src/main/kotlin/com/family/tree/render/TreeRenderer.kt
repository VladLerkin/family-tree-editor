package com.family.tree.render

import com.family.tree.layout.LayoutResult
import com.family.tree.model.Family
import com.family.tree.model.Gender
import com.family.tree.model.GedcomEvent
import com.family.tree.model.Individual
import com.family.tree.model.Relationship
import com.family.tree.storage.ProjectRepository
import java.awt.geom.Point2D
import java.util.Locale

/**
 * Renders nodes and edges to a framework-agnostic GraphicsContext.
 */
class TreeRenderer @JvmOverloads constructor(private val metrics: NodeMetrics = NodeMetrics()) {

    @JvmOverloads
    fun render(data: ProjectRepository.ProjectData?, layout: LayoutResult?, g: GraphicsContext, zoom: Double = 1.0, panX: Double = 0.0, panY: Double = 0.0) {
        if (data == null || layout == null || g == null) return

        // Build id sets to differentiate individuals and families
        val individualIds: MutableSet<String> = HashSet()
        for (i in data.individuals) individualIds.add(i.id)
        val familyIds: MutableSet<String> = HashSet()
        for (f in data.families) familyIds.add(f.id)

        // Map for quick lookup of Individuals by id (to access gender)
        val individualsById: MutableMap<String, Individual> = HashMap()
        for (i in data.individuals) {
            individualsById[i.id] = i
        }
        // Map for quick lookup of Families by id
        val familiesById: MutableMap<String, Family> = HashMap()
        for (f in data.families) {
            familiesById[f.id] = f
        }

        // Draw nodes
        g.setLineWidth(1.5 * zoom)
        for (id in layout.getNodeIds()) {
            val p: Point2D = layout.getPosition(id) ?: continue
            val w = metrics.getWidth(id) * zoom
            val h = metrics.getHeight(id) * zoom
            val x = p.x * zoom + panX
            val y = p.y * zoom + panY
            val label = id

            if (individualIds.contains(id)) {
                val ind = individualsById[id]
                if (ind != null && ind.gender == Gender.FEMALE) {
                    // Light yellow for female (matching screenshot and KMP)
                    g.setFillColor(255, 255, 192, 1.0)
                    g.setStrokeColor(120, 110, 40, 1.0)
                } else if (ind != null && ind.gender == Gender.MALE) {
                    // Light blue/lavender for male (matching screenshot and KMP)
                    g.setFillColor(208, 208, 255, 1.0)
                    g.setStrokeColor(50, 50, 80, 1.0)
                } else {
                    // Unknown/other
                    g.setFillColor(250, 250, 250, 1.0)
                    g.setStrokeColor(176, 190, 197, 1.0)
                }
                if (ind != null) {
                    val first = ind.firstName ?: ""
                    val last = ind.lastName ?: ""

                    // Draw background box first
                    g.fillRect(x, y, w, h)
                    g.drawRect(x, y, w, h)

                    // Text layout: three lines, centered - scaled 2x for better display
                    val fontSize = 24.0 * kotlin.math.max(zoom, 0.1)  // 12.0 * 2
                    g.setFontSize(fontSize)
                    val lineH = fontSize * TextAwareNodeMetrics.lineSpacing()
                    val padV = TextAwareNodeMetrics.verticalPadding() * zoom
                    val cx = x + w / 2.0
                    val y1 = y + padV + lineH // baseline of first line
                    val y2 = y + padV + lineH * 2.0 // baseline of second line
                    val y3 = y + padV + lineH * 3.0 // baseline of third line (dates)

                    // Ensure label text is black
                    g.setFillColor(0, 0, 0, 1.0)

                    // First line centered - measure actual width
                    val w1 = g.measureTextWidth(first)
                    g.drawText(first, cx - w1 / 2.0, y1)

                    // Second line centered - measure actual width
                    val w2 = g.measureTextWidth(last)
                    g.drawText(last, cx - w2 / 2.0, y2)

                    // Third line: dates with appropriate formatting - measure actual width
                    val b = extractEventDate(ind, "BIRT")
                    val d = extractEventDate(ind, "DEAT")
                    val dateText = formatDates(b, d, first, last)
                    if (!dateText.isNullOrEmpty()) {
                        val w3 = g.measureTextWidth(dateText)
                        g.drawText(dateText, cx - w3 / 2.0, y3)
                    }

                    // Draw selection highlight frame if selected (for individuals too)
                    val sel: Set<String>? = RenderHighlightState.getSelectedIds()
                    if (sel != null && sel.contains(id)) {
                        val baseLW = 1.5 * zoom
                        g.setLineWidth(3.0 * zoom)
                        g.setStrokeColor(0, 180, 80, 1.0)
                        val pad = 2.0 * zoom
                        g.drawRect(x - pad, y - pad, w + pad * 2.0, h + pad * 2.0)
                        g.setLineWidth(baseLW)
                    }

                    // Skip the generic label drawing below since we already drew text
                    continue
                }
            } else if (familyIds.contains(id)) {
                // Do not visualize family nodes on the canvas per requirement
                continue
            } else {
                g.setFillColor(235, 235, 235, 1.0)
                g.setStrokeColor(60, 60, 60, 1.0)
                g.fillRect(x, y, w, h)
                g.drawRect(x, y, w, h)

                // Ensure label text is black
                g.setFillColor(0, 0, 0, 1.0)
                g.drawText(label, x + 8 * zoom, y + 20 * zoom)
                continue
            }

            // Default fallback (should rarely reach here)
            g.fillRect(x, y, w, h)
            g.drawRect(x, y, w, h)

            // Highlight selected nodes with a distinct colored frame
            val sel: Set<String>? = RenderHighlightState.getSelectedIds()
            if (sel != null && sel.contains(id)) {
                val oldW = 1.5
                g.setLineWidth(3.0)
                g.setStrokeColor(0, 180, 80, 1.0) // vivid green frame
                g.drawRect(x - 2, y - 2, w + 4, h + 4)
                g.setLineWidth(oldW)
            }

            // Ensure label text is black
            g.setFillColor(0, 0, 0, 1.0)
            g.drawText(label, x + 8 * zoom, y + 20 * zoom)
        }

        // Draw family-aligned edges (spouse bar + child stem), like in the reference screenshot
        g.setStrokeColor(60, 60, 60, 1.0)
        g.setLineWidth(3.0 * zoom)

        // Collect relationships by family
        val spousesByFamily: MutableMap<String, MutableList<String>> = HashMap() // familyId -> spouseIds
        val childrenByFamily: MutableMap<String, MutableList<String>> = HashMap() // familyId -> childIds
        for (rel in data.relationships) {
            val toId = rel.toId ?: continue
            val fromId = rel.fromId ?: continue
            if (rel.type == Relationship.Type.SPOUSE_TO_FAMILY) {
                spousesByFamily.computeIfAbsent(toId) { ArrayList() }.add(fromId)
            } else if (rel.type == Relationship.Type.FAMILY_TO_CHILD) {
                childrenByFamily.computeIfAbsent(fromId) { ArrayList() }.add(toId)
            }
        }

        // Families involved
        val famIds: MutableSet<String> = HashSet()
        famIds.addAll(spousesByFamily.keys)
        famIds.addAll(childrenByFamily.keys)

        val barGap = 6.0 * zoom // gap below spouse boxes
        val singleBarHalf = 20.0 * zoom // half-width when only one spouse

        for (famId in famIds) {
            val spouses: List<String> = spousesByFamily.getOrDefault(famId, listOf())
            val children: List<String> = childrenByFamily.getOrDefault(famId, listOf())

            var barY = Double.NEGATIVE_INFINITY
            var minX = Double.POSITIVE_INFINITY
            var maxX = Double.NEGATIVE_INFINITY
            val spouseAnchors: MutableList<DoubleArray> = ArrayList() // [xCenter, bottomY]

            for (sId in spouses) {
                val ps = layout.getPosition(sId) ?: continue
                val w = metrics.getWidth(sId) * zoom
                val h = metrics.getHeight(sId) * zoom
                val xCenter = ps.x * zoom + panX + w / 2.0
                val bottomY = ps.y * zoom + panY + h
                spouseAnchors.add(doubleArrayOf(xCenter, bottomY))
                if (xCenter < minX) minX = xCenter
                if (xCenter > maxX) maxX = xCenter
                if (bottomY > barY) barY = bottomY
            }

            // Fallback to family position if no spouse positions are known
            val barX1: Double
            val barX2: Double
            if (spouseAnchors.isEmpty()) {
                val pf = layout.getPosition(famId) ?: continue // nothing to draw
                val w = metrics.getWidth(famId) * zoom
                val xMid = pf.x * zoom + panX + w / 2.0
                val y = pf.y * zoom + panY
                barY = y // use family Y as bar Y
                barX1 = xMid - singleBarHalf
                barX2 = xMid + singleBarHalf
            } else if (spouseAnchors.size == 1) {
                val xMid = spouseAnchors[0][0]
                barY = spouseAnchors[0][1] + barGap
                barX1 = xMid - singleBarHalf
                barX2 = xMid + singleBarHalf
                // vertical from the single spouse
                g.drawLine(xMid, spouseAnchors[0][1], xMid, barY)
            } else {
                barY += barGap
                barX1 = minX
                barX2 = maxX
                // verticals from each spouse down to the bar
                for (a in spouseAnchors) {
                    g.drawLine(a[0], a[1], a[0], barY)
                }
            }

            // Horizontal bar
            g.drawLine(barX1, barY, barX2, barY)
            val barMidX = (barX1 + barX2) / 2.0

            // Children alignment: a lower bar aligned to children, connected via a short stem from the spouses' bar
            if (children.isNotEmpty()) {
                var minChildX = Double.POSITIVE_INFINITY
                var maxChildX = Double.NEGATIVE_INFINITY
                var minTopY = Double.POSITIVE_INFINITY
                val childAnchors: MutableList<DoubleArray> = ArrayList() // [xCenter, topY]
                for (cId in children) {
                    val pc = layout.getPosition(cId) ?: continue
                    val w = metrics.getWidth(cId) * zoom
                    val xCenter = pc.x * zoom + panX + w / 2.0
                    val topY = pc.y * zoom + panY
                    childAnchors.add(doubleArrayOf(xCenter, topY))
                    if (xCenter < minChildX) minChildX = xCenter
                    if (xCenter > maxChildX) maxChildX = xCenter
                    if (topY < minTopY) minTopY = topY
                }
                if (childAnchors.isNotEmpty()) {
                    val childBarGap = 8.0 * zoom // distance above children
                    val childBarY = minTopY - childBarGap

                    if (childAnchors.size == 1) {
                        // Single child: route the stem toward the child's center so it looks correct even if child is offset.
                        val cx = childAnchors[0][0]
                        val ct = childAnchors[0][1]
                        val midY = (barY + childBarY) / 2.0
                        // vertical from spouses' bar midpoint to mid level
                        g.drawLine(barMidX, barY, barMidX, midY)
                        // horizontal at mid level toward the child center
                        g.drawLine(kotlin.math.min(barMidX, cx), midY, kotlin.math.max(barMidX, cx), midY)
                        // vertical from child center down to children bar level
                        g.drawLine(cx, midY, cx, childBarY)
                        // vertical down to the child
                        g.drawLine(cx, childBarY, cx, ct)
                    } else {
                        // Multiple children: route stem to the center of the children range, then span the children bar.
                        val childMidX = (minChildX + maxChildX) / 2.0
                        val midY = (barY + childBarY) / 2.0
                        // vertical from spouses' bar midpoint to mid level
                        g.drawLine(barMidX, barY, barMidX, midY)
                        // horizontal at mid level toward the children range center
                        g.drawLine(kotlin.math.min(barMidX, childMidX), midY, kotlin.math.max(barMidX, childMidX), midY)
                        // vertical from children range center down to the children bar level
                        g.drawLine(childMidX, midY, childMidX, childBarY)
                        // horizontal children bar spanning children range
                        g.drawLine(minChildX, childBarY, maxChildX, childBarY)
                        // verticals from children bar down to each child top
                        for (a in childAnchors) {
                            g.drawLine(a[0], childBarY, a[0], a[1])
                        }
                    }
                }
            }
        }
    }

    /**
     * Detects if the text contains Cyrillic characters.
     */
    private fun isCyrillic(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        for (c in text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                return true
            }
        }
        return false
    }

    private fun extractEventDate(ind: Individual?, type: String?): String? {
        if (ind == null || type == null) return null
        val t = type.trim().uppercase(Locale.ROOT)
        for (ev in ind.events) {
            val evType = ev.type ?: continue
            if (t == evType.trim().uppercase(Locale.ROOT)) {
                return ev.date
            }
        }
        return null
    }

    /**
     * Formats birth and death dates according to the requirements:
     * - Both dates: "birthDate - deathDate"
     * - Only birth: "b.:" or "род.:" prefix
     * - Only death: "d.:" or "ум.:" prefix
     * The language is detected from the individual's name.
     */
    private fun formatDates(birthDate: String?, deathDate: String?, firstName: String?, lastName: String?): String? {
        val hasBirth = !birthDate.isNullOrBlank()
        val hasDeath = !deathDate.isNullOrBlank()

        if (!hasBirth && !hasDeath) {
            return null
        }

        // Detect language from names
        val isCyrillicText = isCyrillic(firstName) || isCyrillic(lastName)

        return if (hasBirth && hasDeath) {
            // Both dates: show as "birthDate - deathDate"
            birthDate!!.trim() + " - " + deathDate!!.trim()
        } else if (hasBirth) {
            // Only birth date
            val prefix = if (isCyrillicText) "род.:" else "b.:"
            prefix + birthDate!!.trim()
        } else {
            // Only death date
            val prefix = if (isCyrillicText) "ум.:" else "d.:"
            prefix + deathDate!!.trim()
        }
    }
}
