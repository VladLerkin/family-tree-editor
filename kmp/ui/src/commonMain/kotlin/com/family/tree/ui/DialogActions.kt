package com.family.tree.ui

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
}
