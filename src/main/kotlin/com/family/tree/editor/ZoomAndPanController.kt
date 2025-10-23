package com.family.tree.editor

class ZoomAndPanController {
    private var zoom: Double = 1.0
    private var minZoom: Double = 0.1
    private var maxZoom: Double = 8.0
    private var panX: Double = 0.0
    private var panY: Double = 0.0
    private var zoomStep: Double = 1.1

    fun getZoom(): Double = zoom
    fun getMinZoom(): Double = minZoom
    fun getMaxZoom(): Double = maxZoom
    fun getPanX(): Double = panX
    fun getPanY(): Double = panY

    fun setZoom(level: Double) {
        if (level.isNaN() || level.isInfinite()) return
        zoom = clamp(level, minZoom, maxZoom)
    }

    fun setZoomBounds(min: Double, max: Double) {
        if (min <= 0 || max <= 0 || min > max) throw IllegalArgumentException("Invalid zoom bounds")
        this.minZoom = min
        this.maxZoom = max
        setZoom(zoom)
    }

    fun setZoomStep(stepFactor: Double) {
        if (stepFactor <= 1.0) throw IllegalArgumentException("Zoom step must be > 1.0")
        this.zoomStep = stepFactor
    }

    fun zoomIn() { setZoom(zoom * zoomStep) }
    fun zoomOut() { setZoom(zoom / zoomStep) }

    fun panBy(dx: Double, dy: Double) {
        if (!dx.isFinite() || !dy.isFinite()) return
        this.panX += dx
        this.panY += dy
    }

    fun resetView() {
        this.zoom = 1.0
        this.panX = 0.0
        this.panY = 0.0
    }

    private fun clamp(v: Double, min: Double, max: Double): Double =
        kotlin.math.max(min, kotlin.math.min(max, v))
}
