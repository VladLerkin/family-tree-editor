package com.family.tree.render

interface GraphicsContext {
    fun setStrokeColor(r: Int, g: Int, b: Int, a: Double)
    fun setFillColor(r: Int, g: Int, b: Int, a: Double)
    fun setLineWidth(w: Double)
    fun setFontSize(size: Double)
    fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double)
    fun fillRect(x: Double, y: Double, w: Double, h: Double)
    fun drawRect(x: Double, y: Double, w: Double, h: Double)
    fun drawText(text: String, x: Double, y: Double)
    fun measureTextWidth(text: String): Double
}
