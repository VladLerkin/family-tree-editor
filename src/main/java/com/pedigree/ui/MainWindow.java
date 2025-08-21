package com.pedigree.ui;

import com.pedigree.editor.AlignAndDistributeController;
import com.pedigree.editor.CanvasView;
import com.pedigree.layout.PedigreeLayoutEngine;
import com.pedigree.render.NodeMetrics;
import com.pedigree.render.TreeRenderer;
import com.pedigree.services.ProjectService;
import com.pedigree.services.UndoRedoService;
import com.pedigree.storage.ProjectRepository;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.*;

public class MainWindow {

    private final ProjectService projectService;
    private final UndoRedoService undoRedoService;

    private final CanvasView canvasView = new CanvasView();
    private final CanvasPane canvasPane = new CanvasPane(canvasView);
    private final IndividualsListView individualsList = new IndividualsListView();
    private final FamiliesListView familiesList = new FamiliesListView();
    private final RelationshipsListView relationshipsList = new RelationshipsListView();
    private final PropertiesInspector propertiesInspector = new PropertiesInspector();

    private final PedigreeLayoutEngine layoutEngine = new PedigreeLayoutEngine();
    private final TreeRenderer renderer = new TreeRenderer();
    private final NodeMetrics metrics = new NodeMetrics();

    private final Label statusBar = new Label("Ready");

    public MainWindow(ProjectService projectService, UndoRedoService undoRedoService) {
        this.projectService = projectService;
        this.undoRedoService = undoRedoService;

        canvasView.setDirtyCallback(this::markDirty);
        canvasPane.setOnSelectionChanged(this::onSelectionChanged);

        // List selection -> canvas selection
        individualsList.setOnSelect(id -> { if (id != null) { canvasView.select(id); canvasPane.draw(); }});
        familiesList.setOnSelect(id -> { if (id != null) { canvasView.select(id); canvasPane.draw(); }});
        relationshipsList.setOnSelect(rel -> { /* optional: select both ends */ canvasPane.draw(); });

        // Families add/edit/delete
        familiesList.setOnAdd(() -> {
            var dlg = new FamilyDialog();
            var data = projectService.getCurrentData();
            dlg.showCreate(data).ifPresent(fam -> {
                data.families.add(fam);
                // relationships: spouses to family
                if (fam.getHusbandId() != null) {
                    var r = new com.pedigree.model.Relationship();
                    r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                    r.setFromId(fam.getHusbandId());
                    r.setToId(fam.getId());
                    data.relationships.add(r);
                }
                if (fam.getWifeId() != null) {
                    var r = new com.pedigree.model.Relationship();
                    r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                    r.setFromId(fam.getWifeId());
                    r.setToId(fam.getId());
                    data.relationships.add(r);
                }
                // family to children
                for (String cid : fam.getChildrenIds()) {
                    var r = new com.pedigree.model.Relationship();
                    r.setType(com.pedigree.model.Relationship.Type.FAMILY_TO_CHILD);
                    r.setFromId(fam.getId());
                    r.setToId(cid);
                    data.relationships.add(r);
                }
                refreshAll();
                statusBar.setText("Added family");
            });
        });
        familiesList.setOnEdit(id -> {
            var data = projectService.getCurrentData();
            if (data == null) return;
            data.families.stream().filter(f -> f.getId().equals(id)).findFirst().ifPresent(fam -> {
                var dlg = new FamilyDialog();
                if (dlg.showEdit(fam, data)) {
                    // remove existing relationships for this family
                    data.relationships.removeIf(r -> id.equals(r.getFromId()) || id.equals(r.getToId()));
                    // re-add relationships based on edited family
                    if (fam.getHusbandId() != null) {
                        var r = new com.pedigree.model.Relationship();
                        r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                        r.setFromId(fam.getHusbandId());
                        r.setToId(fam.getId());
                        data.relationships.add(r);
                    }
                    if (fam.getWifeId() != null) {
                        var r = new com.pedigree.model.Relationship();
                        r.setType(com.pedigree.model.Relationship.Type.SPOUSE_TO_FAMILY);
                        r.setFromId(fam.getWifeId());
                        r.setToId(fam.getId());
                        data.relationships.add(r);
                    }
                    for (String cid : fam.getChildrenIds()) {
                        var r = new com.pedigree.model.Relationship();
                        r.setType(com.pedigree.model.Relationship.Type.FAMILY_TO_CHILD);
                        r.setFromId(fam.getId());
                        r.setToId(cid);
                        data.relationships.add(r);
                    }
                    refreshAll();
                    statusBar.setText("Edited family");
                }
            });
        });
        familiesList.setOnDelete(id -> {
            var data = projectService.getCurrentData();
            if (data == null) return;
            data.families.removeIf(f -> f.getId().equals(id));
            data.relationships.removeIf(r -> id.equals(r.getFromId()) || id.equals(r.getToId()));
            refreshAll();
            statusBar.setText("Deleted family");
        });

        // Individuals add/edit/delete
        individualsList.setOnAdd(() -> {
            var dlg = new IndividualDialog();
            dlg.showCreate().ifPresent(ind -> {
                var data2 = projectService.getCurrentData();
                data2.individuals.add(ind);
                refreshAll();
                statusBar.setText("Added individual: " + ind.getFirstName() + " " + ind.getLastName());
            });
        });
        individualsList.setOnEdit(id -> {
            var data = projectService.getCurrentData();
            if (data == null) return;
            data.individuals.stream().filter(i -> i.getId().equals(id)).findFirst().ifPresent(ind -> {
                var dlg = new IndividualDialog();
                if (dlg.showEdit(ind)) {
                    refreshAll();
                    statusBar.setText("Edited individual: " + ind.getFirstName() + " " + ind.getLastName());
                }
            });
        });
        individualsList.setOnDelete(id -> {
            var data = projectService.getCurrentData();
            if (data == null) return;
            data.individuals.removeIf(i -> i.getId().equals(id));
            data.relationships.removeIf(r -> id.equals(r.getFromId()) || id.equals(r.getToId()));
            refreshAll();
            statusBar.setText("Deleted individual");
        });
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();

        MenuBarFactory menuFactory = new MenuBarFactory(
                this::newProject,
                this::openProject,
                this::saveProject,
                this::saveProjectAs,
                this::exitApp,
                this::importGedcom,
                this::exportGedcom,
                this::exportHtml,
                this::exportSvg,
                this::exportImage,
                this::printCanvas,
                this::undo,
                this::redo,
                this::copySelection,
                this::cutSelection,
                this::pasteClipboard,
                this::deleteSelection,
                this::zoomIn,
                this::zoomOut,
                this::resetZoom,
                () -> align(AlignAndDistributeController.Alignment.TOP),
                () -> align(AlignAndDistributeController.Alignment.MIDDLE),
                () -> align(AlignAndDistributeController.Alignment.BOTTOM),
                () -> align(AlignAndDistributeController.Alignment.LEFT),
                () -> align(AlignAndDistributeController.Alignment.CENTER),
                () -> align(AlignAndDistributeController.Alignment.RIGHT),
                () -> distribute(AlignAndDistributeController.Distribution.HORIZONTAL),
                () -> distribute(AlignAndDistributeController.Distribution.VERTICAL),
                this::openQuickSearch
        );
        root.setTop(menuFactory.create());

        ToolbarFactory toolbarFactory = new ToolbarFactory(
                this::newProject,
                this::openProject,
                this::saveProject,
                this::zoomIn,
                this::zoomOut,
                this::resetZoom
        );
        root.setLeft(null);
        root.setBottom(statusBar);

        // Left: Tabbed lists
        TabPane leftTabs = new TabPane();
        leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        leftTabs.getTabs().add(new Tab("Individuals", individualsList.getView()));
        leftTabs.getTabs().add(new Tab("Families", familiesList.getView()));
        leftTabs.getTabs().add(new Tab("Relationships", relationshipsList.getView()));

        // Center / Right split
        SplitPane centerSplit = new SplitPane();
        centerSplit.setOrientation(Orientation.HORIZONTAL);
        centerSplit.getItems().addAll(leftTabs, canvasPane.getView(), propertiesInspector.getView());
        centerSplit.setDividerPositions(0.2, 0.8);

        BorderPane centerPane = new BorderPane();
        centerPane.setTop(toolbarFactory.create());
        centerPane.setCenter(centerSplit);

        root.setCenter(centerPane);

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Pedigree Chart Editor");
        stage.setScene(scene);
        stage.show();

        newProject();
    }

    private void newProject() {
        projectService.createNewProject();
        refreshAll();
        statusBar.setText("New project created");
    }

    private void openProject() {
        Path path = Dialogs.chooseOpenProjectPath();
        if (path == null) return;
        try {
            projectService.openProject(path);
            refreshAll();
            statusBar.setText("Opened: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Open Project Failed", ex.getMessage());
        }
    }

    private void saveProject() {
        try {
            if (projectService.getCurrentProjectPath() == null) {
                saveProjectAs();
                return;
            }
            projectService.saveProject();
            statusBar.setText("Saved: " + projectService.getCurrentProjectPath());
        } catch (Exception ex) {
            Dialogs.showError("Save Project Failed", ex.getMessage());
        }
    }

    private void saveProjectAs() {
        Path path = Dialogs.chooseSaveProjectPath();
        if (path == null) return;
        try {
            projectService.saveProjectAs(path);
            statusBar.setText("Saved As: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Save As Failed", ex.getMessage());
        }
    }

    private void importGedcom() {
        Path path = Dialogs.chooseOpenGedcomPath();
        if (path == null) return;
        try {
            var importer = new com.pedigree.gedcom.GedcomImporter();
            ProjectRepository.ProjectData imported = importer.importFromFile(path);
            projectService.getCurrentData().individuals.addAll(imported.individuals);
            projectService.getCurrentData().families.addAll(imported.families);
            projectService.getCurrentData().relationships.addAll(imported.relationships);
            refreshAll();
            statusBar.setText("Imported GEDCOM: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Import GEDCOM Failed", ex.getMessage());
        }
    }

    private void exportGedcom() {
        Path path = Dialogs.chooseSaveGedcomPath();
        if (path == null) return;
        try {
            var exporter = new com.pedigree.gedcom.GedcomExporter();
            exporter.exportToFile(projectService.getCurrentData(), path);
            statusBar.setText("Exported GEDCOM: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Export GEDCOM Failed", ex.getMessage());
        }
    }

    private void exportHtml() {
        var data = projectService.getCurrentData();
        var layout = canvasView.getLayout();
        if (data == null || layout == null) {
            Dialogs.showError("Export HTML", "Nothing to export.");
            return;
        }
        var path = Dialogs.chooseSaveHtmlPath();
        if (path == null) return;
        try {
            new com.pedigree.export.HtmlExporter().exportToFile(data, layout, new NodeMetrics(), path);
            statusBar.setText("Exported HTML: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Export HTML Failed", ex.getMessage());
        }
    }

    private void exportSvg() {
        var data = projectService.getCurrentData();
        var layout = canvasView.getLayout();
        if (data == null || layout == null) {
            Dialogs.showError("Export SVG", "Nothing to export.");
            return;
        }
        var path = Dialogs.chooseSaveSvgPath();
        if (path == null) return;
        try {
            new com.pedigree.export.SvgExporter().exportToFile(data, layout, new NodeMetrics(), path);
            statusBar.setText("Exported SVG: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Export SVG Failed", ex.getMessage());
        }
    }

    private void exportImage() {
        var data = projectService.getCurrentData();
        var layout = canvasView.getLayout();
        if (data == null || layout == null) {
            Dialogs.showError("Export Image", "Nothing to export.");
            return;
        }
        var path = Dialogs.chooseSaveImagePath();
        if (path == null) return;
        try {
            new com.pedigree.export.ImageExporter().exportToFile(data, layout, new NodeMetrics(), path);
            statusBar.setText("Exported Image: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Export Image Failed", ex.getMessage());
        }
    }

    private void printCanvas() {
        try {
            com.pedigree.print.PrintService.printNode(canvasPane.getView());
        } catch (Exception ex) {
            Dialogs.showError("Print Failed", ex.getMessage());
        }
    }

    private void exitApp() {
        // Close current project and refresh UI instead of exiting the app
        projectService.closeProject();
        canvasView.setProjectData(null);
        canvasView.setLayout(null);
        refreshAll();
        statusBar.setText("Project closed");
    }

    private void undo() {
        undoRedoService.undo();
        canvasPane.draw();
    }

    private void redo() {
        undoRedoService.redo();
        canvasPane.draw();
    }

    private void zoomIn() {
        canvasView.zoomIn();
        persistViewport();
        canvasPane.draw();
    }

    private void zoomOut() {
        canvasView.zoomOut();
        persistViewport();
        canvasPane.draw();
    }

    private void resetZoom() {
        canvasView.getZoomAndPan().resetView();
        persistViewport();
        canvasPane.draw();
    }

    private void copySelection() {
        // Stub for now: selection content managed by clipboard adapter in future
        statusBar.setText("Copy not implemented (placeholder)");
    }

    private void cutSelection() {
        statusBar.setText("Cut not implemented (placeholder)");
    }

    private void pasteClipboard() {
        statusBar.setText("Paste not implemented (placeholder)");
    }

    private void deleteSelection() {
        var data = projectService.getCurrentData();
        if (data == null) return;
        Set<String> ids = canvasView.getSelectionModel().getSelectedIds();
        if (ids.isEmpty()) return;

        // Remove from individuals and families, and cleanup relationships
        data.individuals.removeIf(i -> ids.contains(i.getId()));
        data.families.removeIf(f -> ids.contains(f.getId()));
        data.relationships.removeIf(r -> ids.contains(r.getFromId()) || ids.contains(r.getToId()));
        // Remove layout positions
        // LayoutResult is recomputed; if persisting to ProjectLayout, also remove there

        refreshAll();
        statusBar.setText("Deleted selected objects");
    }

    private void align(AlignAndDistributeController.Alignment mode) {
        var layout = canvasView == null ? null : getLayoutIdsAndAccessor();
        if (layout == null) return;
        var controller = new AlignAndDistributeController(canvasView.getSelectionModel(), layout.accessor());
        controller.align(mode);
        canvasPane.draw();
        markDirty();
    }

    private void distribute(AlignAndDistributeController.Distribution mode) {
        var layout = getLayoutIdsAndAccessor();
        if (layout == null) return;
        var controller = new AlignAndDistributeController(canvasView.getSelectionModel(), layout.accessor());
        controller.distribute(mode);
        canvasPane.draw();
        markDirty();
    }

    private record Accessor(AlignAndDistributeController.PositionAccessor accessor) {}

    private Accessor getLayoutIdsAndAccessor() {
        var layoutResult = canvasView.getLayout();
        if (projectService.getCurrentData() == null || layoutResult == null) return null;

        var lr = new Accessor(new AlignAndDistributeController.PositionAccessor() {
            @Override public double getX(String id) { return bounds(id)[0]; }
            @Override public double getY(String id) { return bounds(id)[1]; }
            @Override public double getWidth(String id) { return metrics.getWidth(id); }
            @Override public double getHeight(String id) { return metrics.getHeight(id); }
            @Override public void setX(String id, double x) { moveNode(id, x, getY(id)); }
            @Override public void setY(String id, double y) { moveNode(id, getX(id), y); }

            private double[] bounds(String id) {
                var p = layoutResult.getPosition(id);
                if (p == null) return new double[]{0, 0, 0, 0};
                return new double[]{p.getX(), p.getY(), metrics.getWidth(id), metrics.getHeight(id)};
            }

            private void moveNode(String id, double x, double y) {
                canvasView.moveNode(id, x, y);
            }
        });
        return lr;
    }

    private void openQuickSearch() {
        var data = projectService.getCurrentData();
        if (data == null) return;
        new QuickSearchDialog(data, id -> {
            canvasView.select(id);
            canvasPane.draw();
        }).show();
    }

    private void refreshAll() {
        ProjectRepository.ProjectData data = projectService.getCurrentData();
        canvasView.setProjectData(data);
        canvasView.setRenderer(renderer);
        canvasView.setLayout(layoutEngine.computeLayout(data));
        canvasPane.draw();

        individualsList.setData(data);
        familiesList.setData(data);
        relationshipsList.setData(data);
        propertiesInspector.setProjectContext(data, projectService.getCurrentProjectPath());
        propertiesInspector.setSelection(Set.of());
    }

    private void onSelectionChanged(Set<String> selectedIds) {
        propertiesInspector.setSelection(selectedIds);
    }

    private void markDirty() {
        if (projectService.getCurrentMeta() != null) {
            undoRedoService.bindProjectMetadata(projectService.getCurrentMeta());
        }
    }

    private void persistViewport() {
        var layout = projectService.getCurrentLayout();
        if (layout != null) {
            layout.setZoom(canvasView.getZoomAndPan().getZoom());
            layout.setViewOriginX(canvasView.getZoomAndPan().getPanX());
            layout.setViewOriginY(canvasView.getZoomAndPan().getPanY());
        }
    }
}
