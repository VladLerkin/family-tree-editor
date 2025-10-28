package com.family.tree.ui

import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject

actual object DesktopActions {
    actual fun openPed(onLoaded: (LoadedProject?) -> Unit) {
        onLoaded(null)
    }
    actual fun savePed(data: ProjectData): Boolean = false
    actual fun importRel(onLoaded: (LoadedProject?) -> Unit) {
        onLoaded(null)
    }
    actual fun exportSvg(project: ProjectData, scale: Float, pan: Offset): Boolean = false
    actual fun exportSvgFit(project: ProjectData): Boolean = false
    actual fun exportSvgWithOptions(
        project: ProjectData,
        scale: Float,
        pan: Offset,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean = false
    actual fun exportSvgFitWithOptions(
        project: ProjectData,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean = false
    actual fun exportPng(project: ProjectData, scale: Float, pan: Offset): Boolean = false
    actual fun exportPngFit(project: ProjectData): Boolean = false
}
