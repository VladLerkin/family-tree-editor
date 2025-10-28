package com.family.tree.desktop

import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.family.tree.ui.App
import com.family.tree.ui.AppActions

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Family Tree") {
        MenuBar {
            Menu("File") {
                Item("Open") { AppActions.openPed() }
                Item("Save") { AppActions.savePed() }
                Separator()
                Item("Import .rel") { AppActions.importRel() }
                Separator()
                Menu("Export") {
                    Item("SVG (Current View)") { AppActions.exportSvgCurrent() }
                    Item("SVG (Fit to Content)") { AppActions.exportSvgFit() }
                    Separator()
                    Item("PNG (Current View)") { AppActions.exportPngCurrent() }
                    Item("PNG (Fit to Content)") { AppActions.exportPngFit() }
                }
            }
            Menu("View") {
                Item("Grid On/Off") { AppActions.toggleGrid() }
                Menu("Lines") {
                    Item("1x") { AppActions.setLineWidth1x() }
                    Item("2x") { AppActions.setLineWidth2x() }
                }
                Separator()
                Item("Zoom In") { AppActions.zoomIn() }
                Item("Zoom Out") { AppActions.zoomOut() }
                Item("Reset / Fit") { AppActions.reset() }
            }
        }
        App()
    }
}
