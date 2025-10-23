package com.family.tree.render

import java.util.concurrent.ConcurrentHashMap

/**
 * Simple cache for render-related metrics (e.g., text widths).
 */
class RenderCache {
    private val textWidthCache: MutableMap<String, Double> = ConcurrentHashMap()

    fun getTextWidth(text: String?): Double {
        if (text == null) return 0.0
        return textWidthCache.computeIfAbsent(text) { s -> s.length * 7.0 }
    }

    fun clear() {
        textWidthCache.clear()
    }
}
