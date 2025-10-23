package com.family.tree.editor

/**
 * Simple immutable rectangle helper for selection ranges.
 * Kept API-compatible with Java usage.
 */
data class Rect(val x: Double, val y: Double, val width: Double, val height: Double) {
    init {
        require(width >= 0 && height >= 0) { "Negative width/height" }
    }

    fun contains(px: Double, py: Double): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }

    fun intersects(other: Rect?): Boolean {
        if (other == null) return false
        return this.x < other.x + other.width &&
               this.x + this.width > other.x &&
               this.y < other.y + other.height &&
               this.y + this.height > other.y
    }
}