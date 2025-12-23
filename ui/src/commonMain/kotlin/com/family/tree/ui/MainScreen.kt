@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.family.tree.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.io.RelRepository
import com.family.tree.core.io.RelImporter
import com.family.tree.core.layout.SimpleTreeLayout
import com.family.tree.core.model.Individual
import com.family.tree.core.model.IndividualId
import com.family.tree.core.model.FamilyId
import com.family.tree.core.sample.SampleData
import com.family.tree.core.platform.FileGateway
import com.family.tree.core.search.QuickSearchService
import com.family.tree.ui.render.TreeRenderer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MainScreen() {
    // Get platform context (Android Context or null on other platforms)
    val platformContext = rememberPlatformContext()
    
    // Project state: start from sample data with layout
    val loadedSample = remember { SampleData.simpleThreeGenWithLayout() }
    var project by remember {
        println("[DEBUG_LOG] MainScreen: Initializing project state with ${loadedSample.data.individuals.size} individuals")
        mutableStateOf(loadedSample.data)
    }
    println("[DEBUG_LOG] MainScreen: Current project state has ${project.individuals.size} individuals, ${project.families.size} families")
    var projectLayout by remember { mutableStateOf<com.family.tree.core.layout.ProjectLayout?>(loadedSample.layout) }

    // Search service and state
    val searchService = remember(project) { QuickSearchService(project) }
    var individualsSearchQuery by remember { mutableStateOf("") }
    var familiesSearchQuery by remember { mutableStateOf("") }

    // Selection state (supports multi-select for family highlighting)
    var selectedIds by remember { mutableStateOf<Set<IndividualId>>(emptySet()) }
    var selectedFamilyId by remember { mutableStateOf<FamilyId?>(null) }

    // Viewport state (simplified without Animatable to unblock compile)
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Memoize layout computation - only recompute when data changes (same as TreeRenderer)
    val cachedPositions = remember(project.individuals, project.families) {
        SimpleTreeLayout.layout(project.individuals, project.families).also {
            println("[DEBUG_LOG] MainScreen: computed ${it.size} cached positions")
        }
    }

    // Track when to auto-fit view after import
    var shouldAutoFit by remember { mutableStateOf(false) }

    // Coroutine scope for throttling centerOn operations
    val coroutineScope = rememberCoroutineScope()
    var centerOnThrottleJob by remember { mutableStateOf<Job?>(null) }
    var pendingCenterOnRequest by remember { mutableStateOf<Pair<Individual, Map<IndividualId, Offset>>?>(null) }

    fun clampScale(s: Float) = s.coerceIn(0.25f, 4f)

    suspend fun setScaleImmediate(value: Float) { scale = clampScale(value) }
    fun setScaleAnimated(value: Float) { scale = clampScale(value) }

    fun fitToView() {
        val positions = cachedPositions
        if (positions.isEmpty() || canvasSize.width == 0 || canvasSize.height == 0) {
            scale = 1f
            // animate to zero pan
            pan = Offset.Zero
            return
        }
        val xs = positions.values.map { it.x }
        val ys = positions.values.map { it.y }
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        // Node size in layout units (matches SimpleTreeLayout default 120x60)
        val nodeW = 120f
        val nodeH = 60f
        val contentW = (maxX - minX + nodeW)
        val contentH = (maxY - minY + nodeH)
        val margin = 48f
        val sx = (canvasSize.width - 2 * margin) / max(1f, contentW)
        val sy = (canvasSize.height - 2 * margin) / max(1f, contentH)
        val s = clampScale(min(sx, sy))
        scale = s
        // place top-left (minX,minY) at (margin, margin)
        val target = Offset(
            x = margin - minX * s,
            y = margin - minY * s
        )
        pan = target
    }

    fun centerOnImmediate(ind: Individual, customOffsets: Map<IndividualId, Offset>) {
        // Use custom offsets from REL layout if available, otherwise use computed positions
        val (posX, posY) = if (ind.id in customOffsets) {
            val offset = customOffsets[ind.id]!!
            offset.x to offset.y
        } else {
            val vec2 = cachedPositions[ind.id] ?: return
            vec2.x to vec2.y
        }
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        val cx = canvasSize.width / 2f
        val cy = canvasSize.height / 2f
        val nodeW = 120f * scale
        val nodeH = 60f * scale
        val targetX = posX * scale + nodeW / 2
        val targetY = posY * scale + nodeH / 2
        val newPan = Offset(cx - targetX, cy - targetY)
        
        // Update pan directly - optimization happens in TreeRenderer via viewport culling
        // For large trees, only visible nodes recompose (see TreeRenderer.kt:217-223)
        pan = newPan
        
        println("[DEBUG_LOG] MainScreen.centerOn: centered on ${ind.displayName}, new pan=$newPan")
    }

    fun centerOnFamilyMembersImmediate(memberIds: Set<IndividualId>, customOffsets: Map<IndividualId, Offset>) {
        if (memberIds.isEmpty()) return
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        
        // Collect positions for all family members (both from customOffsets and cachedPositions)
        var sumX = 0f
        var sumY = 0f
        var count = 0
        
        for (id in memberIds) {
            val (posX, posY) = if (id in customOffsets) {
                val offset = customOffsets[id]!!
                offset.x to offset.y
            } else {
                val vec2 = cachedPositions[id] ?: continue
                vec2.x to vec2.y
            }
            sumX += posX
            sumY += posY
            count++
        }
        
        if (count == 0) return
        
        // Calculate average position (center of family members)
        val avgX = sumX / count
        val avgY = sumY / count
        
        val cx = canvasSize.width / 2f
        val cy = canvasSize.height / 2f
        val nodeW = 120f * scale
        val nodeH = 60f * scale
        val targetX = avgX * scale + nodeW / 2
        val targetY = avgY * scale + nodeH / 2
        val newPan = Offset(cx - targetX, cy - targetY)
        
        pan = newPan
        
        println("[DEBUG_LOG] MainScreen.centerOnFamilyMembers: centered on $count family members at avg position ($avgX, $avgY), new pan=$newPan")
    }
    
    // Throttled centerOn to prevent ANR when rapidly selecting persons in large trees
    fun centerOn(ind: Individual, customOffsets: Map<IndividualId, Offset>) {
        // Store pending request
        pendingCenterOnRequest = Pair(ind, customOffsets)
        
        // Cancel any existing throttle job
        centerOnThrottleJob?.cancel()
        
        // Schedule new throttle job with 100ms delay to batch rapid selections
        centerOnThrottleJob = coroutineScope.launch {
            delay(100)
            pendingCenterOnRequest?.let { (individual, offsets) ->
                centerOnImmediate(individual, offsets)
                pendingCenterOnRequest = null
            }
        }
    }

    // Auto-fit view after import when positions are ready
    LaunchedEffect(cachedPositions, shouldAutoFit, canvasSize) {
        println("[DEBUG_LOG] MainScreen: LaunchedEffect block running - shouldAutoFit=$shouldAutoFit, positions=${cachedPositions.size}, canvasSize=$canvasSize")
        if (shouldAutoFit && cachedPositions.isNotEmpty() && canvasSize != IntSize.Zero) {
            println("[DEBUG_LOG] MainScreen: Conditions met, calling fitToView()")
            // Add a small delay to ensure canvas is fully initialized
            kotlinx.coroutines.delay(50)
            println("[DEBUG_LOG] MainScreen: After delay, calling fitToView() with ${cachedPositions.size} positions, canvasSize=$canvasSize")
            fitToView()
            shouldAutoFit = false
            println("[DEBUG_LOG] MainScreen: fitToView() completed, shouldAutoFit set to false")
        } else {
            println("[DEBUG_LOG] MainScreen: Conditions NOT met - skipping fitToView()")
        }
    }

    // Person editor dialog state
    var editPersonId by remember { mutableStateOf<IndividualId?>(null) }
    // Family editor dialog state
    var editFamilyId by remember { mutableStateOf<FamilyId?>(null) }
    // Sources manager dialog state
    var showSourcesDialog by remember { mutableStateOf(false) }
    // About dialog state
    var showAboutDialog by remember { mutableStateOf(false) }
    // AI Settings dialog state
    var showAiSettingsDialog by remember { mutableStateOf(false) }

    // File dialog state for Android/platform file pickers
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingOpenCallback by remember { mutableStateOf<((LoadedProject?) -> Unit)?>(null) }
    var pendingSaveData by remember { mutableStateOf<ProjectData?>(null) }
    
    // .rel import dialog state
    var showRelImportDialog by remember { mutableStateOf(false) }
    var pendingRelImportCallback by remember { mutableStateOf<((LoadedProject?) -> Unit)?>(null) }
    
    // .rel import progress state
    var showRelImportProgress by remember { mutableStateOf(false) }
    var relImportProgressMessage by remember { mutableStateOf("") }
    
    // .rel import error state
    var showRelImportError by remember { mutableStateOf(false) }
    var relImportErrorMessage by remember { mutableStateOf("") }
    
    // GEDCOM dialog state
    var showGedcomImportDialog by remember { mutableStateOf(false) }
    var showGedcomExportDialog by remember { mutableStateOf(false) }
    var pendingGedcomImportCallback by remember { mutableStateOf<((ProjectData?) -> Unit)?>(null) }
    var pendingGedcomExportData by remember { mutableStateOf<ProjectData?>(null) }
    
    // SVG export dialog state
    var showSvgExportDialog by remember { mutableStateOf(false) }
    var showSvgExportFitDialog by remember { mutableStateOf(false) }
    var pendingSvgExportData by remember { mutableStateOf<Triple<ProjectData, Float, Offset>?>(null) }
    var pendingSvgExportFitData by remember { mutableStateOf<ProjectData?>(null) }
    
    // AI text import dialog state
    var showAiTextImportDialog by remember { mutableStateOf(false) }
    var pendingAiTextImportCallback by remember { mutableStateOf<((LoadedProject?) -> Unit)?>(null) }
    
    // AI import progress state
    var showAiImportProgress by remember { mutableStateOf(false) }
    var aiImportProgressMessage by remember { mutableStateOf("") }
    
    // AI import info dialog state
    var showAiImportInfoDialog by remember { mutableStateOf(false) }
    var aiImportInfoMessage by remember { mutableStateOf("") }
    var isPermissionError by remember { mutableStateOf(false) }
    
    // Voice input state
    var isVoiceRecording by remember { mutableStateOf(false) }
    var voiceInputStatus by remember { mutableStateOf("") }
    var showVoiceInputDialog by remember { mutableStateOf(false) }
    
    // Store loaded project temporarily to ensure state update happens in composition scope
    var loadedProjectTemp by remember { mutableStateOf<LoadedProject?>(null) }

    // Coroutine scope for AI operations
    val scope = rememberCoroutineScope()
    
    // Voice input processor
    val voiceInputProcessor = remember {
        val voiceRecorder = com.family.tree.core.platform.VoiceRecorder(platformContext)
        com.family.tree.core.ai.VoiceInputProcessor(voiceRecorder, scope)
    }
    
    // Keyboard focus & modifiers
    val focusRequester = remember { FocusRequester() }
    var isSpacePressed by remember { mutableStateOf(false) }

    // Auto-focus only on desktop to avoid keyboard popup on mobile
    LaunchedEffect(Unit) { 
        if (PlatformEnv.isDesktop) {
            focusRequester.requestFocus()
        }
    }

    // Wire global app actions for Desktop menu and other entry points
    AppActions.newProject = {
        println("[DEBUG_LOG] MainScreen.newProject: Creating new empty project")
        // Create empty project
        project = ProjectData(
            individuals = emptyList(),
            families = emptyList(),
            sources = emptyList()
        )
        projectLayout = null
        selectedIds = emptySet()
        selectedFamilyId = null
        // Reset viewport
        scale = 1f
        pan = Offset.Zero
        println("[DEBUG_LOG] MainScreen.newProject: New project created")
    }
    AppActions.openPed = {

        DesktopActions.openPed { loaded ->
            println("[DEBUG_LOG] MainScreen.openPed callback: loaded=$loaded")
            if (loaded != null) {
                println("[DEBUG_LOG] MainScreen.openPed: Received data with ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                project = loaded.data
                projectLayout = loaded.layout
                println("[DEBUG_LOG] MainScreen.openPed: Updated project state, layout has ${loaded.layout?.nodePositions?.size ?: 0} node positions")
                selectedIds = emptySet()
                // Apply viewport from layout if present; otherwise fit to content
                val layout = loaded.layout
                if (layout != null && (layout.zoom != 1.0 || layout.viewOriginX != 0.0 || layout.viewOriginY != 0.0)) {
                    println("[DEBUG_LOG] MainScreen.openPed: Applying viewport from layout - zoom=${layout.zoom}, origin=(${layout.viewOriginX}, ${layout.viewOriginY})")
                    scale = layout.zoom.toFloat()
                    pan = Offset(layout.viewOriginX.toFloat(), layout.viewOriginY.toFloat())
                } else {
                    println("[DEBUG_LOG] MainScreen.openPed: No layout viewport, calling fitToView()")
                    fitToView()
                }
                println("[DEBUG_LOG] MainScreen.openPed: Final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}, scale=$scale")
            } else {
                println("[DEBUG_LOG] MainScreen.openPed: loaded is null (user cancelled or error)")
            }
        }
    }
    AppActions.importRel = {
        DesktopActions.importRel { loaded ->
            println("[DEBUG_LOG] MainScreen.importRel callback: loaded=$loaded")
            if (loaded != null) {
                println("[DEBUG_LOG] MainScreen.importRel: Received data with ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                project = loaded.data
                projectLayout = loaded.layout
                println("[DEBUG_LOG] MainScreen.importRel: Updated project state, layout has ${loaded.layout?.nodePositions?.size ?: 0} node positions")
                selectedIds = emptySet()
                shouldAutoFit = true
                println("[DEBUG_LOG] MainScreen.importRel: Set shouldAutoFit=true, final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}")
            } else {
                println("[DEBUG_LOG] MainScreen.importRel: loaded is null (user cancelled or error)")
            }
        }
    }
    AppActions.importGedcom = {
        DesktopActions.importGedcom { data ->
            println("[DEBUG_LOG] MainScreen.importGedcom callback: data=$data")
            if (data != null) {
                println("[DEBUG_LOG] MainScreen.importGedcom: Received data with ${data.individuals.size} individuals, ${data.families.size} families")
                project = data
                projectLayout = null
                println("[DEBUG_LOG] MainScreen.importGedcom: Updated project state")
                selectedIds = emptySet()
                fitToView()
                println("[DEBUG_LOG] MainScreen.importGedcom: Final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}")
            } else {
                println("[DEBUG_LOG] MainScreen.importGedcom: data is null (user cancelled or error)")
            }
        }
    }
    AppActions.importAiText = {
        // Show progress dialog
        showAiImportProgress = true
        aiImportProgressMessage = "Подготовка..."
        
        DesktopActions.importAiText(
            onLoaded = { loaded ->
                println("[DEBUG_LOG] MainScreen.importAiText callback: loaded=$loaded")
                // Hide progress dialog
                showAiImportProgress = false
                
                if (loaded != null) {
                    println("[DEBUG_LOG] MainScreen.importAiText: Received data with ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                    project = loaded.data
                    projectLayout = loaded.layout
                    println("[DEBUG_LOG] MainScreen.importAiText: Updated project state")
                    selectedIds = emptySet()
                    fitToView()
                    println("[DEBUG_LOG] MainScreen.importAiText: Final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}")
                } else {
                    println("[DEBUG_LOG] MainScreen.importAiText: loaded is null (user cancelled or error)")
                }
            },
            onProgress = { message ->
                println("[DEBUG_LOG] MainScreen.importAiText progress: $message")
                aiImportProgressMessage = message
            }
        )
    }
    AppActions.voiceInput = {
        run {
            if (!voiceInputProcessor.isVoiceInputAvailable()) {
                aiImportInfoMessage = "Голосовой ввод недоступен на этой платформе"
                showAiImportInfoDialog = true
                return@run
            }
            
            if (voiceInputProcessor.isRecording()) {
                // Already recording, stop it
                voiceInputProcessor.stopRecording()
                isVoiceRecording = false
                voiceInputStatus = ""
                return@run
            }
            
            // Загружаем сохраненные настройки и сразу начинаем запись
            println("[DEBUG_LOG] MainScreen.voiceInput: Starting voice input with saved settings")
            isVoiceRecording = true
            voiceInputStatus = "Speak..."
            showVoiceInputDialog = true
            
            voiceInputProcessor.startVoiceInput(
                onSuccess = { loaded ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Success - ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                    isVoiceRecording = false
                    showVoiceInputDialog = false
                    voiceInputStatus = ""
                    
                    // Update project
                    project = loaded.data
                    projectLayout = loaded.layout
                    selectedIds = emptySet()
                    fitToView()
                },
                onError = { errorMessage ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Error - $errorMessage")
                    isVoiceRecording = false
                    voiceInputStatus = ""
                    showVoiceInputDialog = false
                    
                    // Check if it's a permission error
                    isPermissionError = errorMessage.contains("разрешений") || errorMessage.contains("RECORD_AUDIO")
                    
                    // Show error dialog
                    aiImportInfoMessage = "Voice input error:\n$errorMessage"
                    showAiImportInfoDialog = true
                },
                onRecognized = { recognizedText ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Recognized - $recognizedText")
                    voiceInputStatus = "Recognized: \"$recognizedText\"\nProcessing via AI..."
                }
            )
        }
    }
    AppActions.exportGedcom = { DesktopActions.exportGedcom(project) }
    AppActions.exportSvgCurrent = { DesktopActions.exportSvg(project, scale = scale, pan = pan) }
    AppActions.exportSvgFit = { DesktopActions.exportSvgFit(project) }
    AppActions.exportPngCurrent = { DesktopActions.exportPng(project, scale = scale, pan = pan) }
    AppActions.exportPngFit = { DesktopActions.exportPngFit(project) }
    AppActions.zoomIn = { setScaleAnimated(scale * 1.1f) }
    AppActions.zoomOut = { setScaleAnimated(scale * 0.9f) }
    AppActions.reset = { fitToView() }
    AppActions.manageSources = { showSourcesDialog = true }
    AppActions.showAbout = { showAboutDialog = true }
    AppActions.showAiSettings = { showAiSettingsDialog = true }

    // Wire dialog actions for platform file dialogs (used by Android DesktopActions)
    DialogActions.triggerOpenDialog = { callback ->
        pendingOpenCallback = callback
        showOpenDialog = true
    }
    DialogActions.triggerSaveDialog = { data ->
        pendingSaveData = data
        showSaveDialog = true
    }
    DialogActions.triggerRelImport = { callback ->
        pendingRelImportCallback = callback
        showRelImportDialog = true
    }
    DialogActions.triggerGedcomImport = { callback ->
        pendingGedcomImportCallback = callback
        showGedcomImportDialog = true
    }
    DialogActions.triggerGedcomExport = { data ->
        pendingGedcomExportData = data
        showGedcomExportDialog = true
    }
    DialogActions.triggerSvgExport = { data, scale, pan ->
        pendingSvgExportData = Triple(data, scale, pan)
        showSvgExportDialog = true
    }
    DialogActions.triggerSvgExportFit = { data ->
        pendingSvgExportFitData = data
        showSvgExportFitDialog = true
    }
    DialogActions.triggerAiTextImport = { callback ->
        pendingAiTextImportCallback = callback
        showAiTextImportDialog = true
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .platformKeyboardShortcuts(
                    onEscape = { selectedIds = emptySet() },
                    onZoomIn = { setScaleAnimated(scale * 1.1f) },
                    onZoomOut = { setScaleAnimated(scale * 0.9f) }
                )
        ) {
            // Top toolbar
            if (!PlatformEnv.isDesktop) {
                var showMenu by remember { mutableStateOf(false) }
                var showImportMenu by remember { mutableStateOf(false) }
                var shouldExit by remember { mutableStateOf(false) }
                androidx.compose.material3.TopAppBar(
                    title = { Text("Family Tree Editor") },
                    actions = {
                        androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.MoreVert,
                                contentDescription = "More"
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("New") }, onClick = { showMenu = false; AppActions.newProject() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; AppActions.openPed() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Save") }, onClick = { showMenu = false; AppActions.savePed() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Import...") }, onClick = { showMenu = false; showImportMenu = true })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Import AI Text") }, onClick = { showMenu = false; AppActions.importAiText() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Voice Input 🎤") }, onClick = { showMenu = false; AppActions.voiceInput() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export GEDCOM") }, onClick = { showMenu = false; AppActions.exportGedcom() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export SVG (Current)") }, onClick = { showMenu = false; AppActions.exportSvgCurrent() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export SVG (Fit)") }, onClick = { showMenu = false; AppActions.exportSvgFit() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Manage Sources...") }, onClick = { showMenu = false; AppActions.manageSources() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("AI Settings...") }, onClick = { showMenu = false; AppActions.showAiSettings() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("About") }, onClick = { showMenu = false; AppActions.showAbout() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Exit") }, onClick = { showMenu = false; shouldExit = true })
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text(".rel") }, onClick = { showImportMenu = false; AppActions.importRel() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("GEDCOM") }, onClick = { showImportMenu = false; AppActions.importGedcom() })
                        }
                    }
                )
                if (shouldExit) {
                    ExitAppAction(onExit = { AppActions.exit() })
                }
            } else {
                // Desktop: Simple toolbar with title and zoom (menu is in native MenuBar)
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Family Tree Editor", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${(scale * 100).toInt()}%", modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                }
            }

            // Node-level offsets to support dragging individual nodes (JavaFX-like)
            // Initialize from ProjectLayout coordinates if available (for REL format)
            // NOTE: Only depend on projectLayout, NOT on project, to preserve manual position changes
            // when individuals are added/removed
            val nodeOffsets = remember(projectLayout) {
                mutableStateMapOf<IndividualId, Offset>().apply {
                    projectLayout?.nodePositions?.forEach { (idString, nodePos) ->
                        val id = IndividualId(idString)
                        put(id, Offset(nodePos.x.toFloat(), nodePos.y.toFloat()))
                    }
                }
            }

            // Wire savePed action after nodeOffsets is defined
            AppActions.savePed = {
                // Build ProjectLayout from current nodeOffsets, scale and pan
                val layout = com.family.tree.core.layout.ProjectLayout(
                    zoom = scale.toDouble(),
                    viewOriginX = pan.x.toDouble(),
                    viewOriginY = pan.y.toDouble(),
                    nodePositions = nodeOffsets.mapKeys { it.key.value }.mapValues { 
                        com.family.tree.core.layout.NodePos(it.value.x.toDouble(), it.value.y.toDouble()) 
                    }
                )
                DesktopActions.savePedWithLayout(project, layout)
            }

            // Main content: left list, center canvas, right inspector
            // Left panel width for correct zoom cursor offset calculation
            val leftPanelWidthPx = with(LocalDensity.current) { 260.dp.toPx() }
            
            Row(Modifier
                .fillMaxSize()
                // Touch zoom/pan for mobile: apply to entire Row so gestures work on side panels too
                .platformWheelZoom(
                    getScale = { scale },
                    setScale = { setScaleAnimated(it) },
                    getPan = { pan },
                    setPan = { pan = it },
                    getCanvasSize = { canvasSize },
                    leftPanelWidth = leftPanelWidthPx
                )
            ) {
                // Left panel: Individuals/Families tabs (like legacy JavaFX)
                Box(
                    modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0x0D000000)),
                    contentAlignment = Alignment.TopStart
                ) {
                Column(Modifier.fillMaxSize()) {
                    var leftTab by remember { mutableStateOf(0) }
                    androidx.compose.material3.TabRow(selectedTabIndex = leftTab) {
                        androidx.compose.material3.Tab(
                            selected = leftTab == 0,
                            onClick = { leftTab = 0 },
                            text = { Text("Individuals") }
                        )
                        androidx.compose.material3.Tab(
                            selected = leftTab == 1,
                            onClick = { leftTab = 1 },
                            text = { Text("Families") }
                        )
                    }
                    when (leftTab) {
                        0 -> {
                            println("[DEBUG_LOG] MainScreen: Rendering individuals list with ${project.individuals.size} items")
                            Column(Modifier.fillMaxSize().padding(12.dp)) {
                                // Search field for individuals
                                OutlinedTextField(
                                    value = individualsSearchQuery,
                                    onValueChange = { individualsSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    placeholder = { Text("Filter by name or tag...") },
                                    singleLine = true
                                )
                                
                                // Filter individuals based on search query
                                val filteredIndividuals = remember(individualsSearchQuery, project.individuals) {
                                    if (individualsSearchQuery.isBlank()) {
                                        project.individuals
                                    } else {
                                        searchService.findIndividualsByName(individualsSearchQuery)
                                    }
                                }
                                
                                LazyColumn(Modifier.weight(1f)) {
                                    items(filteredIndividuals) { ind ->
                                        val isSelected = selectedIds.contains(ind.id)
                                        val bg = if (isSelected) Color(0x201976D2) else Color.Unspecified
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bg)
                                                .combinedClickable(
                                                    onClick = {
                                                        selectedFamilyId = null
                                                        selectedIds = setOf(ind.id)
                                                        centerOn(ind, nodeOffsets)
                                                    },
                                                    onDoubleClick = { editPersonId = ind.id }
                                                )
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        ) {
                                            Text(
                                                "• ${ind.displayName}",
                                                color = if (isSelected) Color(0xFF0D47A1) else Color.Unspecified
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(Modifier.fillMaxSize().padding(12.dp)) {
                                // Helper to format spouse name as "LastName F." (JavaFX style)
                                fun formatSpouseName(indId: IndividualId?): String {
                                    if (indId == null) return ""
                                    val ind = project.individuals.find { it.id == indId } ?: return indId.value
                                    
                                    // Extract first and last name
                                    val lastName = ind.lastName?.trim() ?: ""
                                    val firstName = ind.firstName?.trim() ?: ""
                                    
                                    // Format as "LastName F." (JavaFX style)
                                    val sb = StringBuilder()
                                    if (lastName.isNotEmpty()) sb.append(lastName)
                                    if (firstName.isNotEmpty()) {
                                        if (sb.isNotEmpty()) sb.append(' ')
                                        sb.append(firstName.first().uppercaseChar()).append('.')
                                    }
                                    val result = sb.toString()
                                    return if (result.isBlank()) indId.value else result
                                }
                                
                                // Search field for families
                                OutlinedTextField(
                                    value = familiesSearchQuery,
                                    onValueChange = { familiesSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    placeholder = { Text("Filter by name or tag...") },
                                    singleLine = true
                                )
                                
                                // Filter families based on search query
                                val filteredFamilies = remember(familiesSearchQuery, project.families, project.individuals) {
                                    if (familiesSearchQuery.isBlank()) {
                                        project.families
                                    } else {
                                        searchService.findFamiliesBySpouseName(familiesSearchQuery)
                                    }
                                }
                                
                                // Column headers (JavaFX style)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Husband",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Wife",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Child",
                                        modifier = Modifier.width(40.dp),
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                LazyColumn(Modifier.weight(1f)) {
                                    items(filteredFamilies) { fam ->
                                        val husbandName = formatSpouseName(fam.husbandId)
                                        val wifeName = formatSpouseName(fam.wifeId)
                                        val childrenCount = fam.childrenIds.size.toString()
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {
                                                        // Select the family and highlight all family members
                                                        selectedFamilyId = fam.id
                                                        val familyMemberIds = mutableSetOf<IndividualId>()
                                                        fam.husbandId?.let { familyMemberIds.add(it) }
                                                        fam.wifeId?.let { familyMemberIds.add(it) }
                                                        familyMemberIds.addAll(fam.childrenIds)
                                                        selectedIds = familyMemberIds
                                                        // Center canvas on average position of all family members
                                                        centerOnFamilyMembersImmediate(familyMemberIds, nodeOffsets)
                                                    },
                                                    onDoubleClick = { editFamilyId = fam.id }
                                                )
                                                .padding(vertical = 4.dp, horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = husbandName,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = wifeName,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = childrenCount,
                                                modifier = Modifier.width(40.dp),
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Center canvas with panning/zoom
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { canvasSize = it }
                ) {
                    TreeRenderer(
                        data = project,
                        selectedIds = selectedIds,
                        onSelect = { id -> selectedIds = if (id != null) setOf(id) else emptySet() },
                        onEditPerson = { id -> editPersonId = id },
                        onCenterOn = { id ->
                            project.individuals.find { it.id == id }?.let { centerOn(it, nodeOffsets) }
                        },
                        onReset = { fitToView() },
                        onNodeDrag = { id, delta ->
                            val cur = nodeOffsets[id] ?: Offset.Zero
                            nodeOffsets[id] = cur + delta
                        },
                        nodeOffsets = nodeOffsets,
                        scale = scale,
                        pan = pan,
                        setPan = { pan = it },
                        onCanvasSize = { canvasSize = it },
                        onAddPerson = { clickPosition ->
                            // Generate new unique ID
                            val existingIds = project.individuals.map { it.id.value }
                            var newIdNum = 1
                            while (existingIds.contains("I$newIdNum")) {
                                newIdNum++
                            }
                            val newIndividual = Individual(
                                id = IndividualId("I$newIdNum"),
                                firstName = "New",
                                lastName = "Person"
                            )
                            // Convert screen coordinates to layout space
                            // nodeOffsets stores positions in layout space (unscaled)
                            // TreeRenderer multiplies by scale when rendering: baseX = off.x * scale
                            // So we need to store unscaled coordinates in nodeOffsets
                            val layoutPosition = Offset(
                                x = (clickPosition.x - pan.x) / scale,
                                y = (clickPosition.y - pan.y) / scale
                            )
                            // Set position for new individual in nodeOffsets (layout space)
                            nodeOffsets[newIndividual.id] = layoutPosition
                            // Update project with new individual
                            project = project.copy(
                                individuals = project.individuals + newIndividual
                            )
                            selectedIds = setOf(newIndividual.id)
                            editPersonId = newIndividual.id
                        },
                        onDeletePerson = { idToDelete ->
                            // Remove individual and related data
                            project = project.copy(
                                individuals = project.individuals.filter { it.id != idToDelete },
                                families = project.families.map { family ->
                                    family.copy(
                                        husbandId = if (family.husbandId == idToDelete) null else family.husbandId,
                                        wifeId = if (family.wifeId == idToDelete) null else family.wifeId,
                                        childrenIds = family.childrenIds.filter { it != idToDelete }
                                    )
                                }
                            )
                            selectedIds = emptySet()
                        }
                    )
                }

                // Right inspector (scrollable)
                Box(
                    modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0x0D000000)),
                    contentAlignment = Alignment.TopStart
                ) {
                // Show properties for selected family or individual
                val selectedFamily = selectedFamilyId?.let { id -> project.families.find { it.id == id } }
                if (selectedFamily != null) {
                    // Show family properties
                    PropertiesInspector(
                        family = selectedFamily,
                        project = project,
                        onUpdateFamily = { updated ->
                            val idx = project.families.indexOfFirst { it.id == updated.id }
                            if (idx >= 0) {
                                project = project.copy(
                                    families = project.families.toMutableList().also { it[idx] = updated }
                                )
                            }
                        }
                    )
                } else {
                    // Show individual properties
                    val selected = selectedIds.firstOrNull()?.let { id -> project.individuals.find { it.id == id } }
                    if (selected == null) {
                        Column(Modifier.padding(12.dp).fillMaxWidth()) {
                            Text("Properties\nSelect a person or family…")
                        }
                    } else {
                        PropertiesInspector(
                            individual = selected,
                            project = project,
                            onUpdateIndividual = { updated ->
                                val idx = project.individuals.indexOfFirst { it.id == updated.id }
                                if (idx >= 0) {
                                    project = project.copy(
                                        individuals = project.individuals.toMutableList().also { it[idx] = updated }
                                    )
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }
    // Person editor dialog
    run {
        val person = project.individuals.find { it.id == editPersonId }
        if (person != null) {
            PersonEditorDialog(
                person = person,
                onSave = { updated ->
                    val idx = project.individuals.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) {
                        project = project.copy(
                            individuals = project.individuals.toMutableList().also { it[idx] = updated }
                        )
                    }
                    editPersonId = null
                },
                onDismiss = { editPersonId = null }
            )
        }
    }
    // Family editor dialog
    run {
        val fam = project.families.find { it.id == editFamilyId }
        if (fam != null) {
            FamilyEditorDialog(
                family = fam,
                allIndividuals = project.individuals,
                onSave = { updated ->
                    val idx = project.families.indexOfFirst { it.id == updated.id }
                    if (idx >= 0) {
                        project = project.copy(
                            families = project.families.toMutableList().also { it[idx] = updated }
                        )
                    }
                    editFamilyId = null
                },
                onDismiss = { editFamilyId = null }
            )
        }
    }
    
    // Sources manager dialog
    if (showSourcesDialog) {
        SourcesManagerDialog(
            project = project,
            onDismiss = { showSourcesDialog = false },
            onUpdateProject = { updatedProject ->
                project = updatedProject
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    // AI Settings dialog
    if (showAiSettingsDialog) {
        val storage = remember { com.family.tree.core.ai.AiSettingsStorage() }
        val savedConfig = remember { storage.loadConfig() }
        
        AiConfigDialog(
            initialConfig = savedConfig,
            onDismiss = { showAiSettingsDialog = false },
            onConfirm = { config ->
                storage.saveConfig(config)
                showAiSettingsDialog = false
            }
        )
    }
    
    // AI Import Progress dialog
    if (showAiImportProgress) {
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
                        text = aiImportProgressMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // .rel Import Progress dialog
    if (showRelImportProgress) {
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
                        text = relImportProgressMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    // .rel Import Error dialog
    if (showRelImportError) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showRelImportError = false
                relImportErrorMessage = ""
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
                        text = relImportErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showRelImportError = false
                                relImportErrorMessage = ""
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
    if (showAiImportInfoDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showAiImportInfoDialog = false
                showAiTextImportDialog = false
                pendingAiTextImportCallback?.invoke(null)
                pendingAiTextImportCallback = null
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
                        text = aiImportInfoMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        // Show "Open Settings" button if it's a permission error
                        if (isPermissionError) {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    // Open app settings on Android
                                    val voiceRecorder = com.family.tree.core.platform.VoiceRecorder(platformContext)
                                    voiceRecorder.openAppSettings()
                                    showAiImportInfoDialog = false
                                    showAiTextImportDialog = false
                                    pendingAiTextImportCallback?.invoke(null)
                                    pendingAiTextImportCallback = null
                                    isPermissionError = false
                                }
                            ) {
                                Text("Open Settings")
                            }
                        }
                        
                        androidx.compose.material3.TextButton(
                            onClick = {
                                showAiImportInfoDialog = false
                                showAiTextImportDialog = false
                                pendingAiTextImportCallback?.invoke(null)
                                pendingAiTextImportCallback = null
                                isPermissionError = false
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
    if (showVoiceInputDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                voiceInputProcessor.cancelRecording()
                isVoiceRecording = false
                showVoiceInputDialog = false
                voiceInputStatus = ""
            },
            modifier = Modifier.widthIn(min = 400.dp),
            title = { Text("Voice Input") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isVoiceRecording) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    Text(
                        text = voiceInputStatus,
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
                    if (isVoiceRecording && voiceInputStatus == "Speak...") {
                        Button(
                            onClick = {
                                println("[DEBUG_LOG] MainScreen: User clicked 'Stop Recording' button")
                                voiceInputProcessor.stopRecording()
                                isVoiceRecording = false
                                voiceInputStatus = "Processing recording..."
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
                            isVoiceRecording = false
                            showVoiceInputDialog = false
                            voiceInputStatus = ""
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
            project = loaded.data
            projectLayout = loaded.layout
            selectedIds = emptySet()
            // Apply viewport from layout if present; otherwise fit to content
            val layout = loaded.layout
            if (layout != null && (layout.zoom != 1.0 || layout.viewOriginX != 0.0 || layout.viewOriginY != 0.0)) {
                scale = layout.zoom.toFloat()
                pan = Offset(layout.viewOriginX.toFloat(), layout.viewOriginY.toFloat())
            } else {
                fitToView()
            }
            println("[DEBUG_LOG] MainScreen LaunchedEffect: Applied project state - project.individuals=${project.individuals.size}")
            loadedProjectTemp = null  // Clear after applying
        }
    }
    
    PlatformFileDialogs(
        showOpen = showOpenDialog,
        onDismissOpen = {
            showOpenDialog = false
            pendingOpenCallback = null
        },
        onOpenResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onOpenResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = pendingOpenCallback
            println("[DEBUG_LOG] MainScreen.onOpenResult: callback=$callback")
            if (bytes != null) {
                // Show progress dialog
                showRelImportProgress = true
                relImportProgressMessage = "Opening file..."
                
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
                                        relImportProgressMessage = progress
                                    }
                                })
                            }.getOrNull()
                        }
                        println("[DEBUG_LOG] MainScreen.onOpenResult: loaded=$loaded, data has ${loaded?.data?.individuals?.size ?: 0} individuals")
                        
                        // Hide progress dialog
                        showRelImportProgress = false
                        
                        // Instead of invoking callback directly, store in temp state to trigger LaunchedEffect
                        if (loaded != null) {
                            loadedProjectTemp = loaded
                        }
                        callback?.invoke(loaded)
                        println("[DEBUG_LOG] MainScreen.onOpenResult: callback invoked")
                        
                        // Reset dialog state
                        showOpenDialog = false
                        pendingOpenCallback = null
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] MainScreen.onOpenResult: ERROR opening file - ${e.message}")
                        e.printStackTrace()
                        
                        // Hide progress dialog
                        showRelImportProgress = false
                        
                        callback?.invoke(null)
                        
                        // Reset dialog state
                        showOpenDialog = false
                        pendingOpenCallback = null
                    }
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onOpenResult: bytes is null, invoking callback with null")
                callback?.invoke(null)
                showOpenDialog = false
                pendingOpenCallback = null
            }
        },
        showSave = showSaveDialog,
        onDismissSave = {
            showSaveDialog = false
            pendingSaveData = null
        },
        bytesToSave = {
            val data = pendingSaveData ?: project
            RelRepository().write(data, projectLayout, null)
        },
        // .rel import
        showRelImport = showRelImportDialog,
        onDismissRelImport = {
            showRelImportDialog = false
            pendingRelImportCallback = null
        },
        onRelImportResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onRelImportResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = pendingRelImportCallback
            if (bytes != null) {
                // Show progress dialog
                showRelImportProgress = true
                relImportProgressMessage = "Preparing to import..."
                
                // Import in background to avoid blocking UI on Android TV
                scope.launch {
                    try {
                        // Give UI time to render the progress dialog
                        delay(100)
                        
                        relImportProgressMessage = "Importing .rel file..."
                        
                        val loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            runCatching {
                                // Use RelImporter for legacy .rel binary TLV format
                                println("[DEBUG_LOG] MainScreen.onRelImportResult: Using RelImporter for .rel format")
                                RelImporter().importFromBytes(bytes, onProgress = { progress ->
                                    // Update progress message on main thread
                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        relImportProgressMessage = progress
                                    }
                                })
                            }.getOrNull()
                        }
                        println("[DEBUG_LOG] MainScreen.onRelImportResult: loaded=$loaded, data has ${loaded?.data?.individuals?.size ?: 0} individuals")
                        
                        // Hide progress dialog
                        showRelImportProgress = false
                        
                        if (loaded != null) {
                            loadedProjectTemp = loaded
                        }
                        callback?.invoke(loaded)
                        
                        // Reset dialog state
                        showRelImportDialog = false
                        pendingRelImportCallback = null
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] MainScreen.onRelImportResult: ERROR importing .rel - ${e.message}")
                        e.printStackTrace()
                        
                        // Hide progress dialog
                        showRelImportProgress = false
                        
                        // Show error dialog with detailed message
                        relImportErrorMessage = e.message ?: "Unknown error occurred while importing .rel file"
                        showRelImportError = true
                        
                        callback?.invoke(null)
                        
                        // Reset dialog state
                        showRelImportDialog = false
                        pendingRelImportCallback = null
                    }
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onRelImportResult: bytes is null")
                callback?.invoke(null)
                showRelImportDialog = false
                pendingRelImportCallback = null
            }
        },
        // GEDCOM import
        showGedcomImport = showGedcomImportDialog,
        onDismissGedcomImport = {
            showGedcomImportDialog = false
            pendingGedcomImportCallback = null
        },
        onGedcomImportResult = { bytes ->
            val callback = pendingGedcomImportCallback
            if (bytes != null) {
                val content = bytes.decodeToString()
                val imported = runCatching {
                    com.family.tree.core.gedcom.GedcomImporter().importFromString(content)
                }.getOrNull()
                callback?.invoke(imported)
            } else {
                callback?.invoke(null)
            }
            showGedcomImportDialog = false
            pendingGedcomImportCallback = null
        },
        // GEDCOM export
        showGedcomExport = showGedcomExportDialog,
        onDismissGedcomExport = {
            showGedcomExportDialog = false
            pendingGedcomExportData = null
        },
        gedcomBytesToSave = {
            val data = pendingGedcomExportData ?: project
            val exporter = com.family.tree.core.gedcom.GedcomExporter()
            val content = exporter.exportToString(data)
            content.encodeToByteArray()
        },
        // SVG export (current view)
        showSvgExport = showSvgExportDialog,
        onDismissSvgExport = {
            showSvgExportDialog = false
            pendingSvgExportData = null
        },
        svgBytesToSave = {
            // TODO: Implement SVG exporter for KMP
            val (data, exportScale, exportPan) = pendingSvgExportData ?: Triple(project, scale, pan)
            "<!-- SVG export not yet implemented for Android/iOS -->".encodeToByteArray()
        },
        // SVG export (fit to content)
        showSvgExportFit = showSvgExportFitDialog,
        onDismissSvgExportFit = {
            showSvgExportFitDialog = false
            pendingSvgExportFitData = null
        },
        svgFitBytesToSave = {
            // TODO: Implement SVG exporter for KMP
            val data = pendingSvgExportFitData ?: project
            "<!-- SVG export not yet implemented for Android/iOS -->".encodeToByteArray()
        },
        // AI text import
        showAiTextImport = showAiTextImportDialog,
        onDismissAiTextImport = {
            showAiTextImportDialog = false
            pendingAiTextImportCallback = null
        },
        onAiTextImportResult = { bytes ->
            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: received bytes=${bytes?.size ?: 0} bytes")
            val callback = pendingAiTextImportCallback
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
                    showAiImportProgress = true
                    aiImportProgressMessage = "Extracting text from PDF..."
                    
                    // Extract text from PDF in background
                    scope.launch {
                        try {
                            val pdfExtractor = com.family.tree.core.platform.PdfTextExtractor(platformContext)
                            val extractedText = pdfExtractor.extractText(bytes)
                            
                            if (extractedText.isNullOrBlank()) {
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: PDF text extraction failed or returned empty text")
                                
                                // Show error dialog
                                showAiImportProgress = false
                                aiImportInfoMessage = """
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
                                showAiImportInfoDialog = true
                            } else {
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Successfully extracted ${extractedText.length} chars from PDF")
                                
                                // Process extracted text
                                aiImportProgressMessage = "Processing text..."
                                
                                // Load saved AI settings
                                val storage = com.family.tree.core.ai.AiSettingsStorage()
                                val config = storage.loadConfig()
                                
                                // Determine file type and process
                                val importer = com.family.tree.core.ai.AiTextImporter(config)
                                val imported = if (extractedText.trimStart().startsWith("{") || extractedText.trimStart().startsWith("[")) {
                                    // JSON format - parse directly
                                    println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected JSON format in PDF")
                                    aiImportProgressMessage = "Processing JSON..."
                                    importer.importFromAiResult(extractedText)
                                } else {
                                    // Plain text - call AI
                                    println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected plain text in PDF, calling AI...")
                                    aiImportProgressMessage = "Sending request to AI (${config.model})..."
                                    importer.importFromText(extractedText)
                                }
                                
                                aiImportProgressMessage = "Creating family tree..."
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Success - ${imported.data.individuals.size} individuals, ${imported.data.families.size} families")
                                
                                // Hide progress dialog
                                showAiImportProgress = false
                                
                                loadedProjectTemp = imported
                                callback?.invoke(imported)
                                
                                // Reset dialog state
                                showAiTextImportDialog = false
                                pendingAiTextImportCallback = null
                            }
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: ERROR extracting/processing PDF - ${e.message}")
                            e.printStackTrace()
                            
                            // Hide progress dialog and show error
                            showAiImportProgress = false
                            aiImportInfoMessage = """
                                Ошибка при обработке PDF файла: ${e.message ?: "Неизвестная ошибка"}
                                
                                Пожалуйста, попробуйте:
                                1. Откройте docs.google.com в браузере
                                2. Файл → Скачать → Обычный текст (.txt)
                                3. Импортируйте полученный .txt файл
                            """.trimIndent()
                            showAiImportInfoDialog = true
                        }
                    }
                } else {
                    // Show progress dialog
                    showAiImportProgress = true
                    aiImportProgressMessage = "Reading file..."
                    
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
                                aiImportProgressMessage = "Processing JSON..."
                                importer.importFromAiResult(content)
                            } else {
                                // Plain text - call AI
                                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Detected plain text, calling AI...")
                                aiImportProgressMessage = "Sending request to AI (${config.model})..."
                                importer.importFromText(content)
                            }
                            
                            aiImportProgressMessage = "Creating family tree..."
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: Success - ${imported.data.individuals.size} individuals, ${imported.data.families.size} families")
                            
                            // Hide progress dialog
                            showAiImportProgress = false
                            
                            loadedProjectTemp = imported
                            callback?.invoke(imported)
                        } catch (e: Exception) {
                            println("[DEBUG_LOG] MainScreen.onAiTextImportResult: ERROR - ${e.message}")
                            e.printStackTrace()
                            
                            // Hide progress dialog
                            showAiImportProgress = false
                            
                            callback?.invoke(null)
                        }
                    }
                    // Reset only after processing text/JSON file
                    showAiTextImportDialog = false
                    pendingAiTextImportCallback = null
                }
            } else {
                println("[DEBUG_LOG] MainScreen.onAiTextImportResult: bytes is null")
                callback?.invoke(null)
                showAiTextImportDialog = false
                pendingAiTextImportCallback = null
            }
        }
    )
}
