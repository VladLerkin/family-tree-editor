package com.family.tree.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize,
    leftPanelWidth: Float
): Modifier = this.onPointerEvent(PointerEventType.Scroll) { event ->
    val first = event.changes.firstOrNull() ?: return@onPointerEvent
    val deltaY = first.scrollDelta.y
    if (deltaY == 0f) return@onPointerEvent

    // Base zoom step: negative deltaY is scroll up (zoom in)
    val currentScale = getScale()
    val step = if (deltaY < 0f) 1.1f else 0.9f
    val newScale = (currentScale * step).coerceIn(0.25f, 4f)

    // Keep the point under cursor fixed by adjusting pan
    // Adjust cursor position to be relative to canvas (subtract left panel width)
    val cursorRaw = first.position
    val cursorCanvas = Offset(cursorRaw.x - leftPanelWidth, cursorRaw.y)
    val pan = getPan()
    val k = newScale / (currentScale.takeIf { it != 0f } ?: 1f)
    val newPan = Offset(
        x = cursorCanvas.x - (cursorCanvas.x - pan.x) * k,
        y = cursorCanvas.y - (cursorCanvas.y - pan.y) * k
    )

    setScale(newScale)
    setPan(newPan)
}