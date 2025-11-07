package com.family.tree.editor

import com.family.tree.layout.LayoutResult
import com.family.tree.render.GraphicsContext
import com.family.tree.render.NodeMetrics
import com.family.tree.render.TreeRenderer
import com.family.tree.storage.ProjectRepository
import java.awt.geom.Point2D
import java.util.Objects

/**
 * Framework-agnostic canvas view model that manages rendering and interactions.
 * UI toolkits can call render(...) with a bound GraphicsContext.
 */
class CanvasView {

    val zoomAndPan: ZoomAndPanController = ZoomAndPanController()
    val selectionModel: SelectionModel = SelectionModel()

    var projectData: ProjectRepository.ProjectData? = null
        set(value) {
            field = value
            updateSelectionBoundsProvider()
        }

    var layout: LayoutResult? = null
        set(value) {
            field = value
            updateSelectionBoundsProvider()
        }

    var renderer: TreeRenderer? = null

    var nodeMetrics: NodeMetrics = NodeMetrics()
        set(value) {
            field = value
            updateSelectionBoundsProvider()
        }

    var commandStack: CommandStack? = null

    var dirtyCallback: Runnable? = null

    private fun markDirty() {
        com.family.tree.util.DirtyFlag.setModified()
        dirtyCallback?.run()
    }

    private fun updateSelectionBoundsProvider() {
        val layout = this.layout ?: return
        selectionModel.setBoundsProvider(object : SelectionModel.BoundsProvider {
            override fun getBounds(id: String): Rect? {
                val p: Point2D? = layout.getPosition(id)
                if (p == null) return null
                val w = nodeMetrics.getWidth(id)
                val h = nodeMetrics.getHeight(id)
                return Rect(p.x, p.y, w, h)
            }

            override fun getAllIds(): Set<String> {
                return layout.getNodeIds()
            }
        })
    }

    /**
     * Render current scene using provided GraphicsContext.
     */
    fun render(g: GraphicsContext) {
        val r: TreeRenderer = renderer ?: return
        val l = layout ?: return
        val d: ProjectRepository.ProjectData = projectData ?: return
        com.family.tree.render.RenderCompat.render(r, d, l, g, zoomAndPan.zoom, zoomAndPan.panX, zoomAndPan.panY)
    }

    /**
     * Drag-move a node to a new position (updates layout).
     */
    fun moveNode(id: String?, newX: Double, newY: Double) {
        val l = layout ?: return
        if (id == null) return
        val p = l.getPosition(id) ?: return
        l.setPosition(id, newX, newY)
        markDirty()
    }

    /**
     * Undoable node move using CommandStack if available.
     */
    fun moveNodeWithUndo(id: String?, newX: Double, newY: Double) {
        val l = layout ?: return
        if (id == null) return
        val p = l.getPosition(id) ?: return
        val cs = commandStack
        if (cs == null) {
            moveNode(id, newX, newY)
        } else {
            cs.execute(
                com.family.tree.editor.commands.MoveNodeCommand(
                    l,
                    id,
                    p.x,
                    p.y,
                    newX,
                    newY,
                    this::markDirty
                )
            )
        }
    }

    fun panBy(dx: Double, dy: Double) {
        zoomAndPan.panBy(dx, dy)
    }

    fun zoomIn() { zoomAndPan.zoomIn() }
    fun zoomOut() { zoomAndPan.zoomOut() }
    fun setZoom(level: Double) { zoomAndPan.setZoom(level) }
    
    /**
     * Zoom in or out relative to a specific point (e.g., mouse cursor).
     */
    fun zoomAt(x: Double, y: Double, zoomIn: Boolean) {
        zoomAndPan.zoomAt(x, y, zoomIn)
    }

    /**
     * Hit test: returns the id of the node whose rectangle contains the mouse point.
     * The (x,y) are in canvas coordinates; this method compensates for zoom and pan.
     */
    fun pickNearest(x: Double, y: Double, maxDist: Double): String? { // maxDist kept for API compatibility (unused)
        val l = layout ?: return null
        val lx = (x - zoomAndPan.panX) / zoomAndPan.zoom
        val ly = (y - zoomAndPan.panY) / zoomAndPan.zoom
        for (id in l.getNodeIds()) {
            // Skip family nodes for hit-testing (they are not visualized)
            val hasFamilyId = projectData?.families?.stream()?.anyMatch { f: com.family.tree.model.Family -> id == f.id } ?: false
            if (hasFamilyId) continue
            val p = l.getPosition(id) ?: continue
            val w = nodeMetrics.getWidth(id)
            val h = nodeMetrics.getHeight(id)
            if (lx >= p.x && lx <= p.x + w && ly >= p.y && ly <= p.y + h) {
                return id
            }
        }
        return null
    }

    fun select(obj: Any) {
        Objects.requireNonNull(obj)
        var newId: String? = if (obj is String) obj else null
        if (newId == null) {
            // Fallback to SelectionModel's resolution to avoid duplication of logic
            selectionModel.select(obj)
            newId = selectionModel.getSelectedIds().stream().findFirst().orElse(null)
        } else {
            // Compare with current selection to avoid redundant publish
            val current = selectionModel.getSelectedIds().stream().findFirst().orElse(null)
            if (newId == current) {
                // Already selected; no-op to prevent flicker
                return
            }
            selectionModel.select(newId)
        }
        // Publish selected id for UI components (lists, etc.)
        if (newId != null) {
            com.family.tree.ui.SelectionBus.publish(newId)
        }
    }
}
