package com.pedigree.ui;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.input.KeyCombination;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MenuBarFactory {

    private final Runnable onNew;
    private final Runnable onOpen;
    private final Runnable onSave;
    private final Runnable onSaveAs;
    private final Runnable onExit;

    private final Runnable onImportGedcom;
    private final Runnable onImportRel;
    private final Runnable onExportGedcom;
    private final Runnable onExportHtml;
    private final Runnable onExportSvg;
    private final Runnable onExportImage;
    private final Runnable onPrint;

    private final Runnable onUndo;
    private final Runnable onRedo;
    private final Runnable onCopy;
    private final Runnable onCut;
    private final Runnable onPaste;
    private final Runnable onDelete;

    private final Runnable onZoomIn;
    private final Runnable onZoomOut;
    private final Runnable onResetZoom;

    private final Runnable onAlignTop;
    private final Runnable onAlignMiddle;
    private final Runnable onAlignBottom;
    private final Runnable onAlignLeft;
    private final Runnable onAlignCenter;
    private final Runnable onAlignRight;
    private final Runnable onDistributeH;
    private final Runnable onDistributeV;

    private final Runnable onQuickSearch;
    private final Runnable onDebugExportRelSection;
    private final Runnable onManageSources;

    private final Runnable onAbout;

    // Recent files support
    private final Supplier<List<Path>> recentSupplier;
    private final Consumer<Path> onOpenRecent;

    public MenuBarFactory(
            Runnable onNew, Runnable onOpen, Runnable onSave, Runnable onSaveAs, Runnable onExit,
            Runnable onImportGedcom, Runnable onImportRel, Runnable onExportGedcom, Runnable onExportHtml, Runnable onExportSvg, Runnable onExportImage, Runnable onPrint,
            Runnable onUndo, Runnable onRedo, Runnable onCopy, Runnable onCut, Runnable onPaste, Runnable onDelete,
            Runnable onZoomIn, Runnable onZoomOut, Runnable onResetZoom,
            Runnable onAlignTop, Runnable onAlignMiddle, Runnable onAlignBottom,
            Runnable onAlignLeft, Runnable onAlignCenter, Runnable onAlignRight,
            Runnable onDistributeH, Runnable onDistributeV,
            Runnable onQuickSearch,
            Runnable onDebugExportRelSection,
            Runnable onManageSources,
            Runnable onAbout,
            Supplier<List<Path>> recentSupplier,
            Consumer<Path> onOpenRecent
    ) {
        this.onNew = onNew;
        this.onOpen = onOpen;
        this.onSave = onSave;
        this.onSaveAs = onSaveAs;
        this.onExit = onExit;

        this.onImportGedcom = onImportGedcom;
        this.onImportRel = onImportRel;
        this.onExportGedcom = onExportGedcom;
        this.onExportHtml = onExportHtml;
        this.onExportSvg = onExportSvg;
        this.onExportImage = onExportImage;
        this.onPrint = onPrint;

        this.onUndo = onUndo;
        this.onRedo = onRedo;
        this.onCopy = onCopy;
        this.onCut = onCut;
        this.onPaste = onPaste;
        this.onDelete = onDelete;

        this.onZoomIn = onZoomIn;
        this.onZoomOut = onZoomOut;
        this.onResetZoom = onResetZoom;

        this.onAlignTop = onAlignTop;
        this.onAlignMiddle = onAlignMiddle;
        this.onAlignBottom = onAlignBottom;
        this.onAlignLeft = onAlignLeft;
        this.onAlignCenter = onAlignCenter;
        this.onAlignRight = onAlignRight;
        this.onDistributeH = onDistributeH;
        this.onDistributeV = onDistributeV;

        this.onQuickSearch = onQuickSearch;
        this.onDebugExportRelSection = onDebugExportRelSection;
        this.onManageSources = onManageSources;

        this.onAbout = onAbout;

        this.recentSupplier = recentSupplier;
        this.onOpenRecent = onOpenRecent;
    }

    public MenuBar create() {
        MenuBar mb = new MenuBar();

        // File
        Menu file = new Menu("File");
        file.getItems().addAll(
                item("New", "Shortcut+N", onNew),
                item("Open...", "Shortcut+O", onOpen),
                openRecentMenu(),
                item("Save", "Shortcut+S", onSave),
                item("Save As...", null, onSaveAs),
                importMenu(),
                exportMenu(),
                item("Print...", "Shortcut+P", onPrint),
                item("Close Project", null, () -> {
                    if (!com.pedigree.util.DirtyFlag.isModified()) {
                        if (onExit != null) onExit.run();
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Unsaved Changes");
                    alert.setHeaderText("You have unsaved changes.");
                    ButtonType saveBtn = new ButtonType("Save and Close", ButtonBar.ButtonData.YES);
                    ButtonType dontSaveBtn = new ButtonType("Close Without Saving", ButtonBar.ButtonData.NO);
                    ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);
                    ButtonType res = alert.showAndWait().orElse(cancelBtn);

                    if (res == saveBtn) {
                        if (onSave != null) onSave.run();
                        if (onExit != null) onExit.run();
                    } else if (res == dontSaveBtn) {
                        if (onExit != null) onExit.run();
                    } else {
                        // Cancel: do nothing
                    }
                }),
                item("Exit", "Shortcut+Q", () -> {
                    if (!com.pedigree.util.DirtyFlag.isModified()) {
                        if (onExit != null) onExit.run();
                        javafx.application.Platform.exit();
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Unsaved Changes");
                    alert.setHeaderText("You have unsaved changes.");
                    //alert.setContentText("Save changes before exiting the application?");
                    ButtonType saveBtn = new ButtonType("Save and Exit", ButtonBar.ButtonData.YES);
                    ButtonType dontSaveBtn = new ButtonType("Exit Without Saving", ButtonBar.ButtonData.NO);
                    ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(saveBtn, dontSaveBtn, cancelBtn);
                    ButtonType res = alert.showAndWait().orElse(cancelBtn);

                    if (res == saveBtn) {
                        if (onSave != null) onSave.run();
                        if (onExit != null) onExit.run();
                        javafx.application.Platform.exit();
                    } else if (res == dontSaveBtn) {
                        if (onExit != null) onExit.run();
                        javafx.application.Platform.exit();
                    } else {
                        // Cancel: do nothing
                    }
                })
        );

        // Edit
        Menu edit = new Menu("Edit");
        edit.getItems().addAll(
                item("Undo", "Shortcut+Z", onUndo),
                item("Redo", "Shortcut+Y", onRedo),
                item("Copy", "Shortcut+C", onCopy),
                item("Cut", "Shortcut+X", onCut),
                item("Paste", "Shortcut+V", onPaste),
                item("Delete", "Delete", onDelete)
        );

        // View
        Menu view = new Menu("View");
        view.getItems().addAll(
                item("Zoom In", "Shortcut+Plus", onZoomIn),
                item("Zoom Out", "Shortcut+Minus", onZoomOut),
                item("Reset Zoom", "Shortcut+0", onResetZoom),
                alignMenu(),
                distributeMenu()
        );

        // Tools
        Menu tools = new Menu("Tools");
        tools.getItems().addAll(
                item("Quick Search...", "Shortcut+F", onQuickSearch),
                item("Manage Sources...", null, onManageSources),
                item("Debug: Export REL Section...", null, onDebugExportRelSection)
        );

        // Help
        Menu help = new Menu("Help");
        help.getItems().addAll(
                item("About", null, onAbout)
        );

        mb.getMenus().addAll(file, edit, view, tools, help);
        return mb;
    }

    private Menu openRecentMenu() {
        Menu recent = new Menu("Open Recent");
        // Pre-populate to ensure submenu can open even before onShowing fires
        populateRecent(recent);
        recent.setOnShowing(e -> populateRecent(recent));
        return recent;
    }

    private void populateRecent(Menu recent) {
        recent.getItems().clear();
        List<Path> paths;
        try {
            paths = recentSupplier != null ? recentSupplier.get() : List.of();
        } catch (Exception ex) {
            paths = List.of();
        }
        if (paths == null || paths.isEmpty()) {
            MenuItem empty = new MenuItem("(Empty)");
            empty.setDisable(true);
            recent.getItems().add(empty);
            return;
        }
        for (Path p : paths) {
            if (p == null) continue;
            MenuItem mi = new MenuItem(p.toString());
            mi.setOnAction(a -> { if (onOpenRecent != null) onOpenRecent.accept(p); });
            recent.getItems().add(mi);
        }
    }

    private Menu importMenu() {
        Menu importMenu = new Menu("Import");
        importMenu.getItems().addAll(
                item("From GEDCOM...", null, onImportGedcom),
                item("From REL...", null, onImportRel)
        );
        return importMenu;
    }

    private Menu exportMenu() {
        Menu export = new Menu("Export");
        export.getItems().addAll(
                item("GEDCOM 5.5...", null, onExportGedcom),
                item("HTML...", null, onExportHtml),
                item("SVG...", null, onExportSvg),
                item("Image...", null, onExportImage)
        );
        return export;
    }

    private Menu alignMenu() {
        Menu align = new Menu("Align");
        align.getItems().addAll(
                item("Top", null, onAlignTop),
                item("Middle", null, onAlignMiddle),
                item("Bottom", null, onAlignBottom),
                item("Left", null, onAlignLeft),
                item("Center", null, onAlignCenter),
                item("Right", null, onAlignRight)
        );
        return align;
    }

    private Menu distributeMenu() {
        Menu distribute = new Menu("Distribute");
        distribute.getItems().addAll(
                item("Horizontally", null, onDistributeH),
                item("Vertically", null, onDistributeV)
        );
        return distribute;
    }

    private static MenuItem item(String text, String accel, Runnable action) {
        MenuItem mi = new MenuItem(text);
        if (accel != null) {
            mi.setAccelerator(KeyCombination.keyCombination(accel));
        }
        mi.setOnAction(e -> { if (action != null) action.run(); });
        return mi;
    }
}
