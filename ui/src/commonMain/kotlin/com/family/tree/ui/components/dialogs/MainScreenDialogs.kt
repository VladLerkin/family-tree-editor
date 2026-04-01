package com.family.tree.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.family.tree.core.ProjectData
import com.family.tree.core.ai.VoiceInputProcessor
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.io.RelImporter
import com.family.tree.core.io.RelRepository
import com.family.tree.core.layout.ProjectLayout
import com.family.tree.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreenDialogs(
    state: MainScreenDialogState,
    project: ProjectData,
    onUpdateProject: (ProjectData) -> Unit,
    loadedProjectTemp: LoadedProject?,
    setLoadedProjectTemp: (LoadedProject?) -> Unit,
    onUpdateProjectLayout: (ProjectLayout?) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onUpdatePan: (Offset) -> Unit,
    onClearSelection: () -> Unit,
    fitToView: () -> Unit,
    platformContext: Any?,
    scope: CoroutineScope,
    scale: Float,
    pan: Offset,
    voiceInputProcessor: VoiceInputProcessor
) {


    // Person editor dialog
    run {
        val person = project.individuals.find { it.id == state.editPersonId }
        if (person != null) {
            PersonEditorDialog(
                person = person,
                onSave = { updated ->
                    val idx = project.individuals.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) {
                        onUpdateProject(project.copy(
                            individuals = project.individuals.toMutableList().also { it[idx] = updated }
                        ))
                    }
                    state.editPersonId = null
                },
                onDismiss = { state.editPersonId = null }
            )
        }
    }
    // Family editor dialog
    run {
        val fam = project.families.find { it.id == state.editFamilyId }
        if (fam != null) {
            FamilyEditorDialog(
                family = fam,
                allIndividuals = project.individuals,
                onSave = { updated ->
                    val idx = project.families.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) {
                        onUpdateProject(project.copy(
                            families = project.families.toMutableList().also { it[idx] = updated }
                        ))
                    }
                    state.editFamilyId = null
                },
                onDismiss = { state.editFamilyId = null }
            )
        }
    }
    
    // Sources manager dialog
    if (state.showSourcesDialog) {
        SourcesManagerDialog(
            project = project,
            onDismiss = { state.showSourcesDialog = false },
            onUpdateProject = { updatedProject ->
                onUpdateProject(updatedProject)
            }
        )
    }

    // About dialog
    if (state.showAboutDialog) {
        AboutDialog(onDismiss = { state.showAboutDialog = false })
    }
    
    // AI Settings dialog
    if (state.showAiSettingsDialog) {
        val storage = remember { com.family.tree.core.ai.AiSettingsStorage() }
        val savedConfig = remember { storage.loadConfig() }
        
        AiConfigDialog(
            initialConfig = savedConfig,
            onDismiss = { state.showAiSettingsDialog = false },
            onConfirm = { config ->
                storage.saveConfig(config)
                state.showAiSettingsDialog = false
            }
        )
    }
    
    // AI Import Progress dialog
    if (state.showAiImportProgress) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* Cannot dismiss during import */ }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .width(400.dp)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "AI Import",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )
                    
                    Text(
                        text = state.aiImportProgressMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // .rel Import Progress dialog
    if (state.showRelImportProgress) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* Cannot dismiss during import */ }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .width(400.dp)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = ".rel Import",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )
                    
                    Text(
                        text = state.relImportProgressMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // .rel Import Error dialog
    if (state.showRelImportError) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                state.showRelImportError = false
                state.relImportErrorMessage = ""
            }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .width(500.dp)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = ".rel Import Error",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Text(
                        text = state.relImportErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                state.showRelImportError = false
                                state.relImportErrorMessage = ""
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    // AI Import Info dialog (for errors/warnings like PDF detection)
    if (state.showAiImportInfoDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                state.showAiImportInfoDialog = false
                state.showAiTextImportDialog = false
                state.pendingAiTextImportCallback?.invoke(null)
                state.pendingAiTextImportCallback = null
            }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .width(500.dp)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Information",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Text(
                        text = state.aiImportInfoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        // Show "Open Settings" button if it's a permission error
                        if (state.isPermissionError) {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    // Open app settings on Android
                                    val voiceRecorder = com.family.tree.core.platform.VoiceRecorder(platformContext)
                                    voiceRecorder.openAppSettings()
                                    state.showAiImportInfoDialog = false
                                    state.showAiTextImportDialog = false
                                    state.pendingAiTextImportCallback?.invoke(null)
                                    state.pendingAiTextImportCallback = null
                                    state.isPermissionError = false
                                }
                            ) {
                                Text("Open Settings")
                            }
                        }
                        
                        androidx.compose.material3.TextButton(
                            onClick = {
                                state.showAiImportInfoDialog = false
                                state.showAiTextImportDialog = false
                                state.pendingAiTextImportCallback?.invoke(null)
                                state.pendingAiTextImportCallback = null
                                state.isPermissionError = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
    
    // Voice input dialog
    if (state.showVoiceInputDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                voiceInputProcessor.cancelRecording()
                state.isVoiceRecording = false
                state.showVoiceInputDialog = false
                state.voiceInputStatus = ""
            },
            modifier = Modifier.widthIn(min = 400.dp),
            title = { Text("Voice Input") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.isVoiceRecording) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    Text(
                        text = state.voiceInputStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stop Recording button (shown only during recording)
                    if (state.isVoiceRecording && state.voiceInputStatus == "Speak...") {
                        Button(
                            onClick = {
                                println("[DEBUG_LOG] MainScreen: User clicked 'Stop Recording' button")
                                voiceInputProcessor.stopRecording()
                                state.isVoiceRecording = false
                                state.voiceInputStatus = "Processing recording..."
                            }
                        ) {
                            Text("Stop Recording")
                        }
                    }
                    // Кнопка "Отмена" (всегда показывается)
                    Button(
                        onClick = {
                            println("[DEBUG_LOG] MainScreen: User clicked 'Cancel' button")
                            voiceInputProcessor.cancelRecording()
                            state.isVoiceRecording = false
                            state.showVoiceInputDialog = false
                            state.voiceInputStatus = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Platform file dialogs (for Android and future cross-platform support)
    // Effect to apply loaded project data to main state
    LaunchedEffect(loadedProjectTemp) {
        val loaded = loadedProjectTemp
        if (loaded != null) {
            println("[DEBUG_LOG] MainScreen LaunchedEffect: Applying loaded project with ${loaded.data.individuals.size} individuals")
            onUpdateProject(loaded.data)
            onUpdateProjectLayout(loaded.layout)
            onClearSelection()
            // Apply viewport from layout if present; otherwise fit to content
            val layout = loaded.layout
            if (layout != null && (layout.zoom != 1.0 || layout.viewOriginX != 0.0 || layout.viewOriginY != 0.0)) {
                onUpdateScale(layout.zoom.toFloat())
                onUpdatePan(Offset(layout.viewOriginX.toFloat(), layout.viewOriginY.toFloat()))
            } else {
                fitToView()
            }
            println("[DEBUG_LOG] MainScreen LaunchedEffect: Applied project state - project.individuals=${project.individuals.size}")
            setLoadedProjectTemp(null)  // Clear after applying
        }
    }
    
    PlatformFileDialogs(
        showOpen = state.showOpenDialog,
        onDismissOpen = {
            state.showOpenDialog = false
            state.pendingOpenCallback = null
        },
        onOpenResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onOpenResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = state.pendingOpenCallback
            println("[DEBUG_LOG] MainScreen.onOpenResult: callback=$callback")
            if (bytes != null) {
                // Show progress dialog
                state.showRelImportProgress = true
                state.relImportProgressMessage = "Opening file..."
                
                // Import in background to avoid blocking UI on Android TV
                scope.launch {
                    try {
                        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            runCatching {
                                // Try RelRepository first (for .ped ZIP format with JSON)
                                RelRepository().read(bytes)
                            }.recoverCatching {
                                // Fallback to RelImporter for legacy .rel binary TLV format
                                println("[DEBUG_LOG] MainScreen.onOpenResult: RelRepository failed, trying RelImporter for legacy .rel format")
                                RelImporter().importFromBytes(bytes, onProgress = { progress ->
                                    // Update progress message on main thread
                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        state.relImportProgressMessage = progress
                                    }
                                })
                            }.getOrNull()
                        }
                        println("[DEBUG_LOG] MainScreen.onOpenResult: loaded=$loaded, data has ${loaded?.data?.individuals?.size ?: 0} individuals")
                        
                        // Hide progress dialog
                        state.showRelImportProgress = false
                        
                        // Instead of invoking callback directly, store in temp state to trigger LaunchedEffect
                        if (loaded != null) {
                            setLoadedProjectTemp(loaded)
                        }
                        callback?.invoke(loaded)
                        println("[DEBUG_LOG] MainScreen.onOpenResult: callback invoked")
                        
                        // Reset dialog state
                        state.showOpenDialog = false
                        state.pendingOpenCallback = null
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] MainScreen.onOpenResult: ERROR opening file - ${e.message}")
                        e.printStackTrace()
                        
                        // Hide progress dialog
                        state.showRelImportProgress = false
                        
                        callback?.invoke(null)
                        
                        // Reset dialog state
                        state.showOpenDialog = false
                        state.pendingOpenCallback = null
                    }
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onOpenResult: bytes is null, invoking callback with null")
                callback?.invoke(null)
                state.showOpenDialog = false
                state.pendingOpenCallback = null
            }
        },
        showSave = state.showSaveDialog,
        onDismissSave = {
            state.showSaveDialog = false
            state.pendingSaveData = null
        },
        bytesToSave = {
            val data = state.pendingSaveData ?: project
            RelRepository().write(data, null, null)
        },
        // .rel import
        showRelImport = state.showRelImportDialog,
        onDismissRelImport = {
            state.showRelImportDialog = false
            state.pendingRelImportCallback = null
        },
        onRelImportResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onRelImportResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = state.pendingRelImportCallback
            if (bytes != null) {
                // Show progress dialog
                state.showRelImportProgress = true
                state.relImportProgressMessage = "Preparing to import..."
                
                // Import in background to avoid blocking UI on Android TV
                scope.launch {
                    try {
                        // Give UI time to render the progress dialog
                        delay(100)
                        
                        state.relImportProgressMessage = "Importing .rel file..."
                        
                        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            runCatching {
                                // Use RelImporter for legacy .rel binary TLV format
                                println("[DEBUG_LOG] MainScreen.onRelImportResult: Using RelImporter for .rel format")
                                RelImporter().importFromBytes(bytes, onProgress = { progress ->
                                    // Update progress message on main thread
                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        state.relImportProgressMessage = progress
                                    }
                                })
                            }.getOrNull()
                        }
                        println("[DEBUG_LOG] MainScreen.onRelImportResult: loaded=$loaded, data has ${loaded?.data?.individuals?.size ?: 0} individuals")
                        
                        // Hide progress dialog
                        state.showRelImportProgress = false
                        
                        if (loaded != null) {
                            setLoadedProjectTemp(loaded)
                        }
                        callback?.invoke(loaded)
                        
                        // Reset dialog state
                        state.showRelImportDialog = false
                        state.pendingRelImportCallback = null
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] MainScreen.onRelImportResult: ERROR importing .rel - ${e.message}")
                        e.printStackTrace()
                        
                        // Hide progress dialog
                        state.showRelImportProgress = false
                        
                        // Show error dialog with detailed message
                        state.relImportErrorMessage = e.message ?: "Unknown error occurred while importing .rel file"
                        state.showRelImportError = true
                        
                        callback?.invoke(null)
                        
                        // Reset dialog state
                        state.showRelImportDialog = false
                        state.pendingRelImportCallback = null
                    }
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onRelImportResult: bytes is null")
                callback?.invoke(null)
                state.showRelImportDialog = false
                state.pendingRelImportCallback = null
            }
        },
        // GEDCOM import
        showGedcomImport = state.showGedcomImportDialog,
        onDismissGedcomImport = {
            state.showGedcomImportDialog = false
            state.pendingGedcomImportCallback = null
        },
        onGedcomImportResult = { bytes ->
            val callback = state.pendingGedcomImportCallback
            if (bytes != null) {
                val content = bytes.decodeToString()
                val imported = runCatching {
                    com.family.tree.core.gedcom.GedcomImporter().importFromString(content)
                }.getOrNull()
                callback?.invoke(imported)
            } else {
                callback?.invoke(null)
            }
            state.showGedcomImportDialog = false
            state.pendingGedcomImportCallback = null
        },
        // GEDCOM export
        showGedcomExport = state.showGedcomExportDialog,
        onDismissGedcomExport = {
            state.showGedcomExportDialog = false
            state.pendingGedcomExportData = null
        },
        gedcomBytesToSave = {
            val data = state.pendingGedcomExportData ?: project
            val exporter = com.family.tree.core.gedcom.GedcomExporter()
            val content = exporter.exportToString(data)
            content.encodeToByteArray()
        },
        // Markdown Tree export
        showMarkdownExport = state.showMarkdownExportDialog,
        onDismissMarkdownExport = {
            state.showMarkdownExportDialog = false
            state.pendingMarkdownExportData = null
        },
        markdownBytesToSave = {
            val data = state.pendingMarkdownExportData ?: project
            val exporter = com.family.tree.core.export.MarkdownTreeExporter()
            val content = exporter.exportToString(data)
            content.encodeToByteArray()
        },
        // SVG export (current view)
        showSvgExport = state.showSvgExportDialog,
        onDismissSvgExport = {
            state.showSvgExportDialog = false
            state.pendingSvgExportData = null
        },
        svgBytesToSave = {
            // TODO: Implement SVG exporter for KMP
            val (data, exportScale, exportPan) = state.pendingSvgExportData ?: Triple(project, scale, pan)
            "<!-- SVG export not yet implemented for Android/iOS -->".encodeToByteArray()
        },
        // SVG export (fit to content)
        showSvgExportFit = state.showSvgExportFitDialog,
        onDismissSvgExportFit = {
            state.showSvgExportFitDialog = false
            state.pendingSvgExportFitData = null
        },
        svgFitBytesToSave = {
            // TODO: Implement SVG exporter for KMP
            val data = state.pendingSvgExportFitData ?: project
            "<!-- SVG export not yet implemented for Android/iOS -->".encodeToByteArray()
        },
        // AI text import
        showAiTextImport = state.showAiTextImportDialog,
        onDismissAiTextImport = {
            state.showAiTextImportDialog = false
            state.pendingAiTextImportCallback = null
        },
        onAiTextImportResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = state.pendingAiTextImportCallback
            if (bytes != null) {
                // Check if file is PDF (starts with %PDF-)
                val isPdf = bytes.size >= 5 && 
                           bytes[0] == '%'.code.toByte() &&
                           bytes[1] == 'P'.code.toByte() &&
                           bytes[2] == 'D'.code.toByte() &&
                           bytes[3] == 'F'.code.toByte() &&
                           bytes[4] == '-'.code.toByte()
                
                if (isPdf) {
                    println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected PDF format, extracting text")
                    
                    // Show progress dialog
                    state.showAiImportProgress = true
                    state.aiImportProgressMessage = "Extracting text from PDF..."
                    
                    // Extract text from PDF in background
                    scope.launch {
                        try {
                            val pdfExtractor = com.family.tree.core.platform.PdfTextExtractor(platformContext)
                            val extractedText = pdfExtractor.extractText(bytes)
                            
                            if (extractedText.isNullOrBlank()) {
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: PDF text extraction failed or returned empty text")
                                
                                // Show error dialog
                                state.showAiImportProgress = false
                                state.aiImportInfoMessage = """
                                    Failed to extract text from PDF file.
                                    
                                    Possible reasons:
                                    - PDF contains only images (scanned document)
                                    - PDF is copy-protected
                                    - PDF is corrupted or has an unsupported format
                                    
                                    Please try:
                                    1. Open docs.google.com in your browser
                                    2. File → Download → Plain Text (.txt)
                                    3. Import the downloaded .txt file
                                """.trimIndent()
                                state.showAiImportInfoDialog = true
                            } else {
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Successfully extracted ${extractedText.length} chars from PDF")
                                
                                // Process extracted text
                                state.aiImportProgressMessage = "Processing text..."
                                
                                // Load saved AI settings
                                val storage = com.family.tree.core.ai.AiSettingsStorage()
                                val config = storage.loadConfig()
                                
                                // Determine file type and process
                                val importer = com.family.tree.core.ai.AiTextImporter(config)
                                val imported = if (extractedText.trimStart().startsWith("{") || extractedText.trimStart().startsWith("[")) {
                                    // JSON format - parse directly
                                    println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected JSON format in PDF")
                                    state.aiImportProgressMessage = "Processing JSON..."
                                    importer.importFromAiResult(extractedText)
                                } else {
                                    // Plain text - call AI
                                    println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected plain text in PDF, calling AI...")
                                    state.aiImportProgressMessage = "Sending request to AI (${config.model})..."
                                    importer.importFromText(extractedText)
                                }
                                
                                state.aiImportProgressMessage = "Creating family tree..."
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Success - ${imported.data.individuals.size} individuals, ${imported.data.families.size} families")
                                
                                // Hide progress dialog
                                state.showAiImportProgress = false
                                
                                setLoadedProjectTemp(imported)
                                callback?.invoke(imported)
                                
                                // Reset dialog state
                                state.showAiTextImportDialog = false
                                state.pendingAiTextImportCallback = null
                            }
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: ERROR extracting/processing PDF - ${e.message}")
                            e.printStackTrace()
                            
                            // Hide progress dialog and show error
                            state.showAiImportProgress = false
                            state.aiImportInfoMessage = """
                                Ошибка при обработке PDF файла: ${e.message ?: "Неизвестная ошибка"}
                                
                                Пожалуйста, попробуйте:
                                1. Откройте docs.google.com в браузере
                                2. Файл → Скачать → Обычный текст (.txt)
                                3. Импортируйте полученный .txt файл
                            """.trimIndent()
                            state.showAiImportInfoDialog = true
                        }
                    }
                } else {
                    // Show progress dialog
                    state.showAiImportProgress = true
                    state.aiImportProgressMessage = "Reading file..."
                    
                    // Process in background
                    scope.launch {
                        try {
                            val content = bytes.decodeToString()
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Read ${content.length} chars")
                            
                            // Load saved AI settings
                            val storage = com.family.tree.core.ai.AiSettingsStorage()
                            val config = storage.loadConfig()
                            
                            // Determine file type and process
                            val importer = com.family.tree.core.ai.AiTextImporter(config)
                            val imported = if (content.trimStart().startsWith("{") || content.trimStart().startsWith("[")) {
                                // JSON format - parse directly
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected JSON format")
                                state.aiImportProgressMessage = "Processing JSON..."
                                importer.importFromAiResult(content)
                            } else {
                                // Plain text - call AI
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected plain text, calling AI...")
                                state.aiImportProgressMessage = "Sending request to AI (${config.model})..."
                                importer.importFromText(content)
                            }
                            
                            state.aiImportProgressMessage = "Creating family tree..."
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Success - ${imported.data.individuals.size} individuals, ${imported.data.families.size} families")
                            
                            // Hide progress dialog
                            state.showAiImportProgress = false
                            
                            setLoadedProjectTemp(imported)
                            callback?.invoke(imported)
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: ERROR - ${e.message}")
                            e.printStackTrace()
                            
                            // Hide progress dialog
                            state.showAiImportProgress = false
                            
                            callback?.invoke(null)
                        }
                    }
                    // Reset only after processing text/JSON file
                    state.showAiTextImportDialog = false
                    state.pendingAiTextImportCallback = null
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: bytes is null")
                callback?.invoke(null)
                state.showAiTextImportDialog = false
                state.pendingAiTextImportCallback = null
            }
        }
    )
}
