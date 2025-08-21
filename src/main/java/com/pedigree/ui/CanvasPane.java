package com.pedigree.ui;

import com.pedigree.editor.CanvasView;
import com.pedigree.render.GraphicsContext;
import com.pedigree.model.Individual;
import com.pedigree.model.Family;
import com.pedigree.render.RenderHighlightState;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;

import java.util.Set;
import java.util.function.Consumer;

public class CanvasPane {
    private final StackPane root = new StackPane();
    private final Canvas canvas = new Canvas(800, 600);
    private final CanvasView view;
    private Consumer<Set<String>> onSelectionChanged;

    private double lastDragX;
    private double lastDragY;
    private boolean panning;

    public CanvasPane(CanvasView view) {
        this.view = view;
        root.getChildren().add(canvas);

        root.widthProperty().addListener((obs, o, n) -> {
            canvas.setWidth(root.getWidth());
            draw();
        });
        root.heightProperty().addListener((obs, o, n) -> {
            canvas.setHeight(root.getHeight());
            draw();
        });

        canvas.setOnScroll(e -> {
            if (e.getDeltaY() > 0) view.zoomIn(); else view.zoomOut();
            draw();
        });

        canvas.setOnMousePressed(e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
            panning = e.getButton() == MouseButton.MIDDLE || e.isSecondaryButtonDown();
        });

        canvas.setOnMouseDragged(e -> {
            if (panning) {
                double dx = e.getX() - lastDragX;
                double dy = e.getY() - lastDragY;
                view.panBy(dx, dy);
                lastDragX = e.getX();
                lastDragY = e.getY();
                draw();
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (panning) return;
            String id = view.pickNearest(e.getX(), e.getY(), 30);
            if (id == null) return;

            // Double-click opens edit dialog
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                view.select(id);
                if (onSelectionChanged != null) {
                    onSelectionChanged.accept(view.getSelectionModel().getSelectedIds());
                }
                var data = view.getProjectData();
                if (data != null) {
                    // Try Individual first
                    java.util.Optional<Individual> indOpt = data.individuals.stream()
                            .filter(i -> i.getId().equals(id)).findFirst();
                    boolean saved = false;
                    if (indOpt.isPresent()) {
                        var dlg = new IndividualDialog();
                        saved = dlg.showEdit(indOpt.get());
                    } else {
                        // Try Family
                        java.util.Optional<Family> famOpt = data.families.stream()
                                .filter(f -> f.getId().equals(id)).findFirst();
                        if (famOpt.isPresent()) {
                            var dlg = new FamilyDialog();
                            saved = dlg.showEdit(famOpt.get(), data);
                        }
                    }
                    if (saved) {
                        // Notify and redraw after edits
                        if (onSelectionChanged != null) {
                            onSelectionChanged.accept(view.getSelectionModel().getSelectedIds());
                        }
                        draw();
                    }
                }
                return;
            }

            // Single click: just select and redraw
            view.select(id);
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(view.getSelectionModel().getSelectedIds());
            }
            draw();
        });
    }

    public StackPane getView() {
        return root;
    }

    public void setOnSelectionChanged(Consumer<Set<String>> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
    }

    /**
     * Programmatically select a node by id and redraw with highlight.
     */
    public void selectAndHighlight(String id) {
        if (id == null) return;
        view.select(id);
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(view.getSelectionModel().getSelectedIds());
        }
        draw();
    }

    public void draw() {
        javafx.scene.canvas.GraphicsContext jfxGc = canvas.getGraphicsContext2D();
        jfxGc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        // Pass current selection to renderer for highlight
        RenderHighlightState.setSelectedIds(view.getSelectionModel().getSelectedIds());
        // Pass current selection to renderer for highlight
        RenderHighlightState.setSelectedIds(view.getSelectionModel().getSelectedIds());
        GraphicsContext g = new JavaFxGraphicsContext(jfxGc);
        view.render(g);
    }
}
