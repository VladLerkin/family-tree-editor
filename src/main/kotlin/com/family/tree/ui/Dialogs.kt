package com.family.tree.ui

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.nio.file.Path

/**
 * Kotlin version of Dialogs utility. Preserves Java API via @JvmStatic methods.
 */
object Dialogs {
    @JvmStatic
    fun chooseOpenProjectPath(): Path? {
        val fc = FileChooser()
        fc.title = "Open Project"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("Pedigree Project (*.ped)", "*.ped"))
        val file = fc.showOpenDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseSaveProjectPath(): Path? {
        val fc = FileChooser()
        fc.title = "Save Project"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("Pedigree Project (*.ped)", "*.ped"))
        val file = fc.showSaveDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseOpenGedcomPath(): Path? {
        val fc = FileChooser()
        fc.title = "Import GEDCOM"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("GEDCOM (*.ged)", "*.ged"))
        val file = fc.showOpenDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseOpenRelPath(): Path? {
        val fc = FileChooser()
        fc.title = "Import REL"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("Relatives (*.rel)", "*.rel"))
        val file = fc.showOpenDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseSaveGedcomPath(): Path? {
        val fc = FileChooser()
        fc.title = "Export GEDCOM"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("GEDCOM (*.ged)", "*.ged"))
        val file = fc.showSaveDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseSaveSvgPath(): Path? {
        val fc = FileChooser()
        fc.title = "Export SVG"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("SVG (*.svg)", "*.svg"))
        val file = fc.showSaveDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseSaveHtmlPath(): Path? {
        val fc = FileChooser()
        fc.title = "Export HTML"
        fc.extensionFilters.add(FileChooser.ExtensionFilter("HTML (*.html)", "*.html"))
        val file = fc.showSaveDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun chooseSaveImagePath(): Path? {
        val fc = FileChooser()
        fc.title = "Export Image"
        fc.extensionFilters.addAll(
            FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
            FileChooser.ExtensionFilter("JPEG (*.jpg, *.jpeg)", "*.jpg", "*.jpeg")
        )
        val file = fc.showSaveDialog(activeWindow)
        return file?.toPath()
    }

    @JvmStatic
    fun showError(title: String, message: String?) {
        val alert = Alert(Alert.AlertType.ERROR, message ?: "", ButtonType.CLOSE)
        alert.title = title
        alert.showAndWait()
    }

    private val activeWindow: Stage?
        get() {
            for (w in javafx.stage.Window.getWindows()) {
                if (w is Stage && w.isShowing) return w
            }
            return null
        }
}
