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
import androidx.compose.runtime.getValue
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
    val positions = SimpleTreeLayout.layout(data.individuals, data.families)
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
    fun measureNodePx(displayName: String): NodeMeasure {
        fun lhFactorForScale(s: Float): Float {
            // Aggressive mid-scale compression: ~1.10 at s>=1.0 → ~1.02 at s=0.6 → ~1.00 at s<=0.4
            val u = ((1.0f - s) / 0.6f).coerceIn(0f, 1f) // 0 at 1.0, 1 at 0.4
            return 1.10f - 0.10f * u
        }
        fun padYpxForScale(s: Float): Float = (9f * s).coerceIn(1f, 8f)
        fun lineGapPxForScale(s: Float): Float {
            // Drop from 1.5px at s>=1.0 to ~0px at s<=0.5
            val u = ((1.0f - s) / 0.5f).coerceIn(0f, 1f)
            return (1.5f - 1.5f * u).coerceIn(0f, 1.5f)
        }
        // Минимальные размеры + паддинги масштабируются с зумом; измеряем две строки: имя и фамилия
        // Увеличены в 3 раза для более крупного отображения персон
        val baseMinW = 132f * 3f
        val baseMinH = 60f * 3f
        val extraFudge = 0f
        val minW = baseMinW * scale
        val minH = baseMinH * scale
        // Синхронизация с отрисовкой: горизонтальный паддинг ~12px*scale, вертикальный ~9px*scale с ограничением
        // Увеличены в 3 раза для соответствия увеличенным размерам
        val padX = 12f * 3f * scale
        val padY = padYpxForScale(scale) * 3f
        val lineGap = lineGapPxForScale(scale) * 3f
        val tokens = displayName.trim().split(Regex("\\s+"))
        val first = tokens.firstOrNull() ?: ""
        val last = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else ""
        val maxTextWidth = (minW - padX * 2).toInt().coerceAtLeast(1)
        // Подбираем общий кегль (sp) для имени и фамилии, чтобы обе строки гарантированно поместились
        // Увеличены в 3 раза для соответствия увеличенным размерам прямоугольников
        val candidates = listOf(36f, 34.5f, 33f, 31.5f, 30f, 28.5f, 27f, 25.5f, 24f, 22.5f, 21f, 19.5f, 18f, 16.5f, 15f)
        var chosenFsBaseSp = 30f
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
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxTextWidth)
            )
            val rLast = if (last.isNotBlank()) measurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(last),
                style = lastStyle,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = maxTextWidth)
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
        val w = max(minW, contentW + padX * 2)
        val h = max(minH, contentH + padY * 2)
        return NodeMeasure(w, h, chosenFsBaseSp)
    }

    // Предрасчёт прямоугольников всех узлов с учётом измерений
    val measures: Map<IndividualId, NodeMeasure> = buildMap {
        data.individuals.forEach { ind ->
            put(ind.id, measureNodePx(ind.displayName))
        }
    }

    val rects: Map<IndividualId, RectF> = buildMap {
        data.individuals.forEach { ind ->
            val m = measures[ind.id] ?: return@forEach
            // If nodeOffsets has this individual, use it as absolute position (from imported layout)
            // Otherwise fall back to auto-layout position from SimpleTreeLayout
            val off = nodeOffsets[ind.id]
            val (baseX, baseY) = if (off != null) {
                // Use nodeOffsets as absolute position (imported coordinates)
                Pair(off.x, off.y)
            } else {
                // Use auto-layout position
                val p = positions[ind.id] ?: return@forEach
                Pair(p.x * scale, p.y * scale)
            }
            put(ind.id, RectF(baseX, baseY, m.w, m.h))
        }
    }

    fun rectFor(id: IndividualId): RectF? = rects[id]
    fun textFsFor(id: IndividualId): Float = measures[id]?.baseFsSp ?: 30f

    fun marriageBarRect(a: RectF, b: RectF): RectF {
        val left = min(a.centerX, b.centerX)
        val right = max(a.centerX, b.centerX)
        val y = max(a.bottom, b.bottom) + 24f // 8f * 3
        return RectF(left, y, right - left, 6f) // 2f * 3
    }

    fun marriageMid(bar: RectF): Offset = Offset(bar.x + bar.w / 2f, bar.y + bar.h / 2f)

    fun routeVH(from: Offset, child: RectF): List<Offset> {
        val gap = 6f
        val topAnchor = Offset(child.centerX, child.top - gap)
        return listOf(
            from,
            Offset(from.x, topAnchor.y),
            topAnchor,
            Offset(child.centerX, child.top)
        )
    }

    Box(Modifier.onSizeChanged { onCanvasSize(it) }) {
        // Background grid
        if (showGrid) {
            Canvas(Modifier.fillMaxSize()) {
                val spacingBase = 64f
                val spacing = (spacingBase * scale).coerceAtLeast(20f)
                val w = size.width
                val h = size.height
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
        }

        // Pan offset container
        Box(
            modifier = Modifier
                .offset { IntOffset(pan.x.roundToInt(), pan.y.roundToInt()) }
        ) {
            // Edges (сначала рёбра)
            Canvas(Modifier.fillMaxSize()) {
                val edgeColor = Color(0xFF607D8B)
                val gap = 8f
                data.families.forEach { fam ->
                    val husband = fam.husbandId?.let { rectFor(it) }
                    val wife = fam.wifeId?.let { rectFor(it) }
                    when {
                        husband != null && wife != null -> {
                            // Bar y just below lower parent bottom
                            val yBar = max(husband.bottom, wife.bottom) + gap
                            val xLeft = min(husband.centerX, wife.centerX)
                            val xRight = max(husband.centerX, wife.centerX)
                            // Short vertical stubs from each parent bottom to bar
                            drawLine(edgeColor, Offset(husband.centerX, husband.bottom), Offset(husband.centerX, yBar), strokeWidth = 2f * lineWidth)
                            drawLine(edgeColor, Offset(wife.centerX, wife.bottom), Offset(wife.centerX, yBar), strokeWidth = 2f * lineWidth)
                            // Marriage bar between parent stubs
                            drawLine(edgeColor, Offset(xLeft, yBar), Offset(xRight, yBar), strokeWidth = 2f * lineWidth)
                            val mid = Offset((xLeft + xRight) / 2f, yBar)
                            // Children: orthogonal route from bar mid to child top
                            fam.childrenIds.forEach { cid ->
                                val c = rectFor(cid) ?: return@forEach
                                val topY = c.top - 6f
                                // vertical from bar mid to above child
                                drawLine(edgeColor, mid, Offset(mid.x, topY), strokeWidth = 2f * lineWidth)
                                // horizontal to child centerX
                                drawLine(edgeColor, Offset(mid.x, topY), Offset(c.centerX, topY), strokeWidth = 2f * lineWidth)
                                // short vertical down to child top edge
                                drawLine(edgeColor, Offset(c.centerX, topY), Offset(c.centerX, c.top), strokeWidth = 2f * lineWidth)
                            }
                        }
                        husband != null || wife != null -> {
                            // Single parent case: use that parent's bottom center as bus point
                            val p = husband ?: wife!!
                            val yBar = p.bottom + gap
                            // Stub
                            drawLine(edgeColor, Offset(p.centerX, p.bottom), Offset(p.centerX, yBar), strokeWidth = 2f * lineWidth)
                            val mid = Offset(p.centerX, yBar)
                            fam.childrenIds.forEach { cid ->
                                val c = rectFor(cid) ?: return@forEach
                                val topY = c.top - 6f
                                drawLine(edgeColor, mid, Offset(mid.x, topY), strokeWidth = 2f * lineWidth)
                                drawLine(edgeColor, Offset(mid.x, topY), Offset(c.centerX, topY), strokeWidth = 2f * lineWidth)
                                drawLine(edgeColor, Offset(c.centerX, topY), Offset(c.centerX, c.top), strokeWidth = 2f * lineWidth)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            // Nodes (поверх рёбер)
            data.individuals.forEach { ind ->
                val r = rectFor(ind.id) ?: return@forEach
                val leftDp = with(density) { r.x.toDp() }
                val topDp = with(density) { r.y.toDp() }
                val wDp = with(density) { r.w.toDp() }
                val hDp = with(density) { r.h.toDp() }
                var showNodeMenu by androidx.compose.runtime.remember(ind.id) { androidx.compose.runtime.mutableStateOf(false) }
                NodeCard(
                    name = ind.displayName,
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
    name: String,
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
    Box(
        modifier = Modifier
            .offset(x, y)
            .size(width, height)
            .background(Color(0xFFFAFAFA))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        onDrag(dragAmount)
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Скруглённая рамка у карточки узла
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = borderWidth),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
        }
        // Две строки: имя (первая), фамилия (вторая). Разделяем так же, как в measureNodePx: первая токен, остальное — фамилия.
        val tokens = name.trim().split(Regex("\\s+"))
        val first = tokens.firstOrNull() ?: ""
        val last = if (tokens.size > 1) tokens.drop(1).joinToString(" ") else ""
        
        // Используем те же формулы, что и в measureNodePx для полной синхронизации
        fun lhFactorForScale(s: Float): Float {
            val u = ((1.0f - s) / 0.6f).coerceIn(0f, 1f)
            return 1.10f - 0.10f * u
        }
        fun lineGapPxForScale(s: Float): Float {
            val u = ((1.0f - s) / 0.5f).coerceIn(0f, 1f)
            return (1.5f - 1.5f * u).coerceIn(0f, 1.5f)
        }
        
        val innerPadX = 12f * fontScale
        val innerPadY = 9f * fontScale
        val lineGapPx = lineGapPxForScale(fontScale)
        val lhFactor = lhFactorForScale(fontScale)
        val densityLocal = LocalDensity.current
        val innerPadXdp = with(densityLocal) { innerPadX.toDp() }
        val innerPadYdpClamped = with(densityLocal) { (9f * fontScale).coerceIn(1f, 8f).toDp() }
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Встроенное контекстное меню для узла
        menuContent()
    }
}
