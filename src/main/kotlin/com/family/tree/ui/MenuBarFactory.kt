package com.family.tree.ui

import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Supplier

class MenuBarFactory(
    private val onNew: Runnable?,
    private val onOpen: Runnable?,
    private val onSave: Runnable?,
    private val onSaveAs: Runnable?,
    private val onExit: Runnable?,

    private val onImportGedcom: Runnable?,
    private val onImportRel: Runnable?,
    private val onExportGedcom: Runnable?,
    private val onExportHtml: Runnable?,
    private val onExportSvg: Runnable?,
    private val onExportImage: Runnable?,
    private val onPrint: Runnable?,

    private val onUndo: Runnable?,
    private val onRedo: Runnable?,
    private val onCopy: Runnable?,
    private val onCut: Runnable?,
    private val onPaste: Runnable?,
    private val onDelete: Runnable?,

    private val onZoomIn: Runnable?,
    private val onZoomOut: Runnable?,
    private val onResetZoom: Runnable?,

    private val onAlignTop: Runnable?,
    private val onAlignMiddle: Runnable?,
    private val onAlignBottom: Runnable?,
    private val onAlignLeft: Runnable?,
    private val onAlignCenter: Runnable?,
    private val onAlignRight: Runnable?,
    private val onDistributeH: Runnable?,
    private val onDistributeV: Runnable?,

    private val onQuickSearch: Runnable?,
    private val onDebugExportRelSection: Runnable?,
    private val onManageSources: Runnable?,

    private val onAbout: Runnable?,

    private val recentSupplier: Supplier<List<Path>>?,
    private val onOpenRecent: Consumer<Path>?
) {

    fun create(): MenuBar {
        val mb = MenuBar()

        // File
        val file = Menu("File")
        file.items.addAll(
            item("New", "Shortcut+N", onNew),
            item("Open...", "Shortcut+O", onOpen),
            openRecentMenu(),
            item("Save", "Shortcut+S", onSave),
            item("Save As...", null, onSaveAs),
            importMenu(),
            exportMenu(),
            item("Print...", "Shortcut+P", onPrint),
            item("Close Project", null) {
                if (!com.family.tree.util.DirtyFlag.isModified()) {
                    onExit?.run(); return@item
                }
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Unsaved Changes"
                alert.headerText = "You have unsaved changes."
                val saveBtn = ButtonType("Save and Close", ButtonBar.ButtonData.YES)
                val dontSaveBtn = ButtonType("Close Without Saving", ButtonBar.ButtonData.NO)
                val cancelBtn = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                alert.buttonTypes.setAll(saveBtn, dontSaveBtn, cancelBtn)
                val res = alert.showAndWait().orElse(cancelBtn)
                when (res) {
                    saveBtn -> { onSave?.run(); onExit?.run() }
                    dontSaveBtn -> { onExit?.run() }
                    else -> {}
                }
            },
            item("Exit", "Shortcut+Q") {
                if (!com.family.tree.util.DirtyFlag.isModified()) {
                    onExit?.run()
                    javafx.application.Platform.exit()
                    return@item
                }
                val alert = Alert(Alert.AlertType.CONFIRMATION)
                alert.title = "Unsaved Changes"
                alert.headerText = "You have unsaved changes."
                val saveBtn = ButtonType("Save and Exit", ButtonBar.ButtonData.YES)
                val dontSaveBtn = ButtonType("Exit Without Saving", ButtonBar.ButtonData.NO)
                val cancelBtn = ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                alert.buttonTypes.setAll(saveBtn, dontSaveBtn, cancelBtn)
                val res = alert.showAndWait().orElse(cancelBtn)
                when (res) {
                    saveBtn -> { onSave?.run(); onExit?.run(); javafx.application.Platform.exit() }
                    dontSaveBtn -> { onExit?.run(); javafx.application.Platform.exit() }
                    else -> {}
                }
            }
        )

        // Edit
        val edit = Menu("Edit")
        edit.items.addAll(
            item("Undo", "Shortcut+Z", onUndo),
            item("Redo", "Shortcut+Y", onRedo),
            item("Copy", "Shortcut+C", onCopy),
            item("Cut", "Shortcut+X", onCut),
            item("Paste", "Shortcut+V", onPaste),
            item("Delete", "Delete", onDelete)
        )

        // View
        val view = Menu("View")
        view.items.addAll(
            item("Zoom In", "Shortcut+Plus", onZoomIn),
            item("Zoom Out", "Shortcut+Minus", onZoomOut),
            item("Reset Zoom", "Shortcut+0", onResetZoom),
            alignMenu(),
            distributeMenu()
        )

        // Tools
        val tools = Menu("Tools")
        tools.items.addAll(
            item("Quick Search...", "Shortcut+F", onQuickSearch),
            item("Manage Sources...", null, onManageSources),
            item("Debug: Export REL Section...", null, onDebugExportRelSection)
        )

        // Help
        val help = Menu("Help")
        help.items.addAll(
            item("About", null, onAbout)
        )

        mb.menus.addAll(file, edit, view, tools, help)
        return mb
    }

    private fun openRecentMenu(): Menu {
        val recent = Menu("Open Recent")
        populateRecent(recent)
        recent.setOnShowing { populateRecent(recent) }
        return recent
    }

    private fun populateRecent(recent: Menu) {
        recent.items.clear()
        val paths = try {
            recentSupplier?.get() ?: listOf()
        } catch (_: Exception) { listOf() }
        if (paths.isEmpty()) {
            val empty = MenuItem("(Empty)")
            empty.isDisable = true
            recent.items.add(empty)
            return
        }
        for (p in paths) {
            if (p == null) continue
            val mi = MenuItem(p.toString())
            mi.setOnAction { onOpenRecent?.accept(p) }
            recent.items.add(mi)
        }
    }

    private fun importMenu(): Menu {
        val importMenu = Menu("Import")
        importMenu.items.addAll(
            item("From GEDCOM...", null, onImportGedcom),
            item("From REL...", null, onImportRel)
        )
        return importMenu
    }

    private fun exportMenu(): Menu {
        val export = Menu("Export")
        export.items.addAll(
            item("GEDCOM 5.5...", null, onExportGedcom),
            item("HTML...", null, onExportHtml),
            item("SVG...", null, onExportSvg),
            item("Image...", null, onExportImage)
        )
        return export
    }

    private fun alignMenu(): Menu {
        val align = Menu("Align")
        align.items.addAll(
            item("Top", null, onAlignTop),
            item("Middle", null, onAlignMiddle),
            item("Bottom", null, onAlignBottom),
            item("Left", null, onAlignLeft),
            item("Center", null, onAlignCenter),
            item("Right", null, onAlignRight)
        )
        return align
    }

    private fun distributeMenu(): Menu {
        val distribute = Menu("Distribute")
        distribute.items.addAll(
            item("Horizontally", null, onDistributeH),
            item("Vertically", null, onDistributeV)
        )
        return distribute
    }

    private fun item(text: String, accel: String?, action: Runnable?): MenuItem {
        val mi = MenuItem(text)
        if (accel != null) mi.accelerator = KeyCombination.keyCombination(accel)
        mi.setOnAction { action?.run() }
        return mi
    }

    private inline fun item(text: String, accel: String?, crossinline action: () -> Unit): MenuItem {
        val mi = MenuItem(text)
        if (accel != null) mi.accelerator = KeyCombination.keyCombination(accel)
        mi.setOnAction { action() }
        return mi
    }
}
