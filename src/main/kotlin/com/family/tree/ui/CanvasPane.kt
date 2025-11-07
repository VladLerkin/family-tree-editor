package com.family.tree.ui

import com.family.tree.editor.CanvasView
import com.family.tree.render.GraphicsContext
import com.family.tree.render.RenderHighlightState
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import java.util.function.Consumer

/**
 * Kotlin implementation of the main canvas pane for rendering and interaction.
 * Replaces the legacy Java version.
 */
class CanvasPane(private val view: CanvasView) {
    private val root = StackPane()
    private val canvas = Canvas(800.0, 600.0)
    private var onSelectionChanged: Consumer<Set<String>>? = null

    private var lastDragX = 0.0
    private var lastDragY = 0.0
    private var panning = false

    // Dragging a node
    private var draggingNodeId: String? = null
    private var dragOffsetLX = 0.0 // offset in layout coords from node origin to mouse
    private var dragOffsetLY = 0.0
    private var wasDragging = false // true if we moved a node during last gesture
    private var suppressNextClick = false // to prevent click after drag

    init {
        // Set mint green background matching the screenshot
        root.style = "-fx-background-color: #D5E8D4;"
        root.children.add(canvas)

        root.widthProperty().addListener { _, _, _ ->
            canvas.width = root.width
            draw()
        }
        root.heightProperty().addListener { _, _, _ ->
            canvas.height = root.height
            draw()
        }

        canvas.setOnScroll { e ->
            view.zoomAt(e.x, e.y, e.deltaY > 0)
            draw()
        }

        canvas.setOnMousePressed { e ->
            lastDragX = e.x
            lastDragY = e.y
            wasDragging = false
            suppressNextClick = false

            if (e.button == MouseButton.PRIMARY) {
                // Primary button: either drag a node (if clicked on a node) or pan the canvas (if empty space)
                val id = view.pickNearest(e.x, e.y, 30.0)
                if (id != null) {
                    panning = false
                    draggingNodeId = id
                    // Compute offset in layout coords between mouse and node origin
                    val zoom = view.zoomAndPan.zoom
                    val panX = view.zoomAndPan.panX
                    val panY = view.zoomAndPan.panY
                    val p = view.layout?.getPosition(id)
                    if (p != null) {
                        val mouseLX = (e.x - panX) / zoom
                        val mouseLY = (e.y - panY) / zoom
                        dragOffsetLX = mouseLX - p.x
                        dragOffsetLY = mouseLY - p.y
                    } else {
                        dragOffsetLX = 0.0
                        dragOffsetLY = 0.0
                    }
                    // Select the node on press
                    view.select(id)
                    onSelectionChanged?.accept(view.selectionModel.getSelectedIds())
                } else {
                    // No node under cursor: start panning with left button
                    panning = true
                    draggingNodeId = null
                }
            } else {
                // Middle or secondary button pans as before
                panning = e.button == MouseButton.MIDDLE || e.isSecondaryButtonDown
                draggingNodeId = null
            }
        }

        canvas.setOnMouseDragged { e ->
            if (panning) {
                val dx = e.x - lastDragX
                val dy = e.y - lastDragY
                view.panBy(dx, dy)
                lastDragX = e.x
                lastDragY = e.y
                draw()
            } else if (draggingNodeId != null) {
                val zoom = view.zoomAndPan.zoom
                val panX = view.zoomAndPan.panX
                val panY = view.zoomAndPan.panY
                val mouseLX = (e.x - panX) / zoom
                val mouseLY = (e.y - panY) / zoom
                val newX = mouseLX - dragOffsetLX
                val newY = mouseLY - dragOffsetLY
                view.moveNode(draggingNodeId!!, newX, newY)
                wasDragging = true
                draw()
            }
        }

        canvas.setOnMouseReleased { _ ->
            // Stop dragging
            if (wasDragging) {
                suppressNextClick = true
            }
            draggingNodeId = null
        }

        canvas.setOnMouseClicked { e ->
            if (panning) return@setOnMouseClicked
            // Suppress synthetic click after a drag gesture
            if (suppressNextClick) { suppressNextClick = false; return@setOnMouseClicked }
            val id = view.pickNearest(e.x, e.y, 30.0) ?: return@setOnMouseClicked

            // Double-click opens edit dialog
            if (e.button == MouseButton.PRIMARY && e.clickCount == 2) {
                view.select(id)
                onSelectionChanged?.accept(view.selectionModel.getSelectedIds())
                val data = view.projectData
                if (data != null) {
                    // Try Individual first
                    val indOpt = data.individuals.stream().filter { i -> i.id == id }.findFirst()
                    var saved = false
                    if (indOpt.isPresent) {
                        val dlg = IndividualDialog()
                        saved = dlg.showEdit(indOpt.get())
                    } else {
                        // Try Family
                        val famOpt = data.families.stream().filter { f -> f.id == id }.findFirst()
                        if (famOpt.isPresent) {
                            val dlg = FamilyDialog()
                            saved = dlg.showEdit(famOpt.get(), data)
                        }
                    }
                    if (saved) {
                        // Notify and redraw after edits
                        onSelectionChanged?.accept(view.selectionModel.getSelectedIds())
                        draw()
                    }
                }
                return@setOnMouseClicked
            }

            // Single click: just select and redraw
            view.select(id)
            onSelectionChanged?.accept(view.selectionModel.getSelectedIds())
            draw()
        }
    }

    fun getView(): StackPane = root

    fun setOnSelectionChanged(onSelectionChanged: Consumer<Set<String>>?) {
        this.onSelectionChanged = onSelectionChanged
    }

    /**
     * Programmatically select a node by id and redraw with highlight.
     */
    fun selectAndHighlight(id: String?) {
        if (id == null) return
        view.select(id)
        onSelectionChanged?.accept(view.selectionModel.getSelectedIds())
        draw()
    }

    /**
     * Programmatically select multiple nodes and redraw with highlight.
     * Publishes the first id (if any) to SelectionBus so lists stay in sync.
     */
    fun selectAndHighlight(ids: Set<String>?) {
        if (ids == null || ids.isEmpty()) return
        val sel = view.selectionModel
        sel.clear()
        // Preserve order roughly by iterating as provided
        var first: String? = null
        for (s in ids) {
            if (s.isBlank()) continue
            if (first == null) first = s
            sel.addToSelection(s)
        }
        if (first != null) {
            SelectionBus.publish(first)
        }
        onSelectionChanged?.accept(sel.getSelectedIds())
        draw()
    }

    fun draw() {
        val jfxGc = canvas.graphicsContext2D
        jfxGc.clearRect(0.0, 0.0, canvas.width, canvas.height)
        // Pass current selection to renderer for highlight
        RenderHighlightState.setSelectedIds(view.selectionModel.getSelectedIds())
        val g: GraphicsContext = JavaFxGraphicsContext(jfxGc)
        view.render(g)
    }
}
