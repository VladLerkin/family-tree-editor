package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerMode
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
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
    svgFitBytesToSave: () -> ByteArray
) {
    // Open dialog
    if (showOpen) {
        val delegate = remember {
            println("[DEBUG_LOG] PlatformFileDialogs.ios: Creating open delegate")
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: documentPicker callback - file picked: $didPickDocumentAtURL")
                    val url = didPickDocumentAtURL
                    
                    // Start accessing security-scoped resource (required for files from UIDocumentPicker)
                    val accessGranted = url.startAccessingSecurityScopedResource()
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Security-scoped access granted: $accessGranted")
                    
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Read ${bytes?.size ?: 0} bytes from file")
                    
                    // Stop accessing security-scoped resource
                    if (accessGranted) {
                        url.stopAccessingSecurityScopedResource()
                        println("[DEBUG_LOG] PlatformFileDialogs.ios: Stopped accessing security-scoped resource")
                    }
                    
                    onOpenResult(bytes)
                    onDismissOpen()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: documentPickerWasCancelled callback")
                    onOpenResult(null)
                    onDismissOpen()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            println("[DEBUG_LOG] PlatformFileDialogs.ios: LaunchedEffect for open dialog")
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            println("[DEBUG_LOG] PlatformFileDialogs.ios: rootViewController = $rootViewController")
            if (rootViewController != null) {
                // Allow all file types for .ped (ZIP with JSON), JSON, and other formats
                val documentTypes = listOf("public.item")
                val picker = UIDocumentPickerViewController(
                    documentTypes = documentTypes,
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                
                picker.setDelegate(delegate)
                println("[DEBUG_LOG] PlatformFileDialogs.ios: Presenting picker, delegate set to $delegate")
                rootViewController.presentViewController(picker, animated = true, completion = null)
                println("[DEBUG_LOG] PlatformFileDialogs.ios: presentViewController called")
            } else {
                // Fallback if no root view controller
                println("[DEBUG_LOG] PlatformFileDialogs.ios: No root view controller, returning null")
                onOpenResult(null)
                onDismissOpen()
            }
        }
    }
    
    // Save dialog
    if (showSave) {
        val delegate = remember {
            println("[DEBUG_LOG] PlatformFileDialogs.ios: Creating save delegate")
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Save documentPicker callback - file saved to: $didPickDocumentAtURL")
                    // File was saved/exported successfully
                    onDismissSave()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Save documentPickerWasCancelled callback")
                    onDismissSave()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            println("[DEBUG_LOG] PlatformFileDialogs.ios: LaunchedEffect for save dialog")
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            println("[DEBUG_LOG] PlatformFileDialogs.ios: Save rootViewController = $rootViewController")
            if (rootViewController != null) {
                val bytes = bytesToSave()
                val data = byteArrayToNSData(bytes)
                println("[DEBUG_LOG] PlatformFileDialogs.ios: Prepared ${bytes.size} bytes for saving")
                
                // Write to temporary file first using NSFileManager
                val fileManager = NSFileManager.defaultManager
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                val tempPath = "${tempDir}project.ped"
                val tempFileUrl = NSURL.fileURLWithPath(tempPath)
                
                // Write data to temporary file
                val success = fileManager.createFileAtPath(
                    path = tempPath,
                    contents = data,
                    attributes = null
                )
                println("[DEBUG_LOG] PlatformFileDialogs.ios: Temp file write success = $success")
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        uRL = tempFileUrl,
                        inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService
                    )
                    
                    picker.setDelegate(delegate)
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Presenting save picker, delegate set to $delegate")
                    rootViewController.presentViewController(picker, animated = true, completion = null)
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Save presentViewController called")
                } else {
                    // Failed to write temp file
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: Failed to write temp file")
                    onDismissSave()
                }
            } else {
                // Fallback if no root view controller
                println("[DEBUG_LOG] PlatformFileDialogs.ios: Save - No root view controller")
                onDismissSave()
            }
        }
    }
    
    // REL Import dialog
    if (showRelImport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    val url = didPickDocumentAtURL
                    
                    // Start accessing security-scoped resource (required for files from UIDocumentPicker)
                    val accessGranted = url.startAccessingSecurityScopedResource()
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: REL Import - Security-scoped access granted: $accessGranted")
                    
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                    println("[DEBUG_LOG] PlatformFileDialogs.ios: REL Import - Read ${bytes?.size ?: 0} bytes from file")
                    
                    // Stop accessing security-scoped resource
                    if (accessGranted) {
                        url.stopAccessingSecurityScopedResource()
                        println("[DEBUG_LOG] PlatformFileDialogs.ios: REL Import - Stopped accessing security-scoped resource")
                    }
                    
                    onRelImportResult(bytes)
                    onDismissRelImport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onRelImportResult(null)
                    onDismissRelImport()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                // Allow all file types for .rel files
                val documentTypes = listOf("public.item")
                val picker = UIDocumentPickerViewController(
                    documentTypes = documentTypes,
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                picker.setDelegate(delegate)
                rootViewController.presentViewController(picker, animated = true, completion = null)
            } else {
                onRelImportResult(null)
                onDismissRelImport()
            }
        }
    }
    
    // GEDCOM Import dialog
    if (showGedcomImport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    val url = didPickDocumentAtURL
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                    onGedcomImportResult(bytes)
                    onDismissGedcomImport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onGedcomImportResult(null)
                    onDismissGedcomImport()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val documentTypes = listOf("public.data", "public.plain-text")
                val picker = UIDocumentPickerViewController(
                    documentTypes = documentTypes,
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                picker.setDelegate(delegate)
                rootViewController.presentViewController(picker, animated = true, completion = null)
            } else {
                onGedcomImportResult(null)
                onDismissGedcomImport()
            }
        }
    }
    
    // GEDCOM Export dialog
    if (showGedcomExport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    onDismissGedcomExport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDismissGedcomExport()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val bytes = gedcomBytesToSave()
                val data = byteArrayToNSData(bytes)
                val fileManager = NSFileManager.defaultManager
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                val tempPath = "${tempDir}family_tree.ged"
                val tempFileUrl = NSURL.fileURLWithPath(tempPath)
                
                val success = fileManager.createFileAtPath(
                    path = tempPath,
                    contents = data,
                    attributes = null
                )
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        uRL = tempFileUrl,
                        inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService
                    )
                    picker.setDelegate(delegate)
                    rootViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    onDismissGedcomExport()
                }
            } else {
                onDismissGedcomExport()
            }
        }
    }
    
    // SVG Export dialog
    if (showSvgExport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    onDismissSvgExport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDismissSvgExport()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val bytes = svgBytesToSave()
                val data = byteArrayToNSData(bytes)
                val fileManager = NSFileManager.defaultManager
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                val tempPath = "${tempDir}tree_export.svg"
                val tempFileUrl = NSURL.fileURLWithPath(tempPath)
                
                val success = fileManager.createFileAtPath(
                    path = tempPath,
                    contents = data,
                    attributes = null
                )
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        uRL = tempFileUrl,
                        inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService
                    )
                    picker.setDelegate(delegate)
                    rootViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    onDismissSvgExport()
                }
            } else {
                onDismissSvgExport()
            }
        }
    }
    
    // SVG Export Fit dialog
    if (showSvgExportFit) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    onDismissSvgExportFit()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDismissSvgExportFit()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val bytes = svgFitBytesToSave()
                val data = byteArrayToNSData(bytes)
                val fileManager = NSFileManager.defaultManager
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                val tempPath = "${tempDir}tree_export_fit.svg"
                val tempFileUrl = NSURL.fileURLWithPath(tempPath)
                
                val success = fileManager.createFileAtPath(
                    path = tempPath,
                    contents = data,
                    attributes = null
                )
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        uRL = tempFileUrl,
                        inMode = UIDocumentPickerMode.UIDocumentPickerModeExportToService
                    )
                    picker.setDelegate(delegate)
                    rootViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    onDismissSvgExportFit()
                }
            } else {
                onDismissSvgExportFit()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun nsDataToByteArray(data: NSData): ByteArray {
    val size = data.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, size.toULong())
        }
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
private fun byteArrayToNSData(bytes: ByteArray): NSData {
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
}
