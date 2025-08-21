package com.pedigree.app;

import com.pedigree.services.ProjectService;
import com.pedigree.services.UndoRedoService;
import com.pedigree.ui.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        ProjectService projectService = new ProjectService();
        UndoRedoService undoRedoService = new UndoRedoService();

        MainWindow mainWindow = new MainWindow(projectService, undoRedoService);
        mainWindow.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
