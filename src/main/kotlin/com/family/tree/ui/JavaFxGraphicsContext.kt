package com.family.tree.ui

import com.family.tree.render.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font

class JavaFxGraphicsContext(private val gc: javafx.scene.canvas.GraphicsContext) : GraphicsContext {
    override fun setStrokeColor(r: Int, g: Int, b: Int, a: Double) {
        gc.stroke = Color.rgb(r, g, b, a)
    }

    override fun setFillColor(r: Int, g: Int, b: Int, a: Double) {
        gc.fill = Color.rgb(r, g, b, a)
    }

    override fun setLineWidth(w: Double) {
        gc.lineWidth = w
    }

    override fun setFontSize(size: Double) {
        try {
            gc.font = Font.font(size)
        } catch (_: Throwable) {
            // ignore if font cannot be set in current context
        }
    }

    override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
        gc.strokeLine(x1, y1, x2, y2)
    }

    override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
        gc.fillRect(x, y, w, h)
    }

    override fun drawRect(x: Double, y: Double, w: Double, h: Double) {
        gc.strokeRect(x, y, w, h)
    }

    override fun drawText(text: String, x: Double, y: Double) {
        gc.fillText(text, x, y)
    }

    override fun measureTextWidth(text: String): Double {
        if (text.isEmpty()) return 0.0
        val textNode = javafx.scene.text.Text(text)
        textNode.font = gc.font
        return textNode.boundsInLocal.width
    }
}
