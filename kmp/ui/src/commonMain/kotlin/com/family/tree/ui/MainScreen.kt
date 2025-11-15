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
    // Project state: start from sample data
    val sample = remember { SampleData.simpleThreeGen() }
    var project by remember {
        println("[DEBUG_LOG] MainScreen: Initializing project state with ${sample.first.size} individuals")
        mutableStateOf(
            ProjectData(individuals = sample.first, families = sample.second)
        )
    }
    println("[DEBUG_LOG] MainScreen: Current project state has ${project.individuals.size} individuals, ${project.families.size} families")
    var projectLayout by remember { mutableStateOf<com.family.tree.core.layout.ProjectLayout?>(null) }

    // Selection state (single-select for now)
    var selectedId by remember { mutableStateOf<IndividualId?>(null) }

    // Viewport state (simplified without Animatable to unblock compile)
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Render toggles
    var showGrid by remember { mutableStateOf(true) }
    var lineWidth by remember { mutableFloatStateOf(1f) }

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

    // File dialog state for Android/platform file pickers
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingOpenCallback by remember { mutableStateOf<((LoadedProject?) -> Unit)?>(null) }
    var pendingSaveData by remember { mutableStateOf<ProjectData?>(null) }
    
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

    // Keyboard focus & modifiers
    val focusRequester = remember { FocusRequester() }
    var isSpacePressed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Wire global app actions for Desktop menu and other entry points
    AppActions.openPed = {
        DesktopActions.openPed { loaded ->
            println("[DEBUG_LOG] MainScreen.openPed callback: loaded=$loaded")
            if (loaded != null) {
                println("[DEBUG_LOG] MainScreen.openPed: Received data with ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                project = loaded.data
                projectLayout = loaded.layout
                println("[DEBUG_LOG] MainScreen.openPed: Updated project state, layout has ${loaded.layout?.nodePositions?.size ?: 0} node positions")
                selectedId = null
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
    AppActions.savePed = { DesktopActions.savePed(project) }
    AppActions.importRel = {
        DesktopActions.importRel { loaded ->
            println("[DEBUG_LOG] MainScreen.importRel callback: loaded=$loaded")
            if (loaded != null) {
                println("[DEBUG_LOG] MainScreen.importRel: Received data with ${loaded.data.individuals.size} individuals, ${loaded.data.families.size} families")
                project = loaded.data
                projectLayout = loaded.layout
                println("[DEBUG_LOG] MainScreen.importRel: Updated project state, layout has ${loaded.layout?.nodePositions?.size ?: 0} node positions")
                selectedId = null
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
                selectedId = null
                fitToView()
                println("[DEBUG_LOG] MainScreen.importGedcom: Final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}")
            } else {
                println("[DEBUG_LOG] MainScreen.importGedcom: data is null (user cancelled or error)")
            }
        }
    }
    AppActions.exportGedcom = { DesktopActions.exportGedcom(project) }
    AppActions.exportSvgCurrent = { DesktopActions.exportSvg(project, scale = scale, pan = pan) }
    AppActions.exportSvgFit = { DesktopActions.exportSvgFit(project) }
    AppActions.exportPngCurrent = { DesktopActions.exportPng(project, scale = scale, pan = pan) }
    AppActions.exportPngFit = { DesktopActions.exportPngFit(project) }
    AppActions.toggleGrid = { showGrid = !showGrid }
    AppActions.setLineWidth1x = { lineWidth = 1f }
    AppActions.setLineWidth2x = { lineWidth = 2f }
    AppActions.zoomIn = { setScaleAnimated(scale * 1.1f) }
    AppActions.zoomOut = { setScaleAnimated(scale * 0.9f) }
    AppActions.reset = { fitToView() }

    // Wire dialog actions for platform file dialogs (used by Android DesktopActions)
    DialogActions.triggerOpenDialog = { callback ->
        pendingOpenCallback = callback
        showOpenDialog = true
    }
    DialogActions.triggerSaveDialog = { data ->
        pendingSaveData = data
        showSaveDialog = true
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

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .platformKeyboardShortcuts(
                    onEscape = { selectedId = null },
                    onZoomIn = { setScaleAnimated(scale * 1.1f) },
                    onZoomOut = { setScaleAnimated(scale * 0.9f) }
                )
        ) {
            // Top toolbar
            if (!PlatformEnv.isDesktop) {
                var showMenu by remember { mutableStateOf(false) }
                androidx.compose.material3.TopAppBar(
                    title = { Text("Compose Multiplatform — Family Tree") },
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
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Open") }, onClick = { showMenu = false; AppActions.openPed() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Save") }, onClick = { showMenu = false; AppActions.savePed() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Import .rel") }, onClick = { showMenu = false; AppActions.importRel() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Import GEDCOM") }, onClick = { showMenu = false; AppActions.importGedcom() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export GEDCOM") }, onClick = { showMenu = false; AppActions.exportGedcom() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export SVG (Current)") }, onClick = { showMenu = false; AppActions.exportSvgCurrent() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Export SVG (Fit)") }, onClick = { showMenu = false; AppActions.exportSvgFit() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text(if (showGrid) "Grid: On" else "Grid: Off") }, onClick = { showMenu = false; AppActions.toggleGrid() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Lines: 1x") }, onClick = { showMenu = false; AppActions.setLineWidth1x() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Lines: 2x") }, onClick = { showMenu = false; AppActions.setLineWidth2x() })
                            val selected = project.individuals.find { it.id == selectedId }
                            if (selected != null) {
                                androidx.compose.material3.DropdownMenuItem(text = { Text("Edit person…") }, onClick = { showMenu = false; editPersonId = selected.id })
                                val fam = project.families.firstOrNull { it.husbandId == selected.id || it.wifeId == selected.id || it.childrenIds.contains(selected.id) }
                                if (fam != null) {
                                    androidx.compose.material3.DropdownMenuItem(text = { Text("Edit family…") }, onClick = { showMenu = false; editFamilyId = fam.id })
                                }
                            }
                        }
                    }
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Compose Multiplatform — Family Tree", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${(scale * 100).toInt()}%", modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                }
            }

            // Node-level offsets to support dragging individual nodes (JavaFX-like)
            // Initialize from ProjectLayout coordinates if available (for REL format)
            val nodeOffsets = remember(project, projectLayout) {
                mutableStateMapOf<IndividualId, Offset>().apply {
                    projectLayout?.nodePositions?.forEach { (idString, nodePos) ->
                        val id = IndividualId(idString)
                        if (project.individuals.any { it.id == id }) {
                            put(id, Offset(nodePos.x.toFloat(), nodePos.y.toFloat()))
                        }
                    }
                }
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
                                LazyColumn(Modifier.weight(1f)) {
                                    items(project.individuals) { ind ->
                                        val isSelected = ind.id == selectedId
                                        val bg = if (isSelected) Color(0x201976D2) else Color.Unspecified
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bg)
                                                .combinedClickable(
                                                    onClick = {
                                                        selectedId = ind.id
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
                                    val ind = project.individuals.find { it.id == indId } ?: return ""
                                    val tokens = ind.displayName.trim().split(Regex("\\s+"))
                                    val first = tokens.firstOrNull() ?: ""
                                    val last = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else ""
                                    return if (last.isNotEmpty() && first.isNotEmpty()) {
                                        val initial = first.firstOrNull()?.uppercaseChar()
                                        if (initial != null) "$last $initial." else last
                                    } else if (last.isNotEmpty()) {
                                        last
                                    } else {
                                        first
                                    }
                                }
                                
                                LazyColumn(Modifier.weight(1f)) {
                                    items(project.families) { fam ->
                                        val kids = fam.childrenIds.size
                                        val husbandName = formatSpouseName(fam.husbandId)
                                        val wifeName = formatSpouseName(fam.wifeId)
                                        val spousesText = when {
                                            husbandName.isNotEmpty() && wifeName.isNotEmpty() -> "$husbandName - $wifeName"
                                            husbandName.isNotEmpty() -> husbandName
                                            wifeName.isNotEmpty() -> wifeName
                                            else -> fam.id.value
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = { /* select family in future */ },
                                                    onDoubleClick = { editFamilyId = fam.id }
                                                )
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        ) {
                                            Text("• $spousesText ($kids)")
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
                        selectedId = selectedId,
                        onSelect = { id -> selectedId = id },
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
                        showGrid = showGrid,
                        lineWidth = lineWidth
                    )
                }

                // Right inspector (scrollable)
                Box(
                    modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0x0D000000)),
                    contentAlignment = Alignment.TopStart
                ) {
                val selected = project.individuals.find { it.id == selectedId }
                if (selected == null) {
                    Column(Modifier.padding(12.dp).fillMaxWidth()) {
                        Text("Inspector\nSelect a person…")
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

    // Platform file dialogs (for Android and future cross-platform support)
    // Store loaded project temporarily to ensure state update happens in composition scope
    var loadedProjectTemp by remember { mutableStateOf<LoadedProject?>(null) }
    
    // Effect to apply loaded project data to main state
    LaunchedEffect(loadedProjectTemp) {
        val loaded = loadedProjectTemp
        if (loaded != null) {
            println("[DEBUG_LOG] MainScreen LaunchedEffect: Applying loaded project with ${loaded.data.individuals.size} individuals")
            project = loaded.data
            projectLayout = loaded.layout
            selectedId = null
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
                val loaded = runCatching {
                    // Use RelImporter for .rel binary TLV format (starts with "Rela" header)
                    RelImporter().importFromBytes(bytes)
                }.getOrNull()
                println("[DEBUG_LOG] MainScreen.onOpenResult: loaded=$loaded, data has ${loaded?.data?.individuals?.size ?: 0} individuals")
                // Instead of invoking callback directly, store in temp state to trigger LaunchedEffect
                if (loaded != null) {
                    loadedProjectTemp = loaded
                }
                callback?.invoke(loaded)
                println("[DEBUG_LOG] MainScreen.onOpenResult: callback invoked, project now has ${project.individuals.size} individuals")
            } else {
                println("[DEBUG_LOG] MainScreen.onOpenResult: bytes is null, invoking callback with null")
                callback?.invoke(null)
            }
            showOpenDialog = false
            pendingOpenCallback = null
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
        }
    )
}
