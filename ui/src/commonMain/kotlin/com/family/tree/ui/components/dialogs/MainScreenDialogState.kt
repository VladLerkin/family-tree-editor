package com.family.tree.ui.components.dialogs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.family.tree.core.ProjectData
import com.family.tree.core.io.LoadedProject
import com.family.tree.core.model.FamilyId
import com.family.tree.core.model.IndividualId

class MainScreenDialogState {
    // File dialog state for Android/platform file pickers
    var showOpenDialog by mutableStateOf(false)
    var showSaveDialog by mutableStateOf(false)
    var pendingOpenCallback by mutableStateOf<((LoadedProject?) -> Unit)?>(null)
    var pendingSaveData by mutableStateOf<ProjectData?>(null)
    
    // .rel import dialog state
    var showRelImportDialog by mutableStateOf(false)
    var pendingRelImportCallback by mutableStateOf<((LoadedProject?) -> Unit)?>(null)
    
    // .rel import progress state
    var showRelImportProgress by mutableStateOf(false)
    var relImportProgressMessage by mutableStateOf("")
    
    // .rel import error state
    var showRelImportError by mutableStateOf(false)
    var relImportErrorMessage by mutableStateOf("")
    
    // GEDCOM dialog state
    var showGedcomImportDialog by mutableStateOf(false)
    var showGedcomExportDialog by mutableStateOf(false)
    var pendingGedcomImportCallback by mutableStateOf<((ProjectData?) -> Unit)?>(null)
    var pendingGedcomExportData by mutableStateOf<ProjectData?>(null)
    
    // Markdown Tree export dialog state
    var showMarkdownExportDialog by mutableStateOf(false)
    var pendingMarkdownExportData by mutableStateOf<ProjectData?>(null)
    
    // SVG export dialog state
    var showSvgExportDialog by mutableStateOf(false)
    var showSvgExportFitDialog by mutableStateOf(false)
    var pendingSvgExportData by mutableStateOf<Triple<ProjectData, Float, Offset>?>(null)
    var pendingSvgExportFitData by mutableStateOf<ProjectData?>(null)
    
    // AI text import dialog state
    var showAiTextImportDialog by mutableStateOf(false)
    var pendingAiTextImportCallback by mutableStateOf<((LoadedProject?) -> Unit)?>(null)
    
}
