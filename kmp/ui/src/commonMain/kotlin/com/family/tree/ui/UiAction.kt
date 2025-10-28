package com.family.tree.ui

import androidx.compose.ui.geometry.Offset

sealed class UiAction {
    // File
    data object OpenJson : UiAction()
    data object SaveJson : UiAction()
    data object OpenRel : UiAction()
    data class SaveRel(val request: Unit = Unit) : UiAction()
    data class ExportSvg(val useFit: Boolean = false) : UiAction()

    // View
    data object ToggleGrid : UiAction()
    data class SetLineWidth(val width: Float) : UiAction()
    data object ZoomIn : UiAction()
    data object ZoomOut : UiAction()
    data object Reset : UiAction() // fit to view
}
