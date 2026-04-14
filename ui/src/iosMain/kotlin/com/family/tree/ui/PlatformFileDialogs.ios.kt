@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.create
import androidx.compose.ui.uikit.LocalUIViewController

import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIWindow
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowScene
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.darwin.NSObject
import platform.posix.memcpy

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
    showMarkdownExport: Boolean,
    onDismissMarkdownExport: () -> Unit,
    markdownBytesToSave: () -> ByteArray,
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
    val localViewController = LocalUIViewController.current

    // Open dialog
    if (showOpen) {
        val delegate = remember {
                        object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                                        val url = didPickDocumentAtURL
                    
                    // Start accessing security-scoped resource (required for files from UIDocumentPicker)
                    val accessGranted = url.startAccessingSecurityScopedResource()
                                        
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                                        
                    // Stop accessing security-scoped resource
                    if (accessGranted) {
                        url.stopAccessingSecurityScopedResource()
                                            }
                    
                    onOpenResult(bytes)
                    onDismissOpen()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                                        onOpenResult(null)
                    onDismissOpen()
                }
            }
        }
        
        LaunchedEffect(showOpen) {
            if (!showOpen) return@LaunchedEffect
                        
            // Strategic delay to ensure UIKit and Compose layers are settled
            kotlinx.coroutines.delay(200)
            
            val hostController = localViewController
            
            withContext(Dispatchers.Main) {
                // Allow all file types for .ped (ZIP with JSON), JSON, and other formats
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeItem),
                    asCopy = true
                )
                
                picker.setDelegate(delegate)
                picker.modalPresentationStyle = platform.UIKit.UIModalPresentationFormSheet
                
                hostController.presentViewController(picker, animated = true) {
                }
            }

        }
    }
    
    // Save dialog
    if (showSave) {
        val delegate = remember {
                        object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                                        // File was saved/exported successfully
                    onDismissSave()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                                        onDismissSave()
                }
            }
        }
        
        LaunchedEffect(showSave) {
            if (!showSave) return@LaunchedEffect
                        
            kotlinx.coroutines.delay(200)
            val hostController = localViewController
            
            withContext(Dispatchers.Main) {
                val bytes = bytesToSave()
                val data = byteArrayToNSData(bytes)
                
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
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        forExportingURLs = listOf(tempFileUrl),
                        asCopy = true
                    )
                    
                    picker.setDelegate(delegate)
                    picker.modalPresentationStyle = platform.UIKit.UIModalPresentationFormSheet
                    
                    hostController.presentViewController(picker, animated = true) {
                    }
                } else {
                    // Failed to write temp file
                    onDismissSave()
                }
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
                                        
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                                        
                    // Stop accessing security-scoped resource
                    if (accessGranted) {
                        url.stopAccessingSecurityScopedResource()
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
        
        LaunchedEffect(showRelImport) {
            if (!showRelImport) return@LaunchedEffect
            kotlinx.coroutines.delay(200)
            val hostController = localViewController

            withContext(Dispatchers.Main) {
                // Allow all file types for .rel files
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeItem),
                    asCopy = true
                )
                picker.setDelegate(delegate)
                picker.modalPresentationStyle = platform.UIKit.UIModalPresentationFormSheet
                hostController.presentViewController(picker, animated = true) {
                }
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
        
        LaunchedEffect(showGedcomImport) {
            if (!showGedcomImport) return@LaunchedEffect
            kotlinx.coroutines.delay(200)
            val hostController = localViewController

            withContext(Dispatchers.Main) {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypeData, UTTypePlainText),
                    asCopy = true
                )
                picker.setDelegate(delegate)
                picker.modalPresentationStyle = platform.UIKit.UIModalPresentationFormSheet
                hostController.presentViewController(picker, animated = true) {
                }
            }

        }
    }
    
    // AI Text Import dialog
    if (showAiTextImport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    val url = didPickDocumentAtURL
                    
                    // Start accessing security-scoped resource
                    val accessGranted = url.startAccessingSecurityScopedResource()
                    
                    val data = NSData.create(contentsOfURL = url)
                    val bytes = data?.let { nsDataToByteArray(it) }
                    
                    // Stop accessing security-scoped resource
                    if (accessGranted) {
                        url.stopAccessingSecurityScopedResource()
                    }
                    
                    onAiTextImportResult(bytes)
                    onDismissAiTextImport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onAiTextImportResult(null)
                    onDismissAiTextImport()
                }
            }
        }
        
        LaunchedEffect(showAiTextImport) {
            if (!showAiTextImport) return@LaunchedEffect
            kotlinx.coroutines.delay(200)
            val hostController = localViewController

            withContext(Dispatchers.Main) {
                // Allow text and JSON files for AI import
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = listOf(UTTypePlainText, UTTypeJSON, UTTypeData),
                    asCopy = true
                )
                picker.setDelegate(delegate)
                picker.modalPresentationStyle = platform.UIKit.UIModalPresentationFormSheet
                hostController.presentViewController(picker, animated = true) {
                }
            }

        }
    }
    
    // Markdown Export dialog
    if (showMarkdownExport) {
        val delegate = remember {
            object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentAtURL: NSURL
                ) {
                    onDismissMarkdownExport()
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onDismissMarkdownExport()
                }
            }
        }
        
        LaunchedEffect(Unit) {
            val rootViewController = findTopmostViewController()
            if (rootViewController != null) {
                val bytes = markdownBytesToSave()
                val data = byteArrayToNSData(bytes)
                val fileManager = NSFileManager.defaultManager
                val tempDir = platform.Foundation.NSTemporaryDirectory()
                val tempPath = "${tempDir}family_tree.md"
                val tempFileUrl = NSURL.fileURLWithPath(tempPath)
                
                val success = fileManager.createFileAtPath(
                    path = tempPath,
                    contents = data,
                    attributes = null
                )
                
                if (success) {
                    val picker = UIDocumentPickerViewController(
                        forExportingURLs = listOf(tempFileUrl),
                        asCopy = true
                    )
                    picker.setDelegate(delegate)
                    rootViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    onDismissMarkdownExport()
                }
            } else {
                onDismissMarkdownExport()
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
            val rootViewController = findTopmostViewController()
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
                        forExportingURLs = listOf(tempFileUrl),
                        asCopy = true
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
            val rootViewController = findTopmostViewController()
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
                        forExportingURLs = listOf(tempFileUrl),
                        asCopy = true
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
            val rootViewController = findTopmostViewController()
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
                        forExportingURLs = listOf(tempFileUrl),
                        asCopy = true
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

private fun findTopmostViewController(): UIViewController? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        
    val windowScene = scenes.mapNotNull { it as? UIWindowScene }.firstOrNull { 
        it.activationState == UISceneActivationStateForegroundActive 
    }
        
    val window = if (windowScene != null) {
        windowScene.windows.mapNotNull { it as? UIWindow }.firstOrNull { it.isKeyWindow() }
    } else {
        UIApplication.sharedApplication.windows.mapNotNull { it as? UIWindow }.firstOrNull { it.isKeyWindow() }
    } ?: UIApplication.sharedApplication.keyWindow
    
        
    var topController = window?.rootViewController
        
    while (topController?.presentedViewController != null) {
        topController = topController.presentedViewController
            }
    
        return topController
}

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

private fun byteArrayToNSData(bytes: ByteArray): NSData {
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
}
