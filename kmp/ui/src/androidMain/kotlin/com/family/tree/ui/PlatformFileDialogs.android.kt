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
    // .rel import dialog
    showRelImport: Boolean,
    onDismissRelImport: () -> Unit,
    onRelImportResult: (bytes: ByteArray?) -> Unit,
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
    svgFitBytesToSave: () -> ByteArray,
    // AI text import dialog
    showAiTextImport: Boolean,
    onDismissAiTextImport: () -> Unit,
    onAiTextImportResult: (bytes: ByteArray?) -> Unit
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
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
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

    // REL IMPORT — SAF OPEN_DOCUMENT
    val relImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri == null) {
                onRelImportResult(null)
                onDismissRelImport()
            } else {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
                onRelImportResult(bytes)
                onDismissRelImport()
            }
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

    // AI TEXT IMPORT — SAF OPEN_DOCUMENT
    val aiTextImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            println("[DEBUG_LOG] PlatformFileDialogs.android: aiTextImportLauncher.onResult called with uri=$uri")
            if (uri == null) {
                println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - uri is null, calling onAiTextImportResult(null)")
                onAiTextImportResult(null)
                onDismissAiTextImport()
            } else {
                println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - reading file from uri")
                // Take persistable URI permission for files from cloud providers (Google Drive, etc.)
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - takePersistableUriPermission successful")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - takePersistableUriPermission failed (not needed): ${e.message}")
                }
                
                val bytes = runCatching {
                    // Check if this is a virtual file (e.g., from Google Drive) and get its MIME type
                    val fileInfo = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val flagsIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_FLAGS)
                            val mimeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                            
                            val isVirtual = if (flagsIndex >= 0) {
                                val flags = cursor.getInt(flagsIndex)
                                (flags and android.provider.DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0
                            } else {
                                false
                            }
                            
                            val mimeType = if (mimeIndex >= 0) {
                                cursor.getString(mimeIndex)
                            } else {
                                null
                            }
                            
                            Pair(isVirtual, mimeType)
                        } else {
                            Pair(false, null)
                        }
                    } ?: Pair(false, null)
                    
                    val (isVirtual, documentMimeType) = fileInfo
                    println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - isVirtual=$isVirtual, documentMimeType=$documentMimeType")
                    
                    if (isVirtual) {
                        // For virtual files from Google Drive, we need to export them to a readable format
                        println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - virtual file detected, attempting export")
                        
                        // Try different export MIME types in order of preference
                        val exportMimeTypes = when {
                            documentMimeType?.startsWith("application/vnd.google-apps.document") == true -> 
                                listOf(
                                    "text/plain",
                                    "text/html", 
                                    "application/rtf",
                                    "application/vnd.oasis.opendocument.text",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/pdf"
                                )
                            documentMimeType?.startsWith("application/vnd.google-apps") == true -> 
                                listOf("text/plain", "application/pdf")
                            else -> 
                                listOf("text/plain", "*/*")
                        }
                        
                        var data: ByteArray? = null
                        for (exportMime in exportMimeTypes) {
                            println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - trying export with MIME type: $exportMime")
                            try {
                                val assetFileDescriptor = context.contentResolver.openTypedAssetFileDescriptor(
                                    uri,
                                    exportMime,
                                    null
                                )
                                
                                data = assetFileDescriptor?.use { afd ->
                                    val inputStream = afd.createInputStream()
                                    val bytes = inputStream.readBytes()
                                    println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - successfully read ${bytes.size} bytes with $exportMime")
                                    bytes
                                }
                                
                                if (data != null) break
                            } catch (e: Exception) {
                                println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - failed with $exportMime: ${e.message}")
                            }
                        }
                        
                        data
                    } else {
                        // For regular files, use openInputStream
                        println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - using openInputStream for regular file")
                        val inputStream = context.contentResolver.openInputStream(uri)
                        println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - openInputStream returned: $inputStream")
                        inputStream?.use { stream ->
                            val data = stream.readBytes()
                            println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - readBytes returned ${data.size} bytes")
                            data
                        }
                    }
                }.onFailure { e ->
                    println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - ERROR reading bytes: ${e.message}")
                    e.printStackTrace()
                }.getOrNull()
                println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - final bytes size: ${bytes?.size ?: 0}, calling onAiTextImportResult")
                onAiTextImportResult(bytes)
                onDismissAiTextImport()
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
            // .ped files (ZIP/octet-stream), JSON, and allow all files
            openLauncher.launch(arrayOf("application/octet-stream", "application/zip", "application/json", "*/*"))
        }
    }

    if (showSave) {
        LaunchedEffect(Unit) {
            // Suggest a default filename
            createLauncher.launch("project.ped")
        }
    }

    if (showRelImport) {
        LaunchedEffect(Unit) {
            // .rel files (legacy binary format)
            relImportLauncher.launch(arrayOf("application/octet-stream"))
        }
    }

    if (showGedcomImport) {
        LaunchedEffect(Unit) {
            gedcomImportLauncher.launch(arrayOf("application/x-gedcom", "*/*"))
        }
    }

    if (showAiTextImport) {
        LaunchedEffect(Unit) {
            println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - LaunchedEffect triggered")
            // Text files (.txt, .json for AI results)
            aiTextImportLauncher.launch(arrayOf("text/plain", "application/json", "*/*"))
            println("[DEBUG_LOG] PlatformFileDialogs.android: AI Text Import - Launcher started")
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
