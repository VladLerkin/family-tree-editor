package com.family.tree.render

import com.family.tree.model.GedcomEvent
import com.family.tree.model.Individual
import com.family.tree.storage.ProjectRepository
import java.util.*

/**
 * Node metrics that size person rectangles based on their name and dates split into three lines:
 * first name on the first line, last name on the second line, dates on the third line, all centered.
 * Width is the max of line widths plus horizontal padding; height accounts for three lines.
 *
 * Measurements are approximate and independent of zoom; TreeRenderer applies zoom.
 */
class TextAwareNodeMetrics : NodeMetrics() {
    // Base font size (layout units, at zoom=1)
    private var baseFontSize = 12.0
    // Approximate average glyph width factor (em to px)
    private val AVG_CHAR_FACTOR = 0.6 // ~0.6 * font size per character
    // Padding around text (layout units)
    companion object {
        @JvmStatic fun horizontalPadding(): Double = H_PAD
        @JvmStatic fun verticalPadding(): Double = V_PAD
        @JvmStatic fun lineSpacing(): Double = LINE_SPACING
        private const val H_PAD = 12.0
        private const val V_PAD = 10.0
        private const val LINE_SPACING = 1.2
    }

    private var data: ProjectRepository.ProjectData? = null

    fun setData(data: ProjectRepository.ProjectData?) {
        this.data = data
    }

    fun setBaseFontSize(baseFontSize: Double) {
        if (baseFontSize > 6.0) this.baseFontSize = baseFontSize
    }

    override fun getWidth(nodeId: String): Double {
        val d = data
        if (d != null && nodeId.isNotEmpty() && isIndividual(nodeId, d)) {
            val ind = getIndividual(nodeId, d) ?: return super.getWidth(nodeId)

            val first = ind.firstName ?: ""
            val last = ind.lastName ?: ""
            val font = baseFontSize
            val w1 = approxTextWidth(first, font)
            val w2 = approxTextWidth(last, font)

            // Also consider date text width derived from events (BIRT/DEAT)
            val dateText = buildDateLine(ind, first, last)
            val w3 = approxTextWidth(dateText, font)

            val w = maxOf(maxOf(w1, w2), w3) + H_PAD * 2.0
            // Minimum width fallback
            return maxOf(w, 80.0)
        }
        return super.getWidth(nodeId)
    }

    override fun getHeight(nodeId: String): Double {
        val d = data
        if (d != null && nodeId.isNotEmpty() && isIndividual(nodeId, d)) {
            val lineH = baseFontSize * LINE_SPACING
            val h = V_PAD * 2.0 + lineH * 3.0
            return maxOf(h, 60.0)
        }
        return super.getHeight(nodeId)
    }

    private fun isIndividual(nodeId: String, data: ProjectRepository.ProjectData): Boolean {
        for (i in data.individuals) {
            if (nodeId == i.id) return true
        }
        return false
    }

    private fun getIndividual(nodeId: String, data: ProjectRepository.ProjectData): Individual? {
        for (i in data.individuals) {
            if (nodeId == i.id) return i
        }
        return null
    }

    private fun isCyrillic(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        for (c in text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
                return true
            }
        }
        return false
    }

    private fun formatDatesForWidth(birthDate: String?, deathDate: String?, firstName: String, lastName: String): String {
        val hasBirth = !birthDate.isNullOrBlank()
        val hasDeath = !deathDate.isNullOrBlank()

        if (!hasBirth && !hasDeath) return ""

        val isCyrillicText = isCyrillic(firstName) || isCyrillic(lastName)

        return if (hasBirth && hasDeath) {
            birthDate!!.trim() + " - " + deathDate!!.trim()
        } else if (hasBirth) {
            val prefix = if (isCyrillicText) "род.:" else "b.:"
            prefix + birthDate!!.trim()
        } else {
            val prefix = if (isCyrillicText) "ум.:" else "d.:"
            prefix + deathDate!!.trim()
        }
    }

    private fun buildDateLine(ind: Individual, firstName: String, lastName: String): String {
        val b = extractEventDate(ind, "BIRT")
        val d = extractEventDate(ind, "DEAT")
        return formatDatesForWidth(b, d, firstName, lastName)
    }

    private fun extractEventDate(ind: Individual, type: String): String? {
        val t = type.trim().uppercase(Locale.ROOT)
        for (ev in ind.events) {
            val evType = ev.type
            if (evType != null && t == evType.trim().uppercase(Locale.ROOT)) {
                return ev.date
            }
        }
        return null
    }

    fun approxTextWidth(s: String?, fontSize: Double): Double {
        if (s.isNullOrEmpty()) return 0.0
        // Rough estimate; good enough for sizing and centering without platform font metrics
        return s.length * fontSize * AVG_CHAR_FACTOR
    }
}
