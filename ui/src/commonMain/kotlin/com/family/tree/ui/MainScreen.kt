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
import com.family.tree.ui.components.panels.RightSidebar
import com.family.tree.ui.components.dialogs.MainScreenDialogState
import com.family.tree.ui.components.dialogs.MainScreenDialogs
import com.family.tree.ui.components.MainTopAppBar

import com.family.tree.ui.components.panels.LeftSidebar
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

        val dialogState = remember { com.family.tree.ui.components.dialogs.MainScreenDialogState() }

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
        dialogState.showAiImportProgress = true
        dialogState.aiImportProgressMessage = "Подготовка..."
        
        DesktopActions.importAiText(
            onLoaded = { loaded ->
                println("[DEBUG_LOG] MainScreen.importAiText callback: loaded=$loaded")
                // Hide progress dialog
                dialogState.showAiImportProgress = false
                
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
                dialogState.aiImportProgressMessage = message
            }
        )
    }
    AppActions.voiceInput = {
        run {
            if (!voiceInputProcessor.isVoiceInputAvailable()) {
                dialogState.aiImportInfoMessage = "Голосовой ввод недоступен на этой платформе"
                dialogState.showAiImportInfoDialog = true
                return@run
            }
            
            if (voiceInputProcessor.isRecording()) {
                // Already recording, stop it
                voiceInputProcessor.stopRecording()
                dialogState.isVoiceRecording = false
                dialogState.voiceInputStatus = ""
                return@run
            }
            
            // Загружаем сохраненные настройки и сразу начинаем запись
            println("[DEBUG_LOG] MainScreen.voiceInput: Starting voice input with saved settings")
            dialogState.isVoiceRecording = true
            dialogState.voiceInputStatus = "Speak..."
            dialogState.showVoiceInputDialog = true
            
            voiceInputProcessor.startVoiceInput(
                onSuccess = { loaded ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Success - ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                    dialogState.isVoiceRecording = false
                    dialogState.showVoiceInputDialog = false
                    dialogState.voiceInputStatus = ""
                    
                    // Update project
                    project = loaded.data
                    projectLayout = loaded.layout
                    selectedIds = emptySet()
                    fitToView()
                },
                onError = { errorMessage ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Error - $errorMessage")
                    dialogState.isVoiceRecording = false
                    dialogState.voiceInputStatus = ""
                    dialogState.showVoiceInputDialog = false
                    
                    // Check if it's a permission error
                    dialogState.isPermissionError = errorMessage.contains("разрешений") || errorMessage.contains("RECORD_AUDIO")
                    
                    // Show error dialog
                    dialogState.aiImportInfoMessage = "Voice input error:\n$errorMessage"
                    dialogState.showAiImportInfoDialog = true
                },
                onRecognized = { recognizedText ->
                    println("[DEBUG_LOG] MainScreen.voiceInput: Recognized - $recognizedText")
                    dialogState.voiceInputStatus = "Recognized: \"$recognizedText\"\nProcessing via AI..."
                }
            )
        }
    }
    AppActions.exportGedcom = { DesktopActions.exportGedcom(project) }
    AppActions.exportMarkdownTree = { DesktopActions.exportMarkdownTree(project) }
    AppActions.exportSvgCurrent = { DesktopActions.exportSvg(project, scale = scale, pan = pan) }
    AppActions.exportSvgFit = { DesktopActions.exportSvgFit(project) }
    AppActions.exportPngCurrent = { DesktopActions.exportPng(project, scale = scale, pan = pan) }
    AppActions.exportPngFit = { DesktopActions.exportPngFit(project) }
    AppActions.zoomIn = { setScaleAnimated(scale * 1.1f) }
    AppActions.zoomOut = { setScaleAnimated(scale * 0.9f) }
    AppActions.reset = { fitToView() }
    AppActions.manageSources = { dialogState.showSourcesDialog = true }
    AppActions.showAbout = { dialogState.showAboutDialog = true }
    AppActions.showAiSettings = { dialogState.showAiSettingsDialog = true }

    // Wire dialog actions for platform file dialogs (used by Android DesktopActions)
    DialogActions.triggerOpenDialog = { callback ->
        dialogState.pendingOpenCallback = callback
        dialogState.showOpenDialog = true
    }
    DialogActions.triggerSaveDialog = { data ->
        dialogState.pendingSaveData = data
        dialogState.showSaveDialog = true
    }
    DialogActions.triggerRelImport = { callback ->
        dialogState.pendingRelImportCallback = callback
        dialogState.showRelImportDialog = true
    }
    DialogActions.triggerGedcomImport = { callback ->
        dialogState.pendingGedcomImportCallback = callback
        dialogState.showGedcomImportDialog = true
    }
    DialogActions.triggerGedcomExport = { data ->
        dialogState.pendingGedcomExportData = data
        dialogState.showGedcomExportDialog = true
    }
    DialogActions.triggerMarkdownExport = { data ->
        dialogState.pendingMarkdownExportData = data
        dialogState.showMarkdownExportDialog = true
    }
    DialogActions.triggerSvgExport = { data, scale, pan ->
        dialogState.pendingSvgExportData = Triple(data, scale, pan)
        dialogState.showSvgExportDialog = true
    }
    DialogActions.triggerSvgExportFit = { data ->
        dialogState.pendingSvgExportFitData = data
        dialogState.showSvgExportFitDialog = true
    }
    DialogActions.triggerAiTextImport = { callback ->
        dialogState.pendingAiTextImportCallback = callback
        dialogState.showAiTextImportDialog = true
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
            MainTopAppBar(scale = scale)

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
                LeftSidebar(
                    project = project,
                    searchService = searchService,
                    selectedIds = selectedIds,
                    onSelectIndividual = { ind ->
                        selectedFamilyId = null
                        selectedIds = setOf(ind.id)
                        centerOn(ind, nodeOffsets)
                    },
                    onSelectFamily = { famId, familyMemberIds ->
                        selectedFamilyId = famId
                        selectedIds = familyMemberIds
                        centerOnFamilyMembersImmediate(familyMemberIds, nodeOffsets)
                    },
                    onEditIndividual = { id -> dialogState.editPersonId = id },
                    onEditFamily = { id -> dialogState.editFamilyId = id },
                    modifier = Modifier.width(260.dp)
                )

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
                        onEditPerson = { id -> dialogState.editPersonId = id },
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
                            dialogState.editPersonId = newIndividual.id
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
                RightSidebar(
                    project = project,
                    selectedIds = selectedIds,
                    selectedFamilyId = selectedFamilyId,
                    onUpdateProject = { project = it },
                    modifier = Modifier.width(260.dp)
                )
            }
        }
    }

    // All Dialogs extracted
    com.family.tree.ui.components.dialogs.MainScreenDialogs(
        state = dialogState,
        project = project,
        onUpdateProject = { project = it },
        loadedProjectTemp = loadedProjectTemp,
        setLoadedProjectTemp = { loadedProjectTemp = it },
        onUpdateProjectLayout = { projectLayout = it },
        onUpdateScale = { scale = it },
        onUpdatePan = { pan = it },
        onClearSelection = { selectedIds = emptySet() },
        fitToView = { fitToView() },
        platformContext = platformContext,
        scope = scope,
        scale = scale,
        pan = pan,
        voiceInputProcessor = voiceInputProcessor
    )

}
