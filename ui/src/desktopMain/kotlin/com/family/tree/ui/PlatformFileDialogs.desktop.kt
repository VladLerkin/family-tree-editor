package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files

@Composable
actual fun PlatformFileDialogs(
    showOpen: Boolean,
    onDismissOpen: () -> Unit,
    onOpenResult: (bytes: ByteArray?) -> Unit,
    showSave: Boolean,
    onDismissSave: () -> Unit,
    bytesToSave: () -> ByteArray,
    // .rel import dialog (unused on Desktop - uses native dialogs in DesktopActions)
    showRelImport: Boolean,
    onDismissRelImport: () -> Unit,
    onRelImportResult: (bytes: ByteArray?) -> Unit,
    // GEDCOM dialogs (unused on Desktop - uses native dialogs in DesktopActions)
    showGedcomImport: Boolean,
    onDismissGedcomImport: () -> Unit,
    onGedcomImportResult: (bytes: ByteArray?) -> Unit,
    showGedcomExport: Boolean,
    onDismissGedcomExport: () -> Unit,
    gedcomBytesToSave: () -> ByteArray,
    // SVG export dialogs (unused on Desktop - uses native dialogs in DesktopActions)
    showSvgExport: Boolean,
    onDismissSvgExport: () -> Unit,
    svgBytesToSave: () -> ByteArray,
    showSvgExportFit: Boolean,
    onDismissSvgExportFit: () -> Unit,
    svgFitBytesToSave: () -> ByteArray,
    // AI text import dialog (unused on Desktop - uses native dialogs in DesktopActions)
    showAiTextImport: Boolean,
    onDismissAiTextImport: () -> Unit,
    onAiTextImportResult: (bytes: ByteArray?) -> Unit
) {
    // Open dialog (AwtWindow wrapper)
    if (showOpen) {
        AwtWindow(create = {
            object : FileDialog(null as Frame?, "Open Project JSON", LOAD) {}
        }, dispose = FileDialog::dispose) { dialog ->
            // Perform side-effect imperatively (no composable calls here)
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            val result = if (dir != null && file != null) {
                runCatching { Files.readAllBytes(File(dir, file).toPath()) }.getOrNull()
            } else null
            onOpenResult(result)
            onDismissOpen()
        }
    }

    // Save dialog
    if (showSave) {
        AwtWindow(create = {
            object : FileDialog(null as Frame?, "Save Project JSON", SAVE) {}
        }, dispose = FileDialog::dispose) { dialog ->
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                val path = File(dir, file).toPath()
                runCatching { Files.write(path, bytesToSave()) }
            }
            onDismissSave()
        }
    }
}
