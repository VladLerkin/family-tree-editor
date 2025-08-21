package com.pedigree.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Path;

public class Dialogs {

    public static Path chooseOpenProjectPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pedigree Project (*.ped)", "*.ped"));
        var file = fc.showOpenDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseSaveProjectPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pedigree Project (*.ped)", "*.ped"));
        var file = fc.showSaveDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseOpenGedcomPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import GEDCOM");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GEDCOM (*.ged)", "*.ged"));
        var file = fc.showOpenDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseSaveGedcomPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export GEDCOM");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GEDCOM (*.ged)", "*.ged"));
        var file = fc.showSaveDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseSaveSvgPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export SVG");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG (*.svg)", "*.svg"));
        var file = fc.showSaveDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseSaveHtmlPath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export HTML");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML (*.html)", "*.html"));
        var file = fc.showSaveDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static Path chooseSaveImagePath() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg, *.jpeg)", "*.jpg", "*.jpeg")
        );
        var file = fc.showSaveDialog(getActiveWindow());
        return file != null ? file.toPath() : null;
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.showAndWait();
    }

    private static Stage getActiveWindow() {
        for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
            if (w instanceof Stage s && s.isShowing()) return s;
        }
        return null;
    }
}
