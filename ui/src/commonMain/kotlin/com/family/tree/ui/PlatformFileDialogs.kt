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
    // .rel import dialog
    showRelImport: Boolean = false,
    onDismissRelImport: () -> Unit = {},
    onRelImportResult: (bytes: ByteArray?) -> Unit = {},
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
    svgFitBytesToSave: () -> ByteArray = { ByteArray(0) },
    // AI text import dialog
    showAiTextImport: Boolean = false,
    onDismissAiTextImport: () -> Unit = {},
    onAiTextImportResult: (bytes: ByteArray?) -> Unit = {}
)
