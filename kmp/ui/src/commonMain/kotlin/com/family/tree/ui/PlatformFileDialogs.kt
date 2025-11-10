package com.family.tree.ui

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformFileDialogs(
    showOpen: Boolean,
    onDismissOpen: () -> Unit,
    onOpenResult: (bytes: ByteArray?) -> Unit,
    showSave: Boolean,
    onDismissSave: () -> Unit,
    bytesToSave: () -> ByteArray,
    // GEDCOM dialogs
    showGedcomImport: Boolean = false,
    onDismissGedcomImport: () -> Unit = {},
    onGedcomImportResult: (bytes: ByteArray?) -> Unit = {},
    showGedcomExport: Boolean = false,
    onDismissGedcomExport: () -> Unit = {},
    gedcomBytesToSave: () -> ByteArray = { ByteArray(0) },
    // SVG export dialogs
    showSvgExport: Boolean = false,
    onDismissSvgExport: () -> Unit = {},
    svgBytesToSave: () -> ByteArray = { ByteArray(0) },
    showSvgExportFit: Boolean = false,
    onDismissSvgExportFit: () -> Unit = {},
    svgFitBytesToSave: () -> ByteArray = { ByteArray(0) }
)
