package com.family.tree.ui

import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject

expect object DesktopActions {
    fun openPed(onLoaded: (LoadedProject?) -> Unit)
    fun savePed(data: ProjectData): Boolean
    fun importRel(onLoaded: (LoadedProject?) -> Unit)
    fun importGedcom(onLoaded: (ProjectData?) -> Unit)
    fun exportGedcom(data: ProjectData): Boolean
    fun exportSvg(project: ProjectData, scale: Float, pan: Offset): Boolean
    fun exportSvgFit(project: ProjectData): Boolean
    // With options
    fun exportSvgWithOptions(
        project: ProjectData,
        scale: Float,
        pan: Offset,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean
    fun exportSvgFitWithOptions(
        project: ProjectData,
        margins: Float,
        background: Boolean,
        lineWidth: Float
    ): Boolean
    // PNG export (Desktop only; Android no-op)
    fun exportPng(project: ProjectData, scale: Float, pan: Offset): Boolean
    fun exportPngFit(project: ProjectData): Boolean
}
