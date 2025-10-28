package com.family.tree.ui

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.io.RelRepository
import com.family.tree.core.layout.SimpleTreeLayout
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import androidx.compose.ui.geometry.Offset

actual object DesktopActions {
    private val relRepo = RelRepository()

    actual fun openPed(onLoaded: (com.family.tree.core.io.LoadedProject?) -> Unit) {
        println("[DEBUG_LOG] openPed: Dialog opening...")
        val fd = FileDialog(null as Frame?, "Open Project", FileDialog.LOAD)
        fd.file = "*.ped"
        fd.isVisible = true
        val dir = fd.directory
        val file = fd.file
        println("[DEBUG_LOG] openPed: dir=$dir, file=$file")
        if (dir == null || file == null) {
            println("[DEBUG_LOG] openPed: User cancelled or no file selected")
            return onLoaded(null)
        }
        val selected = File(dir, file)
        println("[DEBUG_LOG] openPed: Selected file: ${selected.absolutePath}, exists=${selected.exists()}")
        try {
            val bytes = selected.readBytes()
            println("[DEBUG_LOG] openPed: Read ${bytes.size} bytes from file")
            
            val loaded = relRepo.read(bytes)
            println("[DEBUG_LOG] openPed: Successfully parsed LoadedProject - individuals=${loaded.data.individuals.size}, families=${loaded.data.families.size}")
            onLoaded(loaded)
        } catch (e: Exception) {
            println("[DEBUG_LOG] openPed: ERROR during read/parse:")
            e.printStackTrace()
            onLoaded(null)
        }
    }

    actual fun savePed(data: ProjectData): Boolean {
        val fd = FileDialog(null as Frame?, "Save Project", FileDialog.SAVE)
        fd.file = "*.ped"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".ped")) file else "$file.ped")
        return try {
            val meta = ProjectMetadata() // TODO: track actual metadata
            val bytes = relRepo.write(data, layout = null, meta = meta)
            out.writeBytes(bytes)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual fun importRel(onLoaded: (com.family.tree.core.io.LoadedProject?) -> Unit) {
        println("[DEBUG_LOG] importRel: Dialog opening...")
        val fd = FileDialog(null as Frame?, "Import .rel", FileDialog.LOAD)
        fd.file = "*.rel"
        fd.setFilenameFilter { _, name -> name.endsWith(".rel", ignoreCase = true) }
        fd.isVisible = true
        val dir = fd.directory
        val file = fd.file
        println("[DEBUG_LOG] importRel: dir=$dir, file=$file")
        if (dir == null || file == null) {
            println("[DEBUG_LOG] importRel: User cancelled or no file selected")
            return onLoaded(null)
        }
        val selected = File(dir, file)
        println("[DEBUG_LOG] importRel: Selected file: ${selected.absolutePath}, exists=${selected.exists()}")
        try {
            val bytes = selected.readBytes()
            println("[DEBUG_LOG] importRel: Read ${bytes.size} bytes from file")
            
            // Detect format: binary "Relatives" format
            val isBinaryRel = bytes.size >= 9 && 
                              String(bytes.copyOfRange(0, 9), Charsets.UTF_8) == "Relatives"
            
            println("[DEBUG_LOG] importRel: Format detection - isBinaryRel=$isBinaryRel")
            
            if (isBinaryRel) {
                println("[DEBUG_LOG] importRel: Binary 'Relatives' format detected - using RelImporter")
                
                // Use RelImporter to parse the binary .rel file
                val importer = com.family.tree.core.io.RelImporter()
                val loaded = importer.importFromBytes(bytes)
                println("[DEBUG_LOG] importRel: Successfully imported - individuals=${loaded.data.individuals.size}, families=${loaded.data.families.size}")
                onLoaded(loaded)
                return
            }
            
            // If it's not binary, it might be a misnamed .ped file - show error
            java.awt.EventQueue.invokeLater {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "This file does not appear to be a valid legacy .rel file.\n\n" +
                    "If this is a project file, please rename it to .ped and use 'Open' instead of 'Import .rel'.",
                    "Invalid File Format",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
            onLoaded(null)
        } catch (e: Exception) {
            println("[DEBUG_LOG] importRel: ERROR during read/parse:")
            e.printStackTrace()
            
            // Show error dialog
            java.awt.EventQueue.invokeLater {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "Failed to import .rel file:\n${e.message}\n\nPlease check the file format.",
                    "Import Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                )
            }
            onLoaded(null)
        }
    }

    actual fun exportSvg(project: ProjectData, scale: Float, pan: Offset): Boolean {
        val fd = FileDialog(null as Frame?, "Export SVG", FileDialog.SAVE)
        fd.file = "export.svg"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".svg", ignoreCase = true)) file else "$file.svg")
        return writeSvg(out, project, scale, pan, fit = false, margins = 32f, background = false, lineWidth = 2f)
    }

    actual fun exportSvgFit(project: ProjectData): Boolean {
        val fd = FileDialog(null as Frame?, "Export SVG (Fit)", FileDialog.SAVE)
        fd.file = "export.svg"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".svg", ignoreCase = true)) file else "$file.svg")
        return writeSvg(out, project, scale = 1f, pan = Offset.Zero, fit = true, margins = 32f, background = false, lineWidth = 2f)
    }

    actual fun exportSvgWithOptions(
        project: ProjectData,
        scale: Float,
        pan: Offset,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean {
        val fd = FileDialog(null as Frame?, "Export SVG (Options)", FileDialog.SAVE)
        fd.file = "export.svg"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".svg", ignoreCase = true)) file else "$file.svg")
        return writeSvg(out, project, scale, pan, fit = false, margins = margins, background = background, lineWidth = lineWidth)
    }

    actual fun exportSvgFitWithOptions(
        project: ProjectData,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean {
        val fd = FileDialog(null as Frame?, "Export SVG (Fit + Options)", FileDialog.SAVE)
        fd.file = "export.svg"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".svg", ignoreCase = true)) file else "$file.svg")
        return writeSvg(out, project, scale = 1f, pan = Offset.Zero, fit = true, margins = margins, background = background, lineWidth = lineWidth)
    }

    actual fun exportPng(project: ProjectData, scale: Float, pan: Offset): Boolean {
        val fd = FileDialog(null as Frame?, "Export PNG", FileDialog.SAVE)
        fd.file = "export.png"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".png", ignoreCase = true)) file else "$file.png")
        // TODO: Implement raster export; stub for now
        return false
    }

    actual fun exportPngFit(project: ProjectData): Boolean {
        val fd = FileDialog(null as Frame?, "Export PNG (Fit)", FileDialog.SAVE)
        fd.file = "export.png"
        fd.isVisible = true
        val dir = fd.directory ?: return false
        val file = fd.file ?: return false
        val out = File(dir, if (file.endsWith(".png", ignoreCase = true)) file else "$file.png")
        // TODO: Implement raster export; stub for now
        return false
    }

    private fun writeSvg(
        out: File,
        project: ProjectData,
        scale: Float,
        pan: Offset,
        fit: Boolean,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean {
        val positions = SimpleTreeLayout.layout(project.individuals, project.families)
        val nodeW = 132f
        val nodeH = 56f
        val sb = StringBuilder()

        if (positions.isEmpty()) {
            return try {
                Files.write(out.toPath(), "<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>".toByteArray(StandardCharsets.UTF_8))
                true
            } catch (_: Exception) { false }
        }

        val xs = positions.values.map { it.x }
        val ys = positions.values.map { it.y }
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f

        val contentW = (maxX - minX + nodeW)
        val contentH = (maxY - minY + nodeH)

        val svgW: Float
        val svgH: Float
        val translateX: Float
        val translateY: Float
        val scaleStr: String
        if (fit) {
            svgW = contentW + 2 * margins
            svgH = contentH + 2 * margins
            translateX = -minX + margins
            translateY = -minY + margins
            scaleStr = "1"
        } else {
            svgW = contentW * scale + kotlin.math.abs(pan.x) + 2 * margins
            svgH = contentH * scale + kotlin.math.abs(pan.y) + 2 * margins
            translateX = pan.x + margins
            translateY = pan.y + margins
            scaleStr = scale.toString()
        }

        sb.append("""
            <svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"${svgW}\" height=\"${svgH}\">
        """.trimIndent())
        if (background) {
            sb.append("<rect x=\"0\" y=\"0\" width=\"${svgW}\" height=\"${svgH}\" fill=\"#FFFFFF\" />\n")
        }
        sb.append("  <g transform=\"translate(${translateX},${translateY}) scale(${scaleStr})\">\n")

        // Edges (JavaFX-like): marriage bar + orthogonal routes from bar to child top
        data class RectF(val x: Float, val y: Float, val w: Float, val h: Float) {
            val left get() = x
            val right get() = x + w
            val top get() = y
            val bottom get() = y + h
            val centerX get() = x + w / 2f
            val centerY get() = y + h / 2f
        }
        fun rectForId(id: com.family.tree.core.model.IndividualId): RectF? {
            val p = positions[id] ?: return null
            return RectF(p.x, p.y, nodeW, nodeH)
        }
        project.families.forEach { fam ->
            val husband = fam.husbandId?.let { rectForId(it) }
            val wife = fam.wifeId?.let { rectForId(it) }
            if (husband != null && wife != null) {
                val left = kotlin.math.min(husband.centerX, wife.centerX)
                val right = kotlin.math.max(husband.centerX, wife.centerX)
                val yBar = kotlin.math.max(husband.bottom, wife.bottom) + 8f
                val barMidX = (left + right) / 2f
                val barMidY = yBar + 1f // half of 2px bar height
                // marriage bar
                sb.append("<line x1=\"$left\" y1=\"$barMidY\" x2=\"$right\" y2=\"$barMidY\" stroke=\"#607D8B\" stroke-width=\"${lineWidth}\" />\n")

                // children connectors (V→H→short V up to top edge)
                fam.childrenIds.forEach { cid ->
                    val c = rectForId(cid) ?: return@forEach
                    val gap = 6f
                    val topY = c.top - gap
                    // vertical from bar mid to above child
                    sb.append("<line x1=\"$barMidX\" y1=\"$barMidY\" x2=\"$barMidX\" y2=\"$topY\" stroke=\"#607D8B\" stroke-width=\"${lineWidth}\" />\n")
                    // horizontal to child.centerX
                    sb.append("<line x1=\"$barMidX\" y1=\"$topY\" x2=\"${c.centerX}\" y2=\"$topY\" stroke=\"#607D8B\" stroke-width=\"${lineWidth}\" />\n")
                    // short vertical down to the child top edge
                    sb.append("<line x1=\"${c.centerX}\" y1=\"$topY\" x2=\"${c.centerX}\" y2=\"${c.top}\" stroke=\"#607D8B\" stroke-width=\"${lineWidth}\" />\n")
                }
            }
        }

        // Nodes
        project.individuals.forEach { ind ->
            val pos = positions[ind.id] ?: return@forEach
            sb.append("<rect x=\"${pos.x}\" y=\"${pos.y}\" width=\"$nodeW\" height=\"$nodeH\" fill=\"#FAFAFA\" stroke=\"#B0BEC5\" stroke-width=\"${lineWidth}\" />\n")
            val textX = pos.x + nodeW / 2
            val textY = pos.y + nodeH / 2 + 4
            val name = (ind.firstName + " " + ind.lastName).trim()
            sb.append("<text x=\"$textX\" y=\"$textY\" font-size=\"12\" text-anchor=\"middle\" fill=\"#263238\">${escapeXml(name)}</text>\n")
        }

        sb.append("  </g>\n</svg>\n")

        return try {
            Files.write(out.toPath(), sb.toString().toByteArray(StandardCharsets.UTF_8))
            true
        } catch (_: Exception) { false }
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
