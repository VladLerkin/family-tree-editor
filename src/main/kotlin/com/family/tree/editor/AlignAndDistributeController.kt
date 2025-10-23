package com.family.tree.editor

import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Implements alignment and distribution tools on selected nodes using a PositionAccessor.
 */
class AlignAndDistributeController(
    private val selectionModel: SelectionModel,
    private val accessor: PositionAccessor
) {
    init {
        Objects.requireNonNull(selectionModel, "selectionModel")
        Objects.requireNonNull(accessor, "accessor")
    }

    enum class Alignment { TOP, MIDDLE, BOTTOM, LEFT, CENTER, RIGHT }
    enum class Distribution { HORIZONTAL, VERTICAL }

    interface PositionAccessor {
        fun getX(id: String): Double
        fun getY(id: String): Double
        fun getWidth(id: String): Double
        fun getHeight(id: String): Double
        fun setX(id: String, x: Double)
        fun setY(id: String, y: Double)
    }

    fun align(mode: Alignment) {
        val ids = ArrayList(selectionModel.getSelectedIds())
        if (ids.size <= 1) return

        val minX = ids.minOfOrNull { accessor.getX(it) } ?: 0.0
        val minY = ids.minOfOrNull { accessor.getY(it) } ?: 0.0
        val maxX = ids.maxOfOrNull { accessor.getX(it) + accessor.getWidth(it) } ?: 0.0
        val maxY = ids.maxOfOrNull { accessor.getY(it) + accessor.getHeight(it) } ?: 0.0
        val centerX = (minX + maxX) / 2.0
        val centerY = (minY + maxY) / 2.0

        for (id in ids) {
            val x = accessor.getX(id)
            val y = accessor.getY(id)
            val w = accessor.getWidth(id)
            val h = accessor.getHeight(id)

            when (mode) {
                Alignment.LEFT -> accessor.setX(id, minX)
                Alignment.RIGHT -> accessor.setX(id, maxX - w)
                Alignment.CENTER -> accessor.setX(id, centerX - w / 2.0)
                Alignment.TOP -> accessor.setY(id, minY)
                Alignment.BOTTOM -> accessor.setY(id, maxY - h)
                Alignment.MIDDLE -> accessor.setY(id, centerY - h / 2.0)
            }
        }
    }

    fun distribute(mode: Distribution) {
        val ids = ArrayList(selectionModel.getSelectedIds())
        if (ids.size <= 2) return

        if (mode == Distribution.HORIZONTAL) {
            ids.sortBy { accessor.getX(it) }
            val left = accessor.getX(ids.first())
            val last = ids.last()
            val right = accessor.getX(last) + accessor.getWidth(last)
            val totalWidth = ids.sumOf { accessor.getWidth(it) }
            val gap = (right - left - totalWidth) / (ids.size - 1)

            var cursor = left
            for (id in ids) {
                accessor.setX(id, cursor)
                cursor += accessor.getWidth(id) + gap
            }
        } else {
            ids.sortBy { accessor.getY(it) }
            val top = accessor.getY(ids.first())
            val last = ids.last()
            val bottom = accessor.getY(last) + accessor.getHeight(last)
            val totalHeight = ids.sumOf { accessor.getHeight(it) }
            val gap = (bottom - top - totalHeight) / (ids.size - 1)

            var cursor = top
            for (id in ids) {
                accessor.setY(id, cursor)
                cursor += accessor.getHeight(id) + gap
            }
        }
    }
}
