package com.family.tree.export

import com.family.tree.layout.LayoutResult
import com.family.tree.render.GraphicsContext
import com.family.tree.render.NodeMetrics
import com.family.tree.render.TreeRenderer
import com.family.tree.storage.ProjectRepository
import java.awt.geom.Point2D
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class SvgExporter {

    @Throws(IOException::class)
    fun exportToFile(
        data: ProjectRepository.ProjectData?,
        layout: LayoutResult?,
        metrics: NodeMetrics,
        path: Path
    ) {
        val svg = exportToString(data, layout, metrics)
        Files.write(path, svg.toByteArray(StandardCharsets.UTF_8))
    }

    fun exportToString(
        data: ProjectRepository.ProjectData?,
        layout: LayoutResult?,
        metrics: NodeMetrics
    ): String {
        if (data == null || layout == null) return ""
        val b = computeBounds(layout, metrics)
        val margin = 20.0
        val panX = -b.minX + margin
        val panY = -b.minY + margin

        val g = SvgGraphicsContext()
        val renderer = TreeRenderer(metrics)
        renderer.render(data, layout, g, 1.0, panX, panY)

        val width = (b.maxX - b.minX) + margin * 2.0
        val height = (b.maxY - b.minY) + margin * 2.0

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"")
            .append(width).append("\" height=\"").append(height)
            .append("\" viewBox=\"0 0 ").append(width).append(" ").append(height).append("\">\n")
        sb.append(g.content)
        sb.append("</svg>\n")
        return sb.toString()
    }

    private data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

    private fun computeBounds(layout: LayoutResult, metrics: NodeMetrics): Bounds {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        val ids: Set<String> = layout.nodeIds()
        for (id in ids) {
            val p: Point2D? = layout.getPosition(id)
            if (p == null) continue
            val x = p.x
            val y = p.y
            val w = metrics.getWidth(id)
            val h = metrics.getHeight(id)
            minX = kotlin.math.min(minX, x)
            minY = kotlin.math.min(minY, y)
            maxX = kotlin.math.max(maxX, x + w)
            maxY = kotlin.math.max(maxY, y + h)
        }
        if (ids.isEmpty()) {
            minX = 0.0; minY = 0.0; maxX = 1.0; maxY = 1.0
        }
        return Bounds(minX, minY, maxX, maxY)
    }

    private class SvgGraphicsContext : GraphicsContext {
        private val sb = StringBuilder()
        private var stroke: String = "rgb(0,0,0)"
        private var fill: String = "none"
        private var alphaStroke: Double = 1.0
        private var alphaFill: Double = 1.0
        private var lineWidth: Double = 1.0
        private var fontSize: Double = 12.0

        val content: String
            get() = sb.toString()

        override fun setStrokeColor(r: Int, g: Int, b: Int, a: Double) {
            this.stroke = "rgb($r,$g,$b)"
            this.alphaStroke = a
        }

        override fun setFillColor(r: Int, g: Int, b: Int, a: Double) {
            this.fill = "rgb($r,$g,$b)"
            this.alphaFill = a
        }

        override fun setLineWidth(w: Double) {
            this.lineWidth = w
        }

        override fun drawLine(x1: Double, y1: Double, x2: Double, y2: Double) {
            sb.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                .append("\" stroke=\"").append(stroke).append("\" stroke-opacity=\"").append(alphaStroke)
                .append("\" stroke-width=\"").append(lineWidth).append("\" />\n")
        }

        override fun fillRect(x: Double, y: Double, w: Double, h: Double) {
            sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w).append("\" height=\"").append(h)
                .append("\" fill=\"").append(fill).append("\" fill-opacity=\"").append(alphaFill)
                .append("\" stroke=\"none\" />\n")
        }

        override fun drawRect(x: Double, y: Double, w: Double, h: Double) {
            sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(w).append("\" height=\"").append(h)
                .append("\" fill=\"none\" stroke=\"").append(stroke).append("\" stroke-opacity=\"").append(alphaStroke)
                .append("\" stroke-width=\"").append(lineWidth).append("\" />\n")
        }

        override fun setFontSize(size: Double) {
            this.fontSize = if (size > 0) size else 12.0
        }

        override fun drawText(text: String, x: Double, y: Double) {
            sb.append("<text x=\"").append(x).append("\" y=\"").append(y)
                .append("\" fill=\"").append(stroke).append("\" fill-opacity=\"").append(alphaStroke)
                .append("\" font-family=\"Sans-Serif\" font-size=\"").append(fontSize).append("\">")
                .append(escape(text)).append("</text>\n")
        }

        override fun measureTextWidth(text: String): Double {
            if (text.isEmpty()) return 0.0
            return text.length * fontSize * 0.6
        }

        private fun escape(s: String): String {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        }
    }
}
