package com.family.tree.ui.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import com.family.tree.core.ProjectData
import com.family.tree.core.layout.SimpleTreeLayout
import com.family.tree.core.model.IndividualId
import com.family.tree.ui.PlatformEnv
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Detects if the text contains Cyrillic characters.
 */
private fun isCyrillic(text: String?): Boolean {
    if (text.isNullOrEmpty()) return false
    for (c in text) {
        if (c in '\u0400'..'\u04FF') {
            return true
        }
    }
    return false
}

/**
 * Extracts event date from individual's events by type (e.g., "BIRT", "DEAT").
 */
private fun extractEventDate(ind: com.family.tree.core.model.Individual?, type: String?): String? {
    if (ind == null || type == null) return null
    val t = type.trim().uppercase()
    for (ev in ind.events) {
        val evType = ev.type
        if (t == evType.trim().uppercase()) {
            return ev.date
        }
    }
    return null
}

/**
 * Formats birth and death dates according to the requirements:
 * - Both dates: "birthDate - deathDate"
 * - Only birth: "b.:" or "род.:" prefix
 * - Only death: "d.:" or "ум.:" prefix
 * The language is detected from the individual's name.
 */
private fun formatDates(birthDate: String?, deathDate: String?, firstName: String?, lastName: String?): String? {
    val hasBirth = !birthDate.isNullOrBlank()
    val hasDeath = !deathDate.isNullOrBlank()

    if (!hasBirth && !hasDeath) {
        return null
    }

    // Detect language from names
    val isCyrillicText = isCyrillic(firstName) || isCyrillic(lastName)

    return if (hasBirth && hasDeath) {
        // Both dates: show as "birthDate - deathDate"
        birthDate!!.trim() + " - " + deathDate!!.trim()
    } else if (hasBirth) {
        // Only birth date
        val prefix = if (isCyrillicText) "род.:" else "b.:"
        prefix + birthDate!!.trim()
    } else {
        // Only death date
        val prefix = if (isCyrillicText) "ум.:" else "d.:"
        prefix + deathDate!!.trim()
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun TreeRenderer(
    data: ProjectData,
    selectedId: IndividualId?,
    onSelect: (IndividualId?) -> Unit,
    onEditPerson: (IndividualId) -> Unit,
    onCenterOn: (IndividualId) -> Unit,
    onReset: () -> Unit,
    onNodeDrag: (IndividualId, Offset) -> Unit,
    nodeOffsets: Map<IndividualId, Offset>,
    scale: Float,
    pan: Offset,
    setPan: (Offset) -> Unit,
    onCanvasSize: (IntSize) -> Unit,
    showGrid: Boolean,
    lineWidth: Float
) {
    println("[DEBUG_LOG] TreeRenderer: recomposing (total data: ${data.individuals.size} individuals, ${data.families.size} families), scale=$scale, pan=$pan")
    
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Create a fast lookup map for individuals by ID (avoid repeated find() calls)
    val individualsMap = remember(data.individuals) {
        data.individuals.associateBy { it.id }
    }
    
    // Memoize layout computation - only recompute when data changes
    val positions = remember(data.individuals, data.families) {
        SimpleTreeLayout.layout(data.individuals, data.families).also {
            println("[DEBUG_LOG] TreeRenderer: computed ${it.size} positions")
        }
    }
    
    val density = LocalDensity.current

    // Измеритель текста для динамических размеров карточек
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()

    data class RectF(val x: Float, val y: Float, val w: Float, val h: Float) {
        val left get() = x
        val right get() = x + w
        val top get() = y
        val bottom get() = y + h
        val centerX get() = x + w / 2f
        val centerY get() = y + h / 2f
    }

    data class NodeMeasure(val w: Float, val h: Float, val baseFsSp: Float)
    fun measureNodePx(firstName: String, lastName: String, birthDate: String?, deathDate: String?): NodeMeasure {
        fun lhFactorForScale(s: Float): Float {
            // Aggressive mid-scale compression: ~1.10 at s>=1.0 → ~1.02 at s=0.6 → ~1.00 at s<=0.4
            val u = ((1.0f - s) / 0.6f).coerceIn(0f, 1f) // 0 at 1.0, 1 at 0.4
            return 1.10f - 0.10f * u
        }
        fun padYpxForScale(s: Float): Float = (4.5f * s).coerceIn(1f, 4.5f)
        fun lineGapPxForScale(s: Float): Float {
            // Drop from 1.5px at s>=1.0 to ~0px at s<=0.5
            val u = ((1.0f - s) / 0.5f).coerceIn(0f, 1f)
            return (1.5f - 1.5f * u).coerceIn(0f, 1.5f)
        }
        // Минимальные размеры + паддинги масштабируются с зумом; измеряем три строки: имя, фамилия и даты
        val baseMinW = if (PlatformEnv.isDesktop) 74.25f else 49.5f  // На мобильных уменьшаем в 1.5 раза
        val baseMinH = if (PlatformEnv.isDesktop) 50.625f else 33.75f  // Увеличено для третьей строки (1.5x от старого значения)
        val extraFudge = 0f
        val minW = baseMinW * scale
        val minH = baseMinH * scale
        // Синхронизация с отрисовкой: горизонтальный паддинг ~4.5px*scale, вертикальный ~4.5px*scale с ограничением
        val padX = 4.5f * scale
        val padY = padYpxForScale(scale)
        val lineGap = lineGapPxForScale(scale)
        val first = firstName
        val last = lastName
        // Формируем строку с датами для измерения
        val dateText = formatDates(birthDate, deathDate, firstName, lastName)
        // Подбираем общий кегль (sp) для имени, фамилии и дат, чтобы все три строки гарантированно поместились
        val candidates = listOf(3.75f, 3.375f, 3f, 2.625f, 2.25f, 1.875f, 1.5f, 1.3125f, 1.125f)
        var chosenFsBaseSp = 3f
        var resFirstHeight = 0
        var resFirstWidth = 0
        var resLastHeight = 0
        var resLastWidth = 0
        var resDateHeight = 0
        var resDateWidth = 0
        val isAndroid = !PlatformEnv.isDesktop
        for (fsBase in candidates) {
            // Базовый кегль в sp с учётом zoom
            val baseFsSp = (fsBase.sp * scale)
            // На мобильных устройствах НЕ применяем минимумы, чтобы карточки могли реально уменьшаться
            val effFsSp = baseFsSp

            val lhFactor = lhFactorForScale(scale)
            // Предварительное значение lineHeight
            val lineHeightSp = effFsSp * lhFactor

            val nameStyle = TextStyle(
                fontSize = effFsSp,
                // Используем Normal, чтобы измерение соответствовало фактической отрисовке
                fontWeight = FontWeight.Normal,
                lineHeight = lineHeightSp
            )
            val lastStyle = TextStyle(
                fontSize = effFsSp,
                lineHeight = lineHeightSp
            )
            val dateStyle = TextStyle(
                fontSize = effFsSp,
                lineHeight = lineHeightSp
            )
            val rFirst = measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(first),
                style = nameStyle,
                softWrap = false
            )
            // Всегда измеряем фамилию (даже если пустая), чтобы резервировать место
            val rLast = measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(if (last.isNotBlank()) last else " "),
                style = lastStyle,
                softWrap = false
            )
            // Всегда измеряем даты (даже если пустые), чтобы резервировать место
            val rDate = measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(if (!dateText.isNullOrEmpty()) dateText else " "),
                style = dateStyle,
                softWrap = false
            )
            
            // Рассчитываем высоту контента ВСЕГДА с учетом всех трех строк
            var contentHTemp = rFirst.size.height.toFloat()
            contentHTemp += lineGap + rLast.size.height.toFloat()
            contentHTemp += lineGap + rDate.size.height.toFloat()
            contentHTemp += extraFudge
            
            val totalH = contentHTemp + padY * 2
            resFirstHeight = rFirst.size.height
            resFirstWidth = rFirst.size.width
            resLastHeight = rLast.size.height
            resLastWidth = rLast.size.width
            resDateHeight = rDate.size.height
            resDateWidth = rDate.size.width
            chosenFsBaseSp = fsBase
            if (totalH <= minH) break
        }
        // Учитываем ширину всех трех строк: имя, фамилия и даты
        val contentW = max(resFirstWidth, max(resLastWidth, resDateWidth))
        // Высота ВСЕГДА включает три строки
        var contentH = resFirstHeight.toFloat()
        contentH += lineGap + resLastHeight.toFloat()
        contentH += lineGap + resDateHeight.toFloat()
        contentH += extraFudge
        // Add extra width buffer (16 * scale) to ensure wide cards don't overlap at medium/large scales
        val w = max(minW, contentW + padX * 2 + 16f * scale)
        val h = max(minH, contentH + padY * 2)
        return NodeMeasure(w, h, chosenFsBaseSp)
    }

    // Approximate bounds for viewport culling (before expensive text measurement)
    // Use fixed base size scaled appropriately - good enough for culling, precise measurement happens later
    data class ApproxBounds(val x: Float, val y: Float, val w: Float, val h: Float)
    
    // Calculate base positions without nodeOffsets to avoid recalc on drag
    val baseApproxBounds: Map<IndividualId, ApproxBounds> = remember(data.individuals, positions, scale) {
        buildMap {
            data.individuals.forEach { ind ->
                val p = positions[ind.id] ?: return@forEach
                val baseX = p.x * scale
                val baseY = p.y * scale
                // Use approximate size: min card dimensions scaled (updated for 3 lines)
                val approxW = (if (PlatformEnv.isDesktop) 74.25f else 49.5f) * scale
                val approxH = (if (PlatformEnv.isDesktop) 50.625f else 33.75f) * scale
                put(ind.id, ApproxBounds(baseX, baseY, approxW, approxH))
            }
        }
    }
    
    // Function to get actual bounds including nodeOffsets (computed on-demand, not cached)
    fun getApproxBounds(id: IndividualId): ApproxBounds? {
        val base = baseApproxBounds[id] ?: return null
        val off = nodeOffsets[id]
        return if (off != null) {
            ApproxBounds(off.x * scale, off.y * scale, base.w, base.h)
        } else {
            base
        }
    }
    
    // Viewport culling using approximate bounds (cheap operation)
    fun isVisibleApprox(b: ApproxBounds): Boolean {
        val screenLeft = b.x + pan.x
        val screenRight = b.x + b.w + pan.x
        val screenTop = b.y + pan.y
        val screenBottom = b.y + b.h + pan.y
        val margin = if (PlatformEnv.isDesktop) 200f else 50f
        return screenRight >= -margin && screenLeft <= canvasSize.width + margin &&
               screenBottom >= -margin && screenTop <= canvasSize.height + margin
    }
    
    // Filter visible individuals BEFORE expensive text measurement
    // Now depends on nodeOffsets to recalc when nodes are dragged, but baseApproxBounds is cached
    val visibleIndividualIds = remember(baseApproxBounds, nodeOffsets, pan, canvasSize, scale) {
        data.individuals.mapNotNull { ind ->
            getApproxBounds(ind.id)?.let { bounds ->
                if (isVisibleApprox(bounds)) ind.id else null
            }
        }.also {
            println("[DEBUG_LOG] TreeRenderer: viewport culling reduced ${data.individuals.size} to ${it.size} visible individuals")
        }
    }
    
    // Simplified rendering mode for small scales (skip expensive text measurement)
    val useSimplifiedRendering = scale < 0.5f && !PlatformEnv.isDesktop
    
    // NOW measure text only for visible individuals (expensive but limited to visible set)
    // Skip measurement in simplified mode - use fixed sizes instead
    val measures: Map<IndividualId, NodeMeasure> = remember(visibleIndividualIds, individualsMap, scale, useSimplifiedRendering) {
        if (useSimplifiedRendering) {
            // Use fixed approximate size for all nodes in simplified mode
            val fixedW = (if (PlatformEnv.isDesktop) 74.25f else 49.5f) * scale
            val fixedH = (if (PlatformEnv.isDesktop) 50.625f else 33.75f) * scale
            visibleIndividualIds.associateWith { NodeMeasure(fixedW, fixedH, 3f) }
        } else {
            buildMap {
                visibleIndividualIds.forEach { id ->
                    val ind = individualsMap[id] ?: return@forEach
                    val birthDate = extractEventDate(ind, "BIRT")
                    val deathDate = extractEventDate(ind, "DEAT")
                    put(id, measureNodePx(ind.firstName, ind.lastName, birthDate, deathDate))
                }
            }
        }
    }

    // Precise rectangles only for visible individuals
    val rects: Map<IndividualId, RectF> = remember(visibleIndividualIds, positions, nodeOffsets, scale, measures) {
        buildMap {
            visibleIndividualIds.forEach { id ->
                val m = measures[id] ?: return@forEach
                val off = nodeOffsets[id]
                val (baseX, baseY) = if (off != null) {
                    Pair(off.x * scale, off.y * scale)
                } else {
                    val p = positions[id] ?: return@forEach
                    Pair(p.x * scale, p.y * scale)
                }
                put(id, RectF(baseX, baseY, m.w, m.h))
            }
        }
    }

    fun rectFor(id: IndividualId): RectF? = rects[id]
    fun textFsFor(id: IndividualId): Float = measures[id]?.baseFsSp ?: 3f
    
    // Calculate rect for any node (even invisible) for line connections
    fun rectForConnection(id: IndividualId): RectF? {
        // First try visible rects
        rects[id]?.let { return it }
        
        // If not visible, calculate approximate position from layout
        val off = nodeOffsets[id]
        val (baseX, baseY) = if (off != null) {
            Pair(off.x * scale, off.y * scale)
        } else {
            val p = positions[id] ?: return null
            Pair(p.x * scale, p.y * scale)
        }
        // Use approximate size for invisible nodes
        val approxW = (if (PlatformEnv.isDesktop) 74.25f else 49.5f) * scale
        val approxH = (if (PlatformEnv.isDesktop) 50.625f else 33.75f) * scale
        return RectF(baseX, baseY, approxW, approxH)
    }
    
    // Convert visible IDs back to Individual objects for rendering (using fast map lookup)
    val visibleIndividuals = remember(visibleIndividualIds, individualsMap) {
        visibleIndividualIds.mapNotNull { id ->
            individualsMap[id]
        }
    }

    fun marriageBarRect(a: RectF, b: RectF): RectF {
        val left = min(a.centerX, b.centerX)
        val right = max(a.centerX, b.centerX)
        val y = max(a.bottom, b.bottom) + 4.5f * scale
        return RectF(left, y, right - left, 1.125f * scale)
    }

    fun marriageMid(bar: RectF): Offset = Offset(bar.x + bar.w / 2f, bar.y + bar.h / 2f)

    fun routeVH(from: Offset, child: RectF): List<Offset> {
        val gap = 1.69f * scale
        val topAnchor = Offset(child.centerX, child.top - gap)
        return listOf(
            from,
            Offset(from.x, topAnchor.y),
            topAnchor,
            Offset(child.centerX, child.top)
        )
    }

    // Track whether current drag is on a node (to prevent canvas panning when dragging nodes)
    var isDraggingNode by remember { mutableStateOf(false) }
    
    // Wrap pan in remembered State to ensure fresh reads inside pointerInput
    val panState = remember { mutableStateOf(pan) }
    panState.value = pan

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()  // Clip rendering to green canvas boundaries
            .background(Color(0xFFD5E8D4))  // Light mint green background matching screenshot
            .onSizeChanged {
                canvasSize = it
                onCanvasSize(it) 
            }
            // Canvas panning: drag on empty space to pan the tree (like JavaFX version)
            // On mobile, pan/zoom is handled by platformWheelZoom in MainScreen via detectTransformGestures
            // On desktop, we need custom pan handling since platformWheelZoom only handles wheel events
            .then(
                if (PlatformEnv.isDesktop) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                // Wait for a down event
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val downPosition = down.position
                                
                                // Capture initial pan at drag start by reading from State (ensures fresh value)
                                val initialPan = panState.value
                                
                                // Check if drag started on a node
                                val clickedNode = visibleIndividuals.firstOrNull { ind ->
                                    val r = rectFor(ind.id)
                                    if (r != null) {
                                        val screenLeft = r.x + initialPan.x
                                        val screenTop = r.y + initialPan.y
                                        val screenRight = screenLeft + r.w
                                        val screenBottom = screenTop + r.h
                                        downPosition.x >= screenLeft && downPosition.x <= screenRight &&
                                        downPosition.y >= screenTop && downPosition.y <= screenBottom
                                    } else false
                                }
                                
                                if (clickedNode != null) {
                                    isDraggingNode = true
                                    // Wait for up/cancel without panning
                                    waitForUpOrCancellation()
                                    isDraggingNode = false
                                } else {
                                    // Canvas pan: track drag on empty space
                                    isDraggingNode = false
                                    var accumulatedDelta = Offset.Zero
                                    var lastPosition = downPosition
                                    
                                    drag(down.id) { change ->
                                        val delta = change.position - lastPosition
                                        lastPosition = change.position
                                        accumulatedDelta += delta
                                        change.consume()
                                        // Apply accumulated delta to initial pan (not current pan)
                                        setPan(initialPan + accumulatedDelta)
                                    }
                                    
                                    isDraggingNode = false
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Pan offset container
        Box(
            modifier = Modifier
                .offset { IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }
        ) {
            // Edges (сначала рёбра) - only render edges for families with visible participants
            Canvas(Modifier.fillMaxSize()) {
                val edgeColor = Color(0xFF607D8B)
                val barGap = 4.5f * scale  // gap below spouse boxes (scaled)
                val childBarGap = 4.5f * scale  // gap above children (scaled)
                // Минимальная толщина линий для мобильных устройств при малых масштабах:
                // при scale >= 1.0 → 2px, при малых масштабах → минимум 1.2px для видимости
                val baseStroke = 2f * scale
                val strokeW = if (PlatformEnv.isDesktop) baseStroke else max(baseStroke, 1.2f)
                
                // Build set of visible IDs for fast lookup
                val visibleIds = visibleIndividuals.map { it.id }.toSet()
                
                // Helper: check if a line segment intersects viewport (considering pan offset)
                fun lineIntersectsViewport(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
                    val screenX1 = x1 + pan.x
                    val screenY1 = y1 + pan.y
                    val screenX2 = x2 + pan.x
                    val screenY2 = y2 + pan.y
                    
                    val viewLeft = 0f
                    val viewRight = canvasSize.width.toFloat()
                    val viewTop = 0f
                    val viewBottom = canvasSize.height.toFloat()
                    
                    // Check if line bounding box intersects viewport
                    val lineLeft = min(screenX1, screenX2)
                    val lineRight = max(screenX1, screenX2)
                    val lineTop = min(screenY1, screenY2)
                    val lineBottom = max(screenY1, screenY2)
                    
                    return lineRight >= viewLeft && lineLeft <= viewRight &&
                           lineBottom >= viewTop && lineTop <= viewBottom
                }
                
                data.families.forEach { fam ->
                    // Check if any children are visible - if so, always draw lines from parents
                    val hasVisibleChild = fam.childrenIds.any { it in visibleIds }
                    
                    // Skip family only if no children are visible AND no parents are visible
                    val hasVisibleParent = (fam.husbandId in visibleIds) || (fam.wifeId in visibleIds)
                    
                    // OPTIMIZATION: Use fast bounding box check instead of detailed segment checks
                    var hasLinesThroughViewport = false
                    if (!hasVisibleChild && !hasVisibleParent) {
                        // Compute bounding box of the entire family (parents + children)
                        val allIds = listOfNotNull(fam.husbandId, fam.wifeId) + fam.childrenIds
                        if (allIds.isNotEmpty()) {
                            // Use approximate bounds for fast check
                            var minX = Float.POSITIVE_INFINITY
                            var maxX = Float.NEGATIVE_INFINITY
                            var minY = Float.POSITIVE_INFINITY
                            var maxY = Float.NEGATIVE_INFINITY
                            
                            allIds.forEach { id ->
                                val bounds = getApproxBounds(id)
                                if (bounds != null) {
                                    if (bounds.x < minX) minX = bounds.x
                                    if (bounds.x + bounds.w > maxX) maxX = bounds.x + bounds.w
                                    if (bounds.y < minY) minY = bounds.y
                                    if (bounds.y + bounds.h > maxY) maxY = bounds.y + bounds.h
                                }
                            }
                            
                            // Expand bounding box to include connection lines (add gaps)
                            minY -= childBarGap * 2f
                            maxY += barGap * 2f
                            
                            // Check if family bounding box intersects viewport
                            val screenLeft = minX + pan.x
                            val screenRight = maxX + pan.x
                            val screenTop = minY + pan.y
                            val screenBottom = maxY + pan.y
                            
                            hasLinesThroughViewport = screenRight >= 0f && screenLeft <= canvasSize.width.toFloat() &&
                                                     screenBottom >= 0f && screenTop <= canvasSize.height.toFloat()
                        }
                    }
                    
                    if (!hasVisibleChild && !hasVisibleParent && !hasLinesThroughViewport) return@forEach
                    
                    // Use rectForConnection to get parent positions even if they're off-screen
                    val husband = fam.husbandId?.let { rectForConnection(it) }
                    val wife = fam.wifeId?.let { rectForConnection(it) }
                    
                    when {
                        husband != null && wife != null -> {
                            // Marriage bar y just below lower parent bottom
                            val yBar = max(husband.bottom, wife.bottom) + barGap
                            val xLeft = min(husband.centerX, wife.centerX)
                            val xRight = max(husband.centerX, wife.centerX)
                            
                            // Short vertical stubs from each parent bottom to bar
                            drawLine(edgeColor, Offset(husband.centerX, husband.bottom), Offset(husband.centerX, yBar), strokeWidth = strokeW)
                            drawLine(edgeColor, Offset(wife.centerX, wife.bottom), Offset(wife.centerX, yBar), strokeWidth = strokeW)
                            
                            // Marriage bar between parent stubs
                            drawLine(edgeColor, Offset(xLeft, yBar), Offset(xRight, yBar), strokeWidth = strokeW)
                            val barMidX = (xLeft + xRight) / 2f
                            
                            // Children routing
                            if (fam.childrenIds.isEmpty()) {
                                // No children, nothing more to draw
                            } else if (fam.childrenIds.size == 1) {
                                // Single child: route toward child center with intermediate level
                                val cid = fam.childrenIds.first()
                                val c = rectForConnection(cid)
                                if (c != null) {
                                    val childBarY = c.top - childBarGap
                                    val midY = (yBar + childBarY) / 2f
                                    val cx = c.centerX
                                    
                                    // Vertical from marriage bar midpoint to intermediate level
                                    drawLine(edgeColor, Offset(barMidX, yBar), Offset(barMidX, midY), strokeWidth = strokeW)
                                    // Horizontal at intermediate level toward child center
                                    val hLeft = min(barMidX, cx)
                                    val hRight = max(barMidX, cx)
                                    drawLine(edgeColor, Offset(hLeft, midY), Offset(hRight, midY), strokeWidth = strokeW)
                                    // Vertical from child center to child bar level
                                    drawLine(edgeColor, Offset(cx, midY), Offset(cx, childBarY), strokeWidth = strokeW)
                                    // Vertical down to child top
                                    drawLine(edgeColor, Offset(cx, childBarY), Offset(cx, c.top), strokeWidth = strokeW)
                                }
                            } else {
                                // Multiple children: use children bar spanning all children
                                var minChildX = Float.POSITIVE_INFINITY
                                var maxChildX = Float.NEGATIVE_INFINITY
                                var minTopY = Float.POSITIVE_INFINITY
                                val childAnchors = mutableListOf<Pair<Float, Float>>() // centerX, topY
                                
                                fam.childrenIds.forEach { cid ->
                                    val c = rectForConnection(cid)
                                    if (c != null) {
                                        val cx = c.centerX
                                        val topY = c.top
                                        childAnchors.add(Pair(cx, topY))
                                        if (cx < minChildX) minChildX = cx
                                        if (cx > maxChildX) maxChildX = cx
                                        if (topY < minTopY) minTopY = topY
                                    }
                                }
                                
                                if (childAnchors.isNotEmpty()) {
                                    val childBarY = minTopY - childBarGap
                                    val childMidX = (minChildX + maxChildX) / 2f
                                    val midY = (yBar + childBarY) / 2f
                                    
                                    // Vertical from marriage bar midpoint to intermediate level
                                    drawLine(edgeColor, Offset(barMidX, yBar), Offset(barMidX, midY), strokeWidth = strokeW)
                                    // Horizontal at intermediate level toward children range center
                                    val hLeft = min(barMidX, childMidX)
                                    val hRight = max(barMidX, childMidX)
                                    drawLine(edgeColor, Offset(hLeft, midY), Offset(hRight, midY), strokeWidth = strokeW)
                                    // Vertical from children range center to children bar level
                                    drawLine(edgeColor, Offset(childMidX, midY), Offset(childMidX, childBarY), strokeWidth = strokeW)
                                    // Horizontal children bar spanning all children
                                    drawLine(edgeColor, Offset(minChildX, childBarY), Offset(maxChildX, childBarY), strokeWidth = strokeW)
                                    // Vertical lines from children bar down to each child top
                                    childAnchors.forEach { (cx, topY) ->
                                        drawLine(edgeColor, Offset(cx, childBarY), Offset(cx, topY), strokeWidth = strokeW)
                                    }
                                }
                            }
                        }
                        husband != null || wife != null -> {
                            // Single parent case
                            val p = husband ?: wife!!
                            val yBar = p.bottom + barGap
                            
                            // Stub from parent to bar
                            drawLine(edgeColor, Offset(p.centerX, p.bottom), Offset(p.centerX, yBar), strokeWidth = strokeW)
                            val barMidX = p.centerX
                            
                            // Children routing (same logic as two-parent case)
                            if (fam.childrenIds.isEmpty()) {
                                // No children
                            } else if (fam.childrenIds.size == 1) {
                                // Single child
                                val cid = fam.childrenIds.first()
                                val c = rectForConnection(cid)
                                if (c != null) {
                                    val childBarY = c.top - childBarGap
                                    val midY = (yBar + childBarY) / 2f
                                    val cx = c.centerX
                                    
                                    drawLine(edgeColor, Offset(barMidX, yBar), Offset(barMidX, midY), strokeWidth = strokeW)
                                    val hLeft = min(barMidX, cx)
                                    val hRight = max(barMidX, cx)
                                    drawLine(edgeColor, Offset(hLeft, midY), Offset(hRight, midY), strokeWidth = strokeW)
                                    drawLine(edgeColor, Offset(cx, midY), Offset(cx, childBarY), strokeWidth = strokeW)
                                    drawLine(edgeColor, Offset(cx, childBarY), Offset(cx, c.top), strokeWidth = strokeW)
                                }
                            } else {
                                // Multiple children
                                var minChildX = Float.POSITIVE_INFINITY
                                var maxChildX = Float.NEGATIVE_INFINITY
                                var minTopY = Float.POSITIVE_INFINITY
                                val childAnchors = mutableListOf<Pair<Float, Float>>()
                                
                                fam.childrenIds.forEach { cid ->
                                    val c = rectForConnection(cid)
                                    if (c != null) {
                                        val cx = c.centerX
                                        val topY = c.top
                                        childAnchors.add(Pair(cx, topY))
                                        if (cx < minChildX) minChildX = cx
                                        if (cx > maxChildX) maxChildX = cx
                                        if (topY < minTopY) minTopY = topY
                                    }
                                }
                                
                                if (childAnchors.isNotEmpty()) {
                                    val childBarY = minTopY - childBarGap
                                    val childMidX = (minChildX + maxChildX) / 2f
                                    val midY = (yBar + childBarY) / 2f
                                    
                                    drawLine(edgeColor, Offset(barMidX, yBar), Offset(barMidX, midY), strokeWidth = strokeW)
                                    val hLeft = min(barMidX, childMidX)
                                    val hRight = max(barMidX, childMidX)
                                    drawLine(edgeColor, Offset(hLeft, midY), Offset(hRight, midY), strokeWidth = strokeW)
                                    drawLine(edgeColor, Offset(childMidX, midY), Offset(childMidX, childBarY), strokeWidth = strokeW)
                                    drawLine(edgeColor, Offset(minChildX, childBarY), Offset(maxChildX, childBarY), strokeWidth = strokeW)
                                    childAnchors.forEach { (cx, topY) ->
                                        drawLine(edgeColor, Offset(cx, childBarY), Offset(cx, topY), strokeWidth = strokeW)
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // Nodes (поверх рёбер) - only render visible nodes
            // Use key() to stabilize node composition and avoid unnecessary recompositions
            visibleIndividuals.forEach { ind ->
                androidx.compose.runtime.key(ind.id) {
                    val r = rectFor(ind.id) ?: return@forEach
                    val leftDp = with(density) { r.x.toDp() }
                    val topDp = with(density) { r.y.toDp() }
                    val wDp = with(density) { r.w.toDp() }
                    val hDp = with(density) { r.h.toDp() }
                    var showNodeMenu by androidx.compose.runtime.remember(ind.id) { androidx.compose.runtime.mutableStateOf(false) }
                    
                    // Extract birth and death dates from events
                    val birthDate = extractEventDate(ind, "BIRT")
                    val deathDate = extractEventDate(ind, "DEAT")
                    
                    NodeCard(
                    firstName = ind.firstName,
                    lastName = ind.lastName,
                    gender = ind.gender,
                    birthDate = birthDate,
                    deathDate = deathDate,
                    x = leftDp,
                    y = topDp,
                    width = wDp,
                    height = hDp,
                    selected = (ind.id == selectedId),
                    fontScale = scale,
                    lastFsBaseSp = textFsFor(ind.id),
                    simplified = useSimplifiedRendering,
                    onClick = { onSelect(ind.id) },
                    onDrag = { delta -> onNodeDrag(ind.id, delta) },
                    onLongPress = { showNodeMenu = true },
                    onDoubleTap = { onEditPerson(ind.id) }
                ) {
                    DropdownMenu(expanded = showNodeMenu, onDismissRequest = { showNodeMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit person…") }, onClick = { showNodeMenu = false; onEditPerson(ind.id) })
                        DropdownMenuItem(text = { Text("Center on") }, onClick = { showNodeMenu = false; onCenterOn(ind.id) })
                        DropdownMenuItem(text = { Text("Reset") }, onClick = { showNodeMenu = false; onReset() })
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun NodeCard(
    firstName: String,
    lastName: String,
    gender: com.family.tree.core.model.Gender?,
    birthDate: String?,
    deathDate: String?,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    selected: Boolean,
    fontScale: Float,
    lastFsBaseSp: Float,
    simplified: Boolean,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    menuContent: @Composable () -> Unit
) {
    val borderColor = if (selected) Color(0xFF1976D2) else Color(0xFFB0BEC5)
    val borderWidth = if (selected) 3f else 2f
    
    // Gender-based background colors matching the screenshot
    val backgroundColor = when (gender) {
        com.family.tree.core.model.Gender.MALE -> Color(0xFFD0D0FF)      // Light blue/lavender for males
        com.family.tree.core.model.Gender.FEMALE -> Color(0xFFFFFFC0)    // Light yellow for females
        else -> Color(0xFFFAFAFA)                                         // Default light gray for unknown
    }
    
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            // Node drag gestures removed to allow canvas panning to work
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Скруглённая рамка у карточки узла
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = borderWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
        
        // Skip text rendering in simplified mode (small scale with many nodes)
        if (!simplified) {
            // Три строки: имя (первая), фамилия (вторая), даты (третья)
            val first = firstName
            val last = lastName
            
            // Используем те же формулы, что и в measureNodePx для полной синхронизации
            fun lhFactorForScale(s: Float): Float {
                val u = ((1.0f - s) / 0.6f).coerceIn(0f, 1f)
                return 1.10f - 0.10f * u
            }
            fun lineGapPxForScale(s: Float): Float {
                val u = ((1.0f - s) / 0.5f).coerceIn(0f, 1f)
                return (1.5f - 1.5f * u).coerceIn(0f, 1.5f)
            }
            
            val innerPadX = 4.5f * fontScale
            val innerPadY = 4.5f * fontScale
            val lineGapPx = lineGapPxForScale(fontScale)
            val lhFactor = lhFactorForScale(fontScale)
            val densityLocal = LocalDensity.current
            val innerPadXdp = with(densityLocal) { innerPadX.toDp() }
            val innerPadYdpClamped = with(densityLocal) { (4.5f * fontScale).coerceIn(1f, 4.5f).toDp() }
            val lineGapDp = with(densityLocal) { lineGapPx.toDp() }

            // Базовый кегль, который пришёл из измерителя, умноженный на текущий zoom
            val baseFsSp = lastFsBaseSp.sp * fontScale
            // На мобильных устройствах НЕ применяем минимумы, чтобы карточки могли реально уменьшаться
            // и текст масштабировался пропорционально карточке, избегая наложения при малых масштабах
            val effFsSp = baseFsSp
            
            // Межстрочный интервал пропорционален размеру шрифта
            val effLineHeightSp = effFsSp * lhFactor
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = innerPadXdp, vertical = innerPadYdpClamped),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Первая строка: имя (всегда показывается)
                Text(
                    first,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                    fontSize = effFsSp,
                    // Рисуем обычным (не жирным) начертанием, чтобы имена не выглядели «болдом» на канвасе
                    fontWeight = FontWeight.Normal,
                    lineHeight = effLineHeightSp,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    // Минимальная высота строки, чтобы избежать 0px на Android при округлениях
                    modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp)
                )
                // Вторая строка: фамилия (всегда резервируется место)
                androidx.compose.foundation.layout.Spacer(Modifier.height(lineGapDp))
                Text(
                    text = last.ifEmpty { "" },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = effFsSp,
                    lineHeight = effLineHeightSp,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp)
                )
                // Третья строка: даты (всегда резервируется место)
                val dateText = formatDates(birthDate, deathDate, firstName, lastName) ?: ""
                androidx.compose.foundation.layout.Spacer(Modifier.height(lineGapDp))
                Text(
                    text = dateText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontSize = effFsSp,
                    lineHeight = effLineHeightSp,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 1.dp)
                )
            }
        }
        // Встроенное контекстное меню для узла
        menuContent()
    }
}
