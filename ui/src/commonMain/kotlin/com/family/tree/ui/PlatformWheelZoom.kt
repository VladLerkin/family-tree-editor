package com.family.tree.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Cross-platform modifier hook for wheel/trackpad zoom.
 * - commonMain declares the API;
 * - desktopMain provides real implementation;
 * - androidMain returns the same modifier (no-op for now).
 */
expect fun Modifier.platformWheelZoom(
    getScale: () -> Float,
    setScale: (Float) -> Unit,
    getPan: () -> Offset,
    setPan: (Offset) -> Unit,
    getCanvasSize: () -> IntSize,
    leftPanelWidth: Float = 0f
): Modifier
