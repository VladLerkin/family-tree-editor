package com.family.tree.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

// iOS actual: currently no wheel/trackpad zoom; return the original modifier unchanged.
actual fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize
): Modifier = this
