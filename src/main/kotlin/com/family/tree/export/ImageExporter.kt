package com.family.tree.export

import com.family.tree.layout.LayoutResult
import com.family.tree.render.NodeMetrics
import com.family.tree.render.TreeRenderer
import com.family.tree.storage.ProjectRepository
import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Path

class ImageExporter {

    @Throws(IOException::class)
    fun exportToFile(data: ProjectRepository.ProjectData?, layout: LayoutResult?, metrics: NodeMetrics, path: Path) {
        val fileName = path.fileName.toString().lowercase()
        val fmt = if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) "jpg" else "png"
        exportToFile(data, layout, metrics, path, fmt)
    }

    @Throws(IOException::class)
    fun exportToFile(data: ProjectRepository.ProjectData?, layout: LayoutResult?, metrics: NodeMetrics, path: Path, format: String) {
        if (data == null || layout == null) return
        val b = computeBounds(layout, metrics)
        val margin = 20.0
        val width = (b.maxX - b.minX) + margin * 2.0
        val height = (b.maxY - b.minY) + margin * 2.0
        val panX = -b.minX + margin
        val panY = -b.minY + margin

        val canvas = Canvas(kotlin.math.max(1.0, width), kotlin.math.max(1.0, height))
        val jfxGc = canvas.graphicsContext2D
        val gc = com.family.tree.ui.JavaFxGraphicsContext(jfxGc)

        val renderer = TreeRenderer(metrics)
        renderer.render(data, layout, gc, 1.0, panX, panY)

        val img = canvas.snapshot(null, null)
        val bi: BufferedImage = SwingFXUtils.fromFXImage(img, null)
        javax.imageio.ImageIO.write(bi, format, path.toFile())
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
}
