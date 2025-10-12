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
    private final com.pedigree.render.TextAwareNodeMetrics metrics = new com.pedigree.render.TextAwareNodeMetrics();
    private final TreeRenderer renderer = new TreeRenderer(metrics);

    private final Label statusBar = new Label("Ready");

    public MainWindow(ProjectService projectService, UndoRedoService undoRedoService) {
        this.projectService = projectService;
        this.undoRedoService = undoRedoService;

        canvasView.setDirtyCallback(this::markDirty);
        canvasPane.setOnSelectionChanged(this::onSelectionChanged);

        // List selection -> canvas selection
        individualsList.setOnSelect(id -> { if (id != null) { canvasView.select(id); canvasPane.draw(); }});
        familiesList.setOnSelect(familyId -> {
            if (familyId == null) return;
            var data = projectService.getCurrentData();
            if (data == null || data.families == null) return;
            // Find the family and collect all member ids (husband, wife, children)
            data.families.stream().filter(f -> familyId.equals(f.getId())).findFirst().ifPresent(fam -> {
                java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
                if (fam.getHusbandId() != null && !fam.getHusbandId().isBlank()) ids.add(fam.getHusbandId());
                if (fam.getWifeId() != null && !fam.getWifeId().isBlank()) ids.add(fam.getWifeId());
                if (fam.getChildrenIds() != null) {
                    for (String cid : fam.getChildrenIds()) {
                        if (cid != null && !cid.isBlank()) ids.add(cid);
                    }
                }
                // Highlight all family members on the canvas and publish first for UI sync
                canvasPane.selectAndHighlight(ids);
                // Update properties inspector to show the selected family's own properties (tags/notes/media)
                propertiesInspector.setSelection(java.util.Set.of(familyId));
            });
        });
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
                this::importRel,
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
                this::openQuickSearch,
                this::debugExportRelSection,
                this::showAbout,
                projectService::getRecentProjects,
                this::openProject
        );
        root.setTop(menuFactory.create());

        ToolbarFactory toolbarFactory = new ToolbarFactory(
                this::zoomIn,
                this::zoomOut
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
        stage.setTitle("Family Tree Editor");
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
        openProject(path);
    }

    private void openProject(Path path) {
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
            // Persist current viewport and node positions into ProjectLayout prior to saving
            persistViewport();
            persistNodePositions();
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
            // Persist current viewport and node positions into ProjectLayout prior to saving
            persistViewport();
            persistNodePositions();
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
            // Ensure a project exists before importing
            if (projectService.getCurrentData() == null) {
                projectService.createNewProject();
            }
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

    private void importRel() {
        Path path = Dialogs.chooseOpenRelPath();
        if (path == null) return;
        try {
            // Ensure a project exists before importing
            if (projectService.getCurrentData() == null) {
                projectService.createNewProject();
            }
            var importer = new com.pedigree.rel.RelImporter();
            ProjectRepository.ProjectData imported = importer.importFromFileWithLayout(path, projectService.getCurrentLayout());
            projectService.getCurrentData().individuals.addAll(imported.individuals);
            projectService.getCurrentData().families.addAll(imported.families);
            projectService.getCurrentData().relationships.addAll(imported.relationships);
            refreshAll();
            statusBar.setText("Imported REL: " + path);
        } catch (Exception ex) {
            Dialogs.showError("Import REL Failed", ex.getMessage());
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

    private void showAbout() {
        AboutDialog.show();
    }

    private void debugExportRelSection() {
        java.nio.file.Path path = Dialogs.chooseOpenRelPath();
        if (path == null) return;
        javafx.scene.control.TextInputDialog idDialog = new javafx.scene.control.TextInputDialog("P123");
        idDialog.setTitle("Debug: Export REL Section");
        idDialog.setHeaderText("Введите идентификатор персоны (например, P266)");
        idDialog.setContentText("P-id:");
        java.util.Optional<String> res = idDialog.showAndWait();
        if (res.isEmpty()) return;
        String pid = res.get().trim().toUpperCase(java.util.Locale.ROOT);
        if (!pid.matches("P\\d{1,5}")) {
            Dialogs.showError("Debug: Export REL Section", "Неверный формат идентификатора. Ожидается P и число, например P266.");
            return;
        }
        try {
            byte[] raw = java.nio.file.Files.readAllBytes(path);
            byte[] cleaned = dropZeroBytes(raw);
            String text = new String(cleaned, java.nio.charset.StandardCharsets.UTF_8);
            int startIdx = text.indexOf(pid);
            if (startIdx < 0) {
                Dialogs.showError("Debug: Export REL Section", "Не найден раздел " + pid + " в выбранном файле.");
                return;
            }
            int searchFrom = startIdx + pid.length();
            java.util.regex.Pattern secRe = java.util.regex.Pattern.compile("(P\\d{1,5}|F\\d{1,5})");
            java.util.regex.Matcher m = secRe.matcher(text);
            int endIdx = text.length();
            while (m.find()) {
                int idx = m.start(1);
                if (idx > startIdx) { endIdx = idx; break; }
            }
            String section = text.substring(startIdx, Math.max(startIdx, endIdx));
            // sanitize: keep CR/LF
            section = section.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "");

            // Additionally parse multimedia blocks (OBJE) and summarize FORM/TITL/FILE
            StringBuilder out = new StringBuilder();
            out.append(section);
            try {
                java.util.regex.Pattern OBJE_BLOCK_RE = java.util.regex.Pattern.compile("(?i)OBJE\\s*([\\s\\S]+?)\\s*(?=(OBJE|NOTE|NOTES|SOUR|SEX|BIRT|DEAT|FAMC|FAMS|SUBM|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL);
                java.util.regex.Pattern FORM_INNER_RE = java.util.regex.Pattern.compile("(?i)FORM\\s*([^\\r\\n\\s]+)");
                java.util.regex.Pattern TITL_INNER_RE = java.util.regex.Pattern.compile("(?i)TITL\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL);
                java.util.regex.Pattern FILE_INNER_RE = java.util.regex.Pattern.compile("(?i)FILE\\s*([\\s\\S]+?)\\s*(?=(FORM|FILE|TITL|OBJE|NOTE|NOTES|SOUR|P\\d+|F\\d+|_X|_Y)|$)", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher mOb = OBJE_BLOCK_RE.matcher(section);
                int count = 0;
                StringBuilder mediaSummary = new StringBuilder();
                while (mOb.find()) {
                    String chunk = mOb.group(1);
                    if (chunk == null) continue;
                    String form = null, titl = null, file = null;
                    java.util.regex.Matcher mf = FORM_INNER_RE.matcher(chunk);
                    if (mf.find()) form = mf.group(1);
                    java.util.regex.Matcher mt = TITL_INNER_RE.matcher(chunk);
                    if (mt.find()) titl = mt.group(1);
                    java.util.regex.Matcher mfile = FILE_INNER_RE.matcher(chunk);
                    if (mfile.find()) file = mfile.group(1);
                    count++;
                    mediaSummary.append("#").append(count).append(": ");
                    if (titl != null) mediaSummary.append("TITL=\"").append(titl.replaceAll("[\\\\p{Cntrl}&&[^\\r\\n]]", "").trim()).append("\" ");
                    if (form != null) mediaSummary.append("FORM=").append(form.replaceAll("[\\\\p{Cntrl}]", "").trim()).append(" ");
                    if (file != null) mediaSummary.append("FILE=").append(file.replaceAll("[\\\\p{Cntrl}]", "").trim());
                    mediaSummary.append("\n");
                }
                out.append("\n\n--- Parsed Media (OBJE) ---\n");
                if (count == 0) {
                    out.append("(none found)\n");
                } else {
                    out.append(mediaSummary);
                    out.append("Total: ").append(count).append("\n");
                }

                // Also look for standalone FILE/TITL/FORM tokens outside OBJE
                try {
                    String withoutObje = OBJE_BLOCK_RE.matcher(section).replaceAll(" ");
                    java.util.regex.Matcher mf2 = FILE_INNER_RE.matcher(withoutObje);
                    int fileCount = 0;
                    StringBuilder stand = new StringBuilder();
                    while (mf2.find()) {
                        String file = mf2.group(1);
                        if (file != null) file = file.replaceAll("[\\p{Cntrl}]", "").trim();
                        // Backward small window for TITL
                        int start = Math.max(0, mf2.start() - 200);
                        String win = withoutObje.substring(start, mf2.start());
                        String titl = null;
                        java.util.regex.Matcher mt2 = TITL_INNER_RE.matcher(win);
                        if (mt2.find()) titl = mt2.group(1);
                        if (titl != null) titl = titl.replaceAll("[\\p{Cntrl}&&[^\\r\\n]]", "").trim();
                        // Forward small window for FORM
                        int end = Math.min(withoutObje.length(), mf2.end() + 100);
                        String winF = withoutObje.substring(mf2.end(), end);
                        String form = null;
                        java.util.regex.Matcher mfForm = FORM_INNER_RE.matcher(winF);
                        if (mfForm.find()) form = mfForm.group(1);
                        if (form != null) form = form.replaceAll("[\\p{Cntrl}]", "").trim();

                        fileCount++;
                        stand.append("#").append(fileCount).append(": ");
                        if (titl != null && !titl.isBlank()) stand.append("TITL=\"").append(titl).append("\" ");
                        if (form != null && !form.isBlank()) stand.append("FORM=").append(form).append(" ");
                        if (file != null && !file.isBlank()) stand.append("FILE=").append(file);
                        stand.append("\n");
                    }
                    out.append("\n--- Standalone Media Tokens ---\n");
                    if (fileCount == 0) {
                        out.append("(none found)\n");
                    } else {
                        out.append(stand);
                        out.append("Total FILE tokens: ").append(fileCount).append("\n");
                    }
                } catch (Throwable ignore2) { }
            } catch (Throwable ignore) { }

            // Show in a scrollable dialog
            javafx.scene.control.TextArea ta = new javafx.scene.control.TextArea(out.toString());
            ta.setEditable(false);
            ta.setWrapText(false);
            ta.setPrefColumnCount(80);
            ta.setPrefRowCount(30);
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("REL Section: " + pid);
            alert.setHeaderText("Очищенный текстовый фрагмент секции " + pid + " (сводка медиа)");
            alert.getDialogPane().setContent(ta);
            alert.showAndWait();
        } catch (Exception ex) {
            Dialogs.showError("Debug: Export REL Section", ex.getMessage());
        }
    }

    private static byte[] dropZeroBytes(byte[] in) {
        if (in == null) return new byte[0];
        byte[] out = new byte[in.length];
        int p = 0;
        for (byte b : in) if (b != 0) out[p++] = b;
        return java.util.Arrays.copyOf(out, p);
    }

    private void refreshAll() {
        ProjectRepository.ProjectData data = projectService.getCurrentData();
        canvasView.setProjectData(data);
        canvasView.setRenderer(renderer);
        canvasView.setNodeMetrics(metrics);
        metrics.setData(data);

        // Compute base layout
        var computed = layoutEngine.computeLayout(data);

        // Apply persisted positions and viewport if available
        var persisted = projectService.getCurrentLayout();
        if (persisted != null && persisted.getNodePositions() != null) {
            boolean centers = false;
            try { centers = persisted.isPositionsAreCenters(); } catch (Throwable ignore) {}
            for (var e : persisted.getNodePositions().entrySet()) {
                var np = e.getValue();
                if (np != null) {
                    double x = np.x;
                    double y = np.y;
                    if (centers) {
                        double w = metrics.getWidth(e.getKey());
                        double h = metrics.getHeight(e.getKey());
                        x -= w / 2.0;
                        y -= h / 2.0;
                    }
                    computed.setPosition(e.getKey(), x, y);
                }
            }
            if (centers) {
                // Optionally scale coordinates to reduce overlap if source nodes were smaller
                var map = persisted.getNodePositions();
                java.util.List<Double> xs = new java.util.ArrayList<>();
                for (var v : map.values()) if (v != null) xs.add(v.x);
                double minDx = Double.POSITIVE_INFINITY;
                for (int i = 0; i < xs.size(); i++) {
                    for (int j = i + 1; j < xs.size(); j++) {
                        double dx = Math.abs(xs.get(i) - xs.get(j));
                        if (dx > 0 && dx < minDx) minDx = dx;
                    }
                }
                double scale = 1.0;
                double targetWidth = 0.0;
                // Use average width as target
                for (var id : map.keySet()) targetWidth += metrics.getWidth(id);
                if (!map.isEmpty()) targetWidth /= map.size();
                if (minDx != Double.POSITIVE_INFINITY && minDx > 0.0 && targetWidth > 0.0) {
                    // gentle extra space 5%
                    scale = Math.max(1.0, Math.min(3.0, (targetWidth * 1.05) / minDx));
                }

                // Convert stored positions to top-left and apply scale so we don't adjust again next refresh
                for (var e : map.entrySet()) {
                    var np = e.getValue();
                    if (np != null) {
                        double w = metrics.getWidth(e.getKey());
                        double h = metrics.getHeight(e.getKey());
                        np.x = np.x * scale - w / 2.0;
                        np.y = np.y * scale - h / 2.0;
                    }
                }
                // Re-apply with converted positions
                for (var e : map.entrySet()) {
                    var np = e.getValue();
                    if (np != null) computed.setPosition(e.getKey(), np.x, np.y);
                }
                persisted.setPositionsAreCenters(false);
            }
            // Apply zoom and pan (open at minimum zoom unless a meaningful viewport was persisted)
            boolean hasViewport = persisted.getZoom() != 1.0
                    || persisted.getViewOriginX() != 0.0
                    || persisted.getViewOriginY() != 0.0;
            if (hasViewport) {
                canvasView.setZoom(persisted.getZoom());
                double dx = persisted.getViewOriginX() - canvasView.getZoomAndPan().getPanX();
                double dy = persisted.getViewOriginY() - canvasView.getZoomAndPan().getPanY();
                if (dx != 0 || dy != 0) canvasView.panBy(dx, dy);
            } else {
                // No meaningful persisted viewport: start at minimal zoom
                canvasView.setZoom(canvasView.getZoomAndPan().getMinZoom());
            }
        }

        canvasView.setLayout(computed);
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

    private void persistNodePositions() {
        var layoutResult = canvasView.getLayout();
        var persisted = projectService.getCurrentLayout();
        if (layoutResult == null || persisted == null) return;
        var map = persisted.getNodePositions();
        map.clear();
        for (String id : layoutResult.getNodeIds()) {
            var p = layoutResult.getPosition(id);
            if (p == null) continue;
            var np = new com.pedigree.model.ProjectLayout.NodePos();
            np.x = p.getX();
            np.y = p.getY();
            map.put(id, np);
        }
    }
}
