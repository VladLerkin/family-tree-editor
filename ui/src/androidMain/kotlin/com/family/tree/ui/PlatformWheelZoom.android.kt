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
// Important: apply scale updates immediately during gesture so zoom occurs around the fingers' midpoint
actual fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize,
    leftPanelWidth: Float
): Modifier = this.composed {
    // NOTE: We intentionally avoid throttling scale updates here.
    // Throttling caused pan to be computed against a scale that wasn't yet applied,
    // which shifted the focal point and felt like zooming "not under the fingers".
    pointerInput(Unit) {
        detectTransformGestures { centroid, panChange, zoomChange, _ ->
            val oldScale = getScale()
            val newScale = (oldScale * zoomChange).coerceIn(0.25f, 4f)

            // Adjust centroid position to be relative to canvas (subtract left panel width)
            val centroidCanvas = Offset(centroid.x - leftPanelWidth, centroid.y)
            
            // Our app uses screen-space pan (in pixels), applied as: screen = world * scale + pan
            // To keep the point under fingers (centroid) stationary on screen, adjust pan in screen space:
            // newPan = centroid - (centroid - pan) * (newScale / oldScale)
            val pan = getPan()
            val k = if (oldScale != 0f) newScale / oldScale else 1f
            val zoomCompensatedPan = Offset(
                x = centroidCanvas.x - (centroidCanvas.x - pan.x) * k,
                y = centroidCanvas.y - (centroidCanvas.y - pan.y) * k
            )

            // Also apply the actual panning reported by the gesture recognizer (in pixels)
            val newPan = zoomCompensatedPan + panChange

            // Apply immediately to keep zoom centered between fingers
            setScale(newScale)
            setPan(newPan)
        }
    }
}
