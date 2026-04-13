package com.family.tree.desktop

import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.family.tree.core.di.initKoin
import com.family.tree.core.di.platformModule
import com.family.tree.ui.App
import com.family.tree.ui.AppActions
import com.family.tree.ui.di.uiModule

fun main() {
    initKoin(
        additionalModules = listOf(uiModule, platformModule)
    )
    application {
        Window(onCloseRequest = ::exitApplication, title = "Family Tree") {
        MenuBar {
            Menu("File") {
                Item("New") { AppActions.newProject() }
                Item("Open") { AppActions.openPed() }
                Item("Save") { AppActions.savePed() }
                Separator()
                Menu("Import") {
                    Item(".rel") { AppActions.importRel() }
                    Item("GEDCOM") { AppActions.importGedcom() }
                }
                Separator()
                Menu("Export") {
                    Item("SVG (Current View)") { AppActions.exportSvgCurrent() }
                    Item("SVG (Fit to Content)") { AppActions.exportSvgFit() }
                    Separator()
                    Item("PNG (Current View)") { AppActions.exportPngCurrent() }
                    Item("PNG (Fit to Content)") { AppActions.exportPngFit() }
                    Separator()
                    Item("GEDCOM") { AppActions.exportGedcom() }
                    Item("Markdown Tree") { AppActions.exportMarkdownTree() }
                }
                Separator()
                Item("Exit") { AppActions.exit() }
            }
            Menu("Edit") {
                Item("Manage Sources...") { AppActions.manageSources() }
            }
            Menu("View") {
                Item("Zoom In") { AppActions.zoomIn() }
                Item("Zoom Out") { AppActions.zoomOut() }
                Item("Reset / Fit") { AppActions.reset() }
            }
            Menu("AI Tools") {
                Item("Import AI Text") { AppActions.importAiText() }
                Item("Voice Input 🎤") { AppActions.voiceInput() }
                Separator()
                Item("AI Settings...") { AppActions.showAiSettings() }
                Item("Autoresearch Agent...") { AppActions.showAutoSearch() }
            }
            Menu("Help") {
                Item("About") { AppActions.showAbout() }
            }
        }
        App()
        }
    }
}
