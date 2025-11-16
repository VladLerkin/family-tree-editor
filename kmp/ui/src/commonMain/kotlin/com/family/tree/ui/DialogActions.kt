package com.family.tree.ui

import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject

/**
 * Global dialog action endpoints for triggering platform file dialogs.
 * MainScreen assigns these lambdas during composition to control dialog state.
 * Android DesktopActions uses these instead of AWT FileDialog.
 */
object DialogActions {
    var triggerOpenDialog: ((LoadedProject?) -> Unit) -> Unit = {}
    var triggerSaveDialog: (ProjectData) -> Unit = {}
    var triggerRelImport: ((LoadedProject?) -> Unit) -> Unit = {}
    var triggerGedcomImport: ((ProjectData?) -> Unit) -> Unit = {}
    var triggerGedcomExport: (ProjectData) -> Unit = {}
    var triggerSvgExport: (ProjectData, Float, Offset) -> Unit = { _, _, _ -> }
    var triggerSvgExportFit: (ProjectData) -> Unit = {}
}
