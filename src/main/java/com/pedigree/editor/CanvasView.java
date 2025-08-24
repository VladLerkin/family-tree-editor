package com.pedigree.editor;

import com.pedigree.layout.LayoutResult;
import com.pedigree.render.GraphicsContext;
import com.pedigree.render.NodeMetrics;
import com.pedigree.render.TreeRenderer;
import com.pedigree.storage.ProjectRepository;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Set;

/**
 * Framework-agnostic canvas view model that manages rendering and interactions.
 * UI toolkits can call render(...) with a bound GraphicsContext.
 */
public class CanvasView {

    private final ZoomAndPanController zoomAndPan = new ZoomAndPanController();
    private final SelectionModel selectionModel = new SelectionModel();

    private ProjectRepository.ProjectData data;
    private LayoutResult layout;
    private TreeRenderer renderer;
    private NodeMetrics metrics = new NodeMetrics();
    private CommandStack commandStack;
    private Runnable dirtyCallback;

    public ZoomAndPanController getZoomAndPan() { return zoomAndPan; }
    public SelectionModel getSelectionModel() { return selectionModel; }

    public void setProjectData(ProjectRepository.ProjectData data) {
        this.data = data;
        updateSelectionBoundsProvider();
    }

    public ProjectRepository.ProjectData getProjectData() {
        return data;
    }

    public void setLayout(LayoutResult layout) {
        this.layout = layout;
        updateSelectionBoundsProvider();
    }

    public void setRenderer(TreeRenderer renderer) {
        this.renderer = renderer;
    }

    public LayoutResult getLayout() {
        return layout;
    }

    public void setNodeMetrics(NodeMetrics metrics) {
        this.metrics = metrics != null ? metrics : new NodeMetrics();
        updateSelectionBoundsProvider();
    }

    public void setCommandStack(CommandStack commandStack) {
        this.commandStack = commandStack;
    }

    public void setDirtyCallback(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
    }

    private void markDirty() {
        com.pedigree.util.DirtyFlag.setModified();
        if (dirtyCallback != null) dirtyCallback.run();
    }

    private void updateSelectionBoundsProvider() {
        if (layout == null) return;
        selectionModel.setBoundsProvider(new SelectionModel.BoundsProvider() {
            @Override
            public Rect getBounds(String id) {
                Point2D p = layout.getPosition(id);
                if (p == null) return null;
                double w = metrics.getWidth(id);
                double h = metrics.getHeight(id);
                return new Rect(p.getX(), p.getY(), w, h);
            }

            @Override
            public Set<String> getAllIds() {
                return layout.getNodeIds();
            }
        });
    }

    /**
     * Render current scene using provided GraphicsContext.
     */
    public void render(GraphicsContext g) {
        if (renderer == null || layout == null || data == null) return;
        renderer.render(data, layout, g, zoomAndPan.getZoom(), zoomAndPan.getPanX(), zoomAndPan.getPanY());
    }

    /**
     * Drag-move a node to a new position (updates layout).
     */
    public void moveNode(String id, double newX, double newY) {
        if (layout == null || id == null) return;
        Point2D p = layout.getPosition(id);
        if (p == null) return;
        layout.setPosition(id, newX, newY);
        markDirty();
    }

    /**
     * Undoable node move using CommandStack if available.
     */
    public void moveNodeWithUndo(String id, double newX, double newY) {
        if (layout == null || id == null) return;
        Point2D p = layout.getPosition(id);
        if (p == null) return;
        if (commandStack == null) {
            moveNode(id, newX, newY);
        } else {
            commandStack.execute(new com.pedigree.editor.commands.MoveNodeCommand(layout, id, p.getX(), p.getY(), newX, newY, this::markDirty));
        }
    }

    public void panBy(double dx, double dy) {
        zoomAndPan.panBy(dx, dy);
    }

    public void zoomIn() { zoomAndPan.zoomIn(); }
    public void zoomOut() { zoomAndPan.zoomOut(); }
    public void setZoom(double level) { zoomAndPan.setZoom(level); }

    /**
     * Hit test: returns the id of the node whose rectangle contains the mouse point.
     * The (x,y) are in canvas coordinates; this method compensates for zoom and pan.
     */
    public String pickNearest(double x, double y, double maxDist) {
        if (layout == null) return null;
        double lx = (x - zoomAndPan.getPanX()) / zoomAndPan.getZoom();
        double ly = (y - zoomAndPan.getPanY()) / zoomAndPan.getZoom();
        for (String id : layout.getNodeIds()) {
            // Skip family nodes for hit-testing (they are not visualized)
            if (data != null && data.families != null && data.families.stream().anyMatch(f -> id.equals(f.getId()))) {
                continue;
            }
            Point2D p = layout.getPosition(id);
            if (p == null) continue;
            double w = metrics.getWidth(id);
            double h = metrics.getHeight(id);
            if (lx >= p.getX() && lx <= p.getX() + w && ly >= p.getY() && ly <= p.getY() + h) {
                return id;
            }
        }
        return null;
    }

    public void select(Object obj) {
        Objects.requireNonNull(obj);
        String newId = (obj instanceof String s) ? s : null;
        if (newId == null) {
            // Fallback to SelectionModel's resolution to avoid duplication of logic
            selectionModel.select(obj);
            newId = selectionModel.getSelectedIds().stream().findFirst().orElse(null);
        } else {
            // Compare with current selection to avoid redundant publish
            String current = selectionModel.getSelectedIds().stream().findFirst().orElse(null);
            if (newId.equals(current)) {
                // Already selected; no-op to prevent flicker
                return;
            }
            selectionModel.select(newId);
        }
        // Publish selected id for UI components (lists, etc.)
        if (newId != null) {
            com.pedigree.ui.SelectionBus.publish(newId);
        }
    }
}
