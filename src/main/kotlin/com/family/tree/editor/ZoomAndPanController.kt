package com.family.tree.editor

class ZoomAndPanController {
    private var _zoom: Double = 1.0
    private var _minZoom: Double = 0.1
    private var _maxZoom: Double = 8.0
    private var _panX: Double = 0.0
    private var _panY: Double = 0.0
    private var zoomStep: Double = 1.1

    val zoom: Double get() = _zoom
    val minZoom: Double get() = _minZoom
    val maxZoom: Double get() = _maxZoom
    val panX: Double get() = _panX
    val panY: Double get() = _panY

    fun setZoom(level: Double) {
        if (level.isNaN() || level.isInfinite()) return
        _zoom = clamp(level, _minZoom, _maxZoom)
    }

    fun setZoomBounds(min: Double, max: Double) {
        if (min <= 0 || max <= 0 || min > max) throw IllegalArgumentException("Invalid zoom bounds")
        this._minZoom = min
        this._maxZoom = max
        setZoom(_zoom)
    }

    fun setZoomStep(stepFactor: Double) {
        if (stepFactor <= 1.0) throw IllegalArgumentException("Zoom step must be > 1.0")
        this.zoomStep = stepFactor
    }

    fun zoomIn() { setZoom(_zoom * zoomStep) }
    fun zoomOut() { setZoom(_zoom / zoomStep) }

    /**
     * Zoom in or out relative to a specific point (e.g., mouse cursor position).
     * The point stays at the same screen location after zoom.
     *
     * @param screenX X coordinate in screen/view space
     * @param screenY Y coordinate in screen/view space
     * @param zoomIn true to zoom in, false to zoom out
     */
    fun zoomAt(screenX: Double, screenY: Double, zoomIn: Boolean) {
        val oldZoom = _zoom
        val newZoom = if (zoomIn) oldZoom * zoomStep else oldZoom / zoomStep
        val clampedZoom = clamp(newZoom, _minZoom, _maxZoom)
        
        if (clampedZoom == oldZoom) return // No change, already at limit
        
        // Keep the point under cursor fixed by adjusting pan
        // Formula: newPan = cursor - (cursor - oldPan) * (newZoom / oldZoom)
        val k = clampedZoom / oldZoom
        _panX = screenX - (screenX - _panX) * k
        _panY = screenY - (screenY - _panY) * k
        _zoom = clampedZoom
    }

    fun panBy(dx: Double, dy: Double) {
        if (!dx.isFinite() || !dy.isFinite()) return
        this._panX += dx
        this._panY += dy
    }

    fun resetView() {
        this._zoom = 1.0
        this._panX = 0.0
        this._panY = 0.0
    }

    private fun clamp(v: Double, min: Double, max: Double): Double =
        kotlin.math.max(min, kotlin.math.min(max, v))
}
