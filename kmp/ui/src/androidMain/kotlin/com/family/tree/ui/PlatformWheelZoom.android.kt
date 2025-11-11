package com.family.tree.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Android actual: implements pinch-to-zoom and pan gestures for touch devices
// Optimized for large trees: throttles scale updates during active gestures to reduce recomposition overhead
actual fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize
): Modifier = this.composed {
    val coroutineScope = rememberCoroutineScope()
    var throttleJob by remember { mutableStateOf<Job?>(null) }
    var pendingScale by remember { mutableStateOf<Float?>(null) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    
    pointerInput(Unit) {
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
            
            // Update pan immediately for smooth visual feedback
            setPan(Offset(newPanX, newPanY) + panChange)
            
            // Throttle scale updates to reduce expensive recompositions during active zoom
            // Update immediately on first gesture, then throttle subsequent updates
            // Increased delays for large trees: 300ms interval, 100ms batch delay
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime > 300) {
                // More than 300ms since last update: apply immediately
                setScale(newScale)
                lastUpdateTime = currentTime
                pendingScale = null
                // Cancel any pending job since we applied immediately
                throttleJob?.cancel()
                throttleJob = null
            } else {
                // Within throttle window: defer update
                pendingScale = newScale
                throttleJob?.cancel()
                throttleJob = coroutineScope.launch {
                    delay(100) // Wait 100ms before applying batched update
                    pendingScale?.let { scale ->
                        setScale(scale)
                        lastUpdateTime = System.currentTimeMillis()
                        pendingScale = null
                    }
                }
            }
        }
    }
}
