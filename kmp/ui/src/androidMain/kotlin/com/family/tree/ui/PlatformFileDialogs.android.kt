package com.family.tree.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
actual fun PlatformFileDialogs(
    showOpen: Boolean,
    onDismissOpen: () -> Unit,
    onOpenResult: (bytes: ByteArray?) -> Unit,
    showSave: Boolean,
    onDismissSave: () -> Unit,
    bytesToSave: () -> ByteArray,
    // GEDCOM dialogs
    showGedcomImport: Boolean,
    onDismissGedcomImport: () -> Unit,
    onGedcomImportResult: (bytes: ByteArray?) -> Unit,
    showGedcomExport: Boolean,
    onDismissGedcomExport: () -> Unit,
    gedcomBytesToSave: () -> ByteArray,
    // SVG export dialogs
    showSvgExport: Boolean,
    onDismissSvgExport: () -> Unit,
    svgBytesToSave: () -> ByteArray,
    showSvgExportFit: Boolean,
    onDismissSvgExportFit: () -> Unit,
    svgFitBytesToSave: () -> ByteArray
) {
    val context = LocalContext.current

    // OPEN — SAF ACTION_OPEN_DOCUMENT
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                onOpenResult(null)
                onDismissOpen()
            } else {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                onOpenResult(bytes)
                onDismissOpen()
            }
        }
    )

    // SAVE — SAF CREATE_DOCUMENT
    val createLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytesToSave())
                        out.flush()
                    }
                }
            }
            onDismissSave()
        }
    )

    // GEDCOM IMPORT — SAF OPEN_DOCUMENT
    val gedcomImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                onGedcomImportResult(null)
                onDismissGedcomImport()
            } else {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                onGedcomImportResult(bytes)
                onDismissGedcomImport()
            }
        }
    )

    // GEDCOM EXPORT — SAF CREATE_DOCUMENT
    val gedcomExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-gedcom"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(gedcomBytesToSave())
                        out.flush()
                    }
                }
            }
            onDismissGedcomExport()
        }
    )

    // SVG EXPORT — SAF CREATE_DOCUMENT
    val svgExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(svgBytesToSave())
                        out.flush()
                    }
                }
            }
            onDismissSvgExport()
        }
    )

    // SVG EXPORT FIT — SAF CREATE_DOCUMENT
    val svgExportFitLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml"),
        onResult = { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(svgFitBytesToSave())
                        out.flush()
                    }
                }
            }
            onDismissSvgExportFit()
        }
    )

    if (showOpen) {
        LaunchedEffect(Unit) {
            // JSON first, allow any as fallback via user picker
            openLauncher.launch(arrayOf("application/json", "text/*", "application/*"))
        }
    }

    if (showSave) {
        LaunchedEffect(Unit) {
            // Suggest a default filename
            createLauncher.launch("project.json")
        }
    }

    if (showGedcomImport) {
        LaunchedEffect(Unit) {
            gedcomImportLauncher.launch(arrayOf("application/x-gedcom", "text/*", "*/*"))
        }
    }

    if (showGedcomExport) {
        LaunchedEffect(Unit) {
            gedcomExportLauncher.launch("family_tree.ged")
        }
    }

    if (showSvgExport) {
        LaunchedEffect(Unit) {
            svgExportLauncher.launch("tree_export.svg")
        }
    }

    if (showSvgExportFit) {
        LaunchedEffect(Unit) {
            svgExportFitLauncher.launch("tree_export_fit.svg")
        }
    }
}
