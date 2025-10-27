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
