package com.family.tree.app

import com.family.tree.services.ProjectService
import com.family.tree.services.UndoRedoService
import com.family.tree.ui.MainWindow
import javafx.application.Application
import javafx.stage.Stage

class MainApplication : Application() {
    override fun start(primaryStage: Stage) {
        val projectService = ProjectService()
        val undoRedoService = UndoRedoService()

        val mainWindow = MainWindow(projectService, undoRedoService)
        mainWindow.show(primaryStage)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(MainApplication::class.java, *args)
        }
    }
}
