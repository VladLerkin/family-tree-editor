package com.family.tree.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

// iOS actual: implements pinch-to-zoom and pan gestures for touch devices
actual fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize
): Modifier = this.pointerInput(Unit) {
    detectTransformGestures { centroid, panChange, zoomChange, _ ->
        // Apply zoom change
        val currentScale = getScale()
        val newScale = (currentScale * zoomChange).coerceIn(0.25f, 4f)
        
        // Zoom toward centroid (touch point)
        // Before zoom: point P in world coords = (screenX - pan.x) / scale
        // After zoom: we want P to stay at same screen position
        // So: (screenX - newPan.x) / newScale = (screenX - pan.x) / scale
        // Solving: newPan.x = screenX - ((screenX - pan.x) / scale) * newScale
        val pan = getPan()
        val worldX = (centroid.x - pan.x) / currentScale
        val worldY = (centroid.y - pan.y) / currentScale
        val newPanX = centroid.x - worldX * newScale
        val newPanY = centroid.y - worldY * newScale
        
        setScale(newScale)
        setPan(Offset(newPanX, newPanY) + panChange)
    }
}
