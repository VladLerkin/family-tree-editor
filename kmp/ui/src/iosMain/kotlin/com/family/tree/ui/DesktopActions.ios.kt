package com.family.tree.ui

import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject

actual object DesktopActions {
    actual fun openPed(onLoaded: (LoadedProject?) -> Unit) {
        // Trigger the platform file dialog via DialogActions
        DialogActions.triggerOpenDialog(onLoaded)
    }
    actual fun savePed(data: ProjectData): Boolean {
        // Trigger the platform save dialog via DialogActions
        DialogActions.triggerSaveDialog(data)
        return true
    }
    actual fun importRel(onLoaded: (LoadedProject?) -> Unit) {
        // Same as openPed - trigger the platform file dialog
        DialogActions.triggerOpenDialog(onLoaded)
    }
    actual fun importGedcom(onLoaded: (ProjectData?) -> Unit) {
        // Trigger the platform file dialog via DialogActions
        DialogActions.triggerGedcomImport(onLoaded)
    }
    actual fun exportGedcom(data: ProjectData): Boolean {
        // Trigger the platform save dialog via DialogActions
        DialogActions.triggerGedcomExport(data)
        return true
    }
    actual fun exportSvg(project: ProjectData, scale: Float, pan: Offset): Boolean {
        DialogActions.triggerSvgExport(project, scale, pan)
        return true
    }
    actual fun exportSvgFit(project: ProjectData): Boolean {
        DialogActions.triggerSvgExportFit(project)
        return true
    }
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
