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

    // Selection model (single-select for now)
    val selection = remember { com.family.tree.core.editor.SelectionModel() }
    val selectedId by remember { derivedStateOf { selection.selected } }

    // Viewport state (simplified without Animatable to unblock compile)
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Render toggles
    var showGrid by remember { mutableStateOf(true) }
    var lineWidth by remember { mutableFloatStateOf(1f) }

    fun clampScale(s: Float) = s.coerceIn(0.25f, 4f)

    suspend fun setScaleImmediate(value: Float) { scale = clampScale(value) }
    fun setScaleAnimated(value: Float) { scale = clampScale(value) }

    fun fitToView() {
        val positions = SimpleTreeLayout.layout(project.individuals, project.families)
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

    fun centerOn(ind: Individual) {
        val positions = SimpleTreeLayout.layout(project.individuals, project.families)
        val p = positions[ind.id] ?: return
        if (canvasSize.width == 0 || canvasSize.height == 0) return
        val cx = canvasSize.width / 2f
        val cy = canvasSize.height / 2f
        val nodeW = 120f * scale
        val nodeH = 60f * scale
        val targetX = p.x * scale + nodeW / 2
        val targetY = p.y * scale + nodeH / 2
        pan = Offset(cx - targetX, cy - targetY)
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
                selection.clear()
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
                selection.clear()
                fitToView()
                println("[DEBUG_LOG] MainScreen.importRel: Final state - project.individuals=${project.individuals.size}, project.families=${project.families.size}")
            } else {
                println("[DEBUG_LOG] MainScreen.importRel: loaded is null (user cancelled or error)")
            }
        }
    }
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

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .platformKeyboardShortcuts(
                    onEscape = { selection.clear() },
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

            // Main content: left list, center canvas, right inspector
            Row(Modifier.fillMaxSize()) {
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
                                                            selection.select(ind.id)
                                                            centerOn(ind)
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
                                            "$last ${first.first().uppercaseChar()}."
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
                        // Desktop-only wheel zoom (no-op on Android) via expect/actual modifier
                        .platformWheelZoom(
                            getScale = { scale },
                            setScale = { setScaleAnimated(it) },
                            getPan = { pan },
                            setPan = { pan = it },
                            getCanvasSize = { canvasSize }
                        )
                        // Multi-touch gestures: pinch to zoom, drag to pan
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, panChange, zoomChange, _ ->
                                // Apply zoom change
                                val newScale = (scale * zoomChange).coerceIn(0.25f, 4f)
                                
                                // Zoom toward centroid (touch point)
                                // Before zoom: point P in world coords = (screenX - pan.x) / scale
                                // After zoom: we want P to stay at same screen position
                                // So: (screenX - newPan.x) / newScale = (screenX - pan.x) / scale
                                // Solving: newPan.x = screenX - ((screenX - pan.x) / scale) * newScale
                                val worldX = (centroid.x - pan.x) / scale
                                val worldY = (centroid.y - pan.y) / scale
                                val newPanX = centroid.x - worldX * newScale
                                val newPanY = centroid.y - worldY * newScale
                                
                                scale = newScale
                                pan = Offset(newPanX, newPanY) + panChange
                            }
                        }
                ) {
                    // Node-level offsets to support dragging individual nodes (JavaFX-like)
                    val nodeOffsets = remember(project, projectLayout) {
                        mutableStateMapOf<IndividualId, Offset>().apply {
                            // Initialize from ProjectLayout coordinates if available
                            projectLayout?.nodePositions?.forEach { (idString, nodePos) ->
                                val id = IndividualId(idString)
                                if (project.individuals.any { it.id == id }) {
                                    put(id, Offset(nodePos.x.toFloat(), nodePos.y.toFloat()))
                                }
                            }
                        }
                    }
                    TreeRenderer(
                        data = project,
                        selectedId = selectedId,
                        onSelect = { id -> selection.select(id) },
                        onEditPerson = { id -> editPersonId = id },
                        onCenterOn = { id ->
                            project.individuals.find { it.id == id }?.let { centerOn(it) }
                        },
                        onReset = { fitToView() },
                        onNodeDrag = { id, delta ->
                            val cur = nodeOffsets[id] ?: Offset.Zero
                            nodeOffsets[id] = cur + delta
                        },
                        nodeOffsets = nodeOffsets,
                        scale = scale,
                        pan = pan,
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
            selection.clear()
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
        }
    )
}

@Composable
private fun TreeCanvas(
    data: ProjectData,
    selectedId: IndividualId?,
    onSelect: (IndividualId?) -> Unit,
    scale: Float,
    pan: Offset
) {
    val positions = remember(data) { SimpleTreeLayout.layout(data.individuals, data.families) }

    // Node visuals
    val nodeWidth = 120.dp * scale
    val nodeHeight = 60.dp * scale

    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        // Background grid respecting pan/scale
        Canvas(Modifier.fillMaxSize()) {
            val spacingBase = 64f
            val spacing = (spacingBase * scale).coerceAtLeast(20f)
            val w = size.width
            val h = size.height
            // Offset so that grid moves with pan
            val startX = ((pan.x % spacing) + spacing) % spacing
            val startY = ((pan.y % spacing) + spacing) % spacing
            val gridColor = Color(0x11000000)
            var x = startX
            while (x <= w) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, h))
                x += spacing
            }
            var y = startY
            while (y <= h) {
                drawLine(gridColor, Offset(0f, y), Offset(w, y))
                y += spacing
            }
        }
        // Offset whole drawing area by pan
        Box(
            modifier = Modifier
                .offset { IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }
                .fillMaxSize()
        ) {
            // Draw connection lines
            Canvas(Modifier.fillMaxSize()) {
                data.families.forEach { fam ->
                    val parents = listOfNotNull(fam.husbandId, fam.wifeId)
                    val parentCenters = parents.mapNotNull { id ->
                        positions[id]?.let { p -> Offset(p.x * scale, p.y * scale) }
                    }
                    val children = fam.childrenIds
                    val childCenters = children.mapNotNull { id ->
                        positions[id]?.let { p -> Offset(p.x * scale, p.y * scale) }
                    }
                    if (parentCenters.isNotEmpty() && childCenters.isNotEmpty()) {
                        val yParent = parentCenters.map { it.y }.average().toFloat() + nodeHeight.toPx() / 2
                        val xParentMid = parentCenters.map { it.x }.average().toFloat() + nodeWidth.toPx() / 2
                        childCenters.forEach { child ->
                            val yChild = child.y + nodeHeight.toPx() / 2
                            val xChild = child.x + nodeWidth.toPx() / 2
                            drawLine(
                                color = Color(0xFF607D8B),
                                start = Offset(xParentMid, yParent),
                                end = Offset(xChild, yChild),
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }

            // Draw nodes as composables so we can use Text easily
            data.individuals.forEach { ind ->
                val pos = positions[ind.id] ?: return@forEach
                val leftDp = with(density) { (pos.x * scale).toDp() }
                val topDp = with(density) { (pos.y * scale).toDp() }
                NodeCard(
                    name = ind.displayName,
                    x = leftDp,
                    y = topDp,
                    width = nodeWidth,
                    height = nodeHeight,
                    selected = (ind.id == selectedId),
                    onClick = { onSelect(ind.id) }
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    name: String,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF1976D2) else Color(0xFFB0BEC5)
    val borderWidth = if (selected) 3f else 2f
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(Color(0xFFFAFAFA))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Border
        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = borderColor, style = Stroke(width = borderWidth))
        }
        Text(
            name,
            modifier = Modifier.align(Alignment.Center),
            color = Color(0xFF263238),
            fontSize = 12.sp
        )
    }
}