package com.family.tree.ui

import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.layout.ProjectLayout

/**
 * Web (WasmJs) implementation of DesktopActions.
 * Uses DialogActions to trigger platform file dialogs (same pattern as Android).
 */
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

    actual fun savePedWithLayout(data: ProjectData, layout: ProjectLayout): Boolean {
        // For web, we save with layout by delegating to savePed
        // The MainScreen will build the layout and call this method
        DialogActions.triggerSaveDialog(data)
        return true
    }

    actual fun importRel(onLoaded: (LoadedProject?) -> Unit) {
        // Trigger the dedicated .rel import dialog
        DialogActions.triggerRelImport(onLoaded)
    }

    actual fun importGedcom(onLoaded: (ProjectData?) -> Unit) {
        // Trigger the platform file dialog via DialogActions
        DialogActions.triggerGedcomImport(onLoaded)
    }

    actual fun importAiText(onLoaded: (LoadedProject?) -> Unit, onProgress: (String) -> Unit) {
        // Trigger the platform file dialog via DialogActions
        // Note: Web version doesn't support progress updates yet (uses platform file picker)
        DialogActions.triggerAiTextImport(onLoaded)
    }

    actual fun exportGedcom(data: ProjectData): Boolean {
        // Trigger the platform save dialog via DialogActions
        DialogActions.triggerGedcomExport(data)
        return true
    }

    actual fun exportSvg(project: ProjectData, scale: Float, pan: Offset): Boolean {
        // Trigger SVG export dialog
        DialogActions.triggerSvgExport(project, scale, pan)
        return true
    }

    actual fun exportSvgFit(project: ProjectData): Boolean {
        // Trigger SVG export (fit to content) dialog
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
    ): Boolean {
        // Not implemented for web yet
        return false
    }

    actual fun exportSvgFitWithOptions(
        project: ProjectData,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean {
        // Not implemented for web yet
        return false
    }

    actual fun exportPng(project: ProjectData, scale: Float, pan: Offset): Boolean {
        // PNG export not supported on web (requires canvas rendering)
        return false
    }

    actual fun exportPngFit(project: ProjectData): Boolean {
        // PNG export not supported on web (requires canvas rendering)
        return false
    }
}
