package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

@Composable
actual fun PlatformFileDialogs(
    showOpen: Boolean,
    onDismissOpen: () -> Unit,
    onOpenResult: (bytes: ByteArray?) -> Unit,
    showSave: Boolean,
    onDismissSave: () -> Unit,
    bytesToSave: () -> ByteArray,
    showRelImport: Boolean,
    onDismissRelImport: () -> Unit,
    onRelImportResult: (bytes: ByteArray?) -> Unit,
    showGedcomImport: Boolean,
    onDismissGedcomImport: () -> Unit,
    onGedcomImportResult: (bytes: ByteArray?) -> Unit,
    showGedcomExport: Boolean,
    onDismissGedcomExport: () -> Unit,
    gedcomBytesToSave: () -> ByteArray,
    showSvgExport: Boolean,
    onDismissSvgExport: () -> Unit,
    svgBytesToSave: () -> ByteArray,
    showSvgExportFit: Boolean,
    onDismissSvgExportFit: () -> Unit,
    svgFitBytesToSave: () -> ByteArray,
    showAiTextImport: Boolean,
    onDismissAiTextImport: () -> Unit,
    onAiTextImportResult: (bytes: ByteArray?) -> Unit
) {
    // Open project dialog
    if (showOpen) {
        LaunchedEffect(showOpen) {
            openFileDialog(".ped") { bytes ->
                onOpenResult(bytes)
                onDismissOpen()
            }
        }
    }

    // Save project dialog
    if (showSave) {
        LaunchedEffect(Unit) {
            val bytes = bytesToSave()
            downloadFile(bytes, "project.json", "application/json")
            onDismissSave()
        }
    }

    // .rel import dialog
    if (showRelImport) {
        LaunchedEffect(showRelImport) {
            openFileDialog(".rel") { bytes ->
                onRelImportResult(bytes)
                onDismissRelImport()
            }
        }
    }

    // GEDCOM import dialog
    if (showGedcomImport) {
        LaunchedEffect(showGedcomImport) {
            openFileDialog(".ged,.gedcom") { bytes ->
                onGedcomImportResult(bytes)
                onDismissGedcomImport()
            }
        }
    }

    // GEDCOM export dialog
    if (showGedcomExport) {
        LaunchedEffect(Unit) {
            val bytes = gedcomBytesToSave()
            downloadFile(bytes, "family-tree.ged", "text/plain")
            onDismissGedcomExport()
        }
    }

    // SVG export (current view) dialog
    if (showSvgExport) {
        LaunchedEffect(Unit) {
            val bytes = svgBytesToSave()
            downloadFile(bytes, "family-tree-current.svg", "image/svg+xml")
            onDismissSvgExport()
        }
    }

    // SVG export (fit to content) dialog
    if (showSvgExportFit) {
        LaunchedEffect(Unit) {
            val bytes = svgFitBytesToSave()
            downloadFile(bytes, "family-tree-fit.svg", "image/svg+xml")
            onDismissSvgExportFit()
        }
    }

    // AI text import dialog
    if (showAiTextImport) {
        LaunchedEffect(showAiTextImport) {
            openFileDialog(".txt,.pdf") { bytes ->
                onAiTextImportResult(bytes)
                onDismissAiTextImport()
            }
        }
    }
}

/**
 * Opens a file picker dialog and reads the selected file as ByteArray
 */
private fun openFileDialog(accept: String, onResult: (ByteArray?) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = accept
    input.style.display = "none"
    
    input.onchange = {
        val file = input.files?.get(0)
        if (file != null) {
            println("[DEBUG] File selected: ${file.name}, size: ${file.size} bytes")
            val reader = FileReader()
            reader.onload = {
                val arrayBuffer = reader.result
                if (arrayBuffer != null) {
                    val int8Array = Int8Array(arrayBuffer as org.khronos.webgl.ArrayBuffer)
                    val byteArray = ByteArray(int8Array.length) { i -> int8Array[i] }
                    println("[DEBUG] File read successfully: ${byteArray.size} bytes")
                    println("[DEBUG] First 100 bytes: ${byteArray.take(100).joinToString(",")}")
                    onResult(byteArray)
                } else {
                    println("[DEBUG] ArrayBuffer is null")
                    onResult(null)
                }
                // Remove input element after file is read
                document.body?.removeChild(input)
                null
            }
            reader.onerror = {
                println("Error reading file: ${reader.error}")
                onResult(null)
                // Remove input element on error
                document.body?.removeChild(input)
                null
            }
            reader.readAsArrayBuffer(file)
        } else {
            onResult(null)
            // Remove input element if no file selected
            document.body?.removeChild(input)
        }
        null
    }
    
    input.oncancel = {
        println("[DEBUG] File dialog cancelled")
        onResult(null)
        // Remove input element when dialog is cancelled
        document.body?.removeChild(input)
        null
    }
    
    document.body?.appendChild(input)
    input.click()
}

/**
 * Downloads a file with the given content
 */
private fun downloadFile(bytes: ByteArray, filename: String, mimeType: String) {
    // Create a data URL from bytes
    val base64 = bytes.encodeToBase64()
    val dataUrl = "data:$mimeType;base64,$base64"
    
    val a = document.createElement("a") as org.w3c.dom.HTMLAnchorElement
    a.href = dataUrl
    a.download = filename
    a.style.display = "none"
    
    document.body?.appendChild(a)
    a.click()
    document.body?.removeChild(a)
}

/**
 * Encodes ByteArray to Base64 string
 */
private fun ByteArray.encodeToBase64(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val result = StringBuilder()
    var i = 0
    while (i < size) {
        val b1 = this[i++].toInt() and 0xFF
        val b2 = if (i < size) this[i++].toInt() and 0xFF else 0
        val b3 = if (i < size) this[i++].toInt() and 0xFF else 0
        
        val n = (b1 shl 16) or (b2 shl 8) or b3
        
        result.append(chars[n shr 18 and 0x3F])
        result.append(chars[n shr 12 and 0x3F])
        result.append(if (i > size + 1) '=' else chars[n shr 6 and 0x3F])
        result.append(if (i > size) '=' else chars[n and 0x3F])
    }
    return result.toString()
}
