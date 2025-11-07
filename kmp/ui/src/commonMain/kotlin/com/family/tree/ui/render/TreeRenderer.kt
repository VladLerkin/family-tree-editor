package com.family.tree.ui.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    onCanvasSize: (IntSize) -> Unit,
    showGrid: Boolean,
    lineWidth: Float
) {
    println("[DEBUG_LOG] TreeRenderer: rendering with ${data.individuals.size} individuals, ${data.families.size} families, scale=$scale, pan=$pan")
    
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
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
    fun measureNodePx(firstName: String, lastName: String): NodeMeasure {
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
        // Минимальные размеры + паддинги масштабируются с зумом; измеряем две строки: имя и фамилия
        val baseMinW = 74.25f
        val baseMinH = 33.75f
        val extraFudge = 0f
        val minW = baseMinW * scale
        val minH = baseMinH * scale
        // Синхронизация с отрисовкой: горизонтальный паддинг ~4.5px*scale, вертикальный ~4.5px*scale с ограничением
        val padX = 4.5f * scale
        val padY = padYpxForScale(scale)
        val lineGap = lineGapPxForScale(scale)
        val first = firstName
        val last = lastName
        // Подбираем общий кегль (sp) для имени и фамилии, чтобы обе строки гарантированно поместились
        val candidates = listOf(3.75f, 3.375f, 3f, 2.625f, 2.25f, 1.875f, 1.5f, 1.3125f, 1.125f)
        var chosenFsBaseSp = 3f
        var resFirstHeight = 0
        var resFirstWidth = 0
        var resLastHeight = 0
        var resLastWidth = 0
        for (fsBase in candidates) {
            val fs = (fsBase.sp * scale)
            val lhFactor = lhFactorForScale(scale)
            val nameStyle = TextStyle(
                fontSize = fs,
                fontWeight = FontWeight.Medium,
                lineHeight = fs * lhFactor
            )
            val lastStyle = TextStyle(
                fontSize = fs,
                lineHeight = fs * lhFactor
            )
            val rFirst = measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(first),
                style = nameStyle,
                softWrap = false
            )
            val rLast = if (last.isNotBlank()) measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(last),
                style = lastStyle,
                softWrap = false
            ) else null
            val contentH = rFirst.size.height + (if (last.isNotBlank()) lineGap.toInt() + (rLast?.size?.height ?: 0) else 0) + extraFudge
            val totalH = contentH + padY * 2
            resFirstHeight = rFirst.size.height
            resFirstWidth = rFirst.size.width
            resLastHeight = rLast?.size?.height ?: 0
            resLastWidth = rLast?.size?.width ?: 0
            chosenFsBaseSp = fsBase
            if (totalH <= minH) break
        }
        val contentW = max(resFirstWidth, resLastWidth)
        val contentH = resFirstHeight + (if (last.isNotEmpty()) lineGap.toInt() + resLastHeight else 0) + extraFudge
        // Add extra width buffer (8 * scale) to ensure text fits completely without cutoff
        val w = max(minW, contentW + padX * 2 + 8f * scale)
        val h = max(minH, contentH + padY * 2)
        return NodeMeasure(w, h, chosenFsBaseSp)
    }

    // Предрасчёт прямоугольников всех узлов с учётом измерений
    // Memoize to avoid expensive text measurement on every recomposition
    val measures: Map<IndividualId, NodeMeasure> = remember(data.individuals, scale, measurer) {
        buildMap {
            data.individuals.forEach { ind ->
                put(ind.id, measureNodePx(ind.firstName, ind.lastName))
            }
        }
    }

    val rects: Map<IndividualId, RectF> = remember(data.individuals, positions, nodeOffsets, scale, measures) {
        buildMap {
            data.individuals.forEach { ind ->
                val m = measures[ind.id] ?: return@forEach
                // If nodeOffsets has this individual, use it as absolute position (from imported layout)
                // Otherwise fall back to auto-layout position from SimpleTreeLayout
                val off = nodeOffsets[ind.id]
                val (baseX, baseY) = if (off != null) {
                    // Use nodeOffsets as absolute position (imported coordinates), scaled
                    Pair(off.x * scale, off.y * scale)
                } else {
                    // Use auto-layout position
                    val p = positions[ind.id] ?: return@forEach
                    Pair(p.x * scale, p.y * scale)
                }
                put(ind.id, RectF(baseX, baseY, m.w, m.h))
            }
        }
    }

    fun rectFor(id: IndividualId): RectF? = rects[id]
    fun textFsFor(id: IndividualId): Float = measures[id]?.baseFsSp ?: 3f
    
    // Viewport culling: check if rectangle intersects viewport
    // Rects are in world space (scaled positions), rendered at screen position = worldPos + pan
    // For visibility: worldPos + pan must intersect screen bounds [0, canvasSize]
    // Therefore: worldPos.right + pan.x >= -margin  AND  worldPos.left + pan.x <= canvasSize.width + margin
    fun isVisible(r: RectF): Boolean {
        val screenLeft = r.left + pan.x
        val screenRight = r.right + pan.x
        val screenTop = r.top + pan.y
        val screenBottom = r.bottom + pan.y
        val margin = 200f
        return screenRight >= -margin && screenLeft <= canvasSize.width + margin &&
               screenBottom >= -margin && screenTop <= canvasSize.height + margin
    }
    
    // Filter visible individuals for rendering - memoize based on viewport bounds and rects
    val visibleIndividuals = remember(rects, pan, canvasSize) {
        data.individuals.filter { ind ->
            rectFor(ind.id)?.let { isVisible(it) } ?: false
        }.also {
            println("[DEBUG_LOG] TreeRenderer: viewport culling reduced ${data.individuals.size} to ${it.size} visible individuals")
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

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()  // Clip rendering to green canvas boundaries
            .background(Color(0xFFD5E8D4))  // Light mint green background matching screenshot
            .onSizeChanged { 
                canvasSize = it
                onCanvasSize(it) 
            }
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
                val strokeW = 2f * scale  // line width (scaled to match JavaFX)
                
                // Build set of visible IDs for fast lookup
                val visibleIds = visibleIndividuals.map { it.id }.toSet()
                
                data.families.forEach { fam ->
                    // Skip family if none of its members are visible
                    val hasVisibleMember = (fam.husbandId in visibleIds) || 
                                          (fam.wifeId in visibleIds) || 
                                          fam.childrenIds.any { it in visibleIds }
                    if (!hasVisibleMember) return@forEach
                    val husband = fam.husbandId?.let { rectFor(it) }
                    val wife = fam.wifeId?.let { rectFor(it) }
                    
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
                                val c = rectFor(cid)
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
                                    val c = rectFor(cid)
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
                                val c = rectFor(cid)
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
                                    val c = rectFor(cid)
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
            visibleIndividuals.forEach { ind ->
                val r = rectFor(ind.id) ?: return@forEach
                val leftDp = with(density) { r.x.toDp() }
                val topDp = with(density) { r.y.toDp() }
                val wDp = with(density) { r.w.toDp() }
                val hDp = with(density) { r.h.toDp() }
                var showNodeMenu by androidx.compose.runtime.remember(ind.id) { androidx.compose.runtime.mutableStateOf(false) }
                NodeCard(
                    firstName = ind.firstName,
                    lastName = ind.lastName,
                    gender = ind.gender,
                    x = leftDp,
                    y = topDp,
                    width = wDp,
                    height = hDp,
                    selected = (ind.id == selectedId),
                    fontScale = scale,
                    lastFsBaseSp = textFsFor(ind.id),
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

@Composable
private fun NodeCard(
    firstName: String,
    lastName: String,
    gender: com.family.tree.core.model.Gender?,
    x: Dp,
    y: Dp,
    width: Dp,
    height: Dp,
    selected: Boolean,
    fontScale: Float,
    lastFsBaseSp: Float,
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
            // Removed detectDragGestures to avoid consuming touch events that should go to parent's detectTransformGestures
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Скруглённая рамка у карточки узла
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = borderWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
        // Две строки: имя (первая), фамилия (вторая) - как в JavaFX версии
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
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = innerPadXdp, vertical = innerPadYdpClamped),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                first,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                fontSize = lastFsBaseSp.sp * fontScale,
                fontWeight = FontWeight.Medium,
                lineHeight = lastFsBaseSp.sp * fontScale * lhFactor,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (last.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(lineGapDp))
                Text(
                    last,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    fontSize = lastFsBaseSp.sp * fontScale,
                    lineHeight = lastFsBaseSp.sp * fontScale * lhFactor,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Встроенное контекстное меню для узла
        menuContent()
    }
}
