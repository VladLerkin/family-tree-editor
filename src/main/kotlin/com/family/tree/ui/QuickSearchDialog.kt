package com.family.tree.ui

import com.family.tree.model.Individual
import com.family.tree.search.QuickSearchService
import com.family.tree.storage.ProjectRepository
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.stage.Modality
import javafx.stage.Stage
import java.util.function.Consumer

class QuickSearchDialog(private val data: ProjectRepository.ProjectData, private val onChosen: Consumer<String>?) {

    fun show() {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Quick Search"

        val root = BorderPane()
        root.padding = Insets(10.0)

        val query = TextField()
        query.promptText = "Type a name..."
        val list = ListView<Individual>()
        val items: ObservableList<Individual> = FXCollections.observableArrayList()
        list.items = items

        root.top = query
        root.center = list

        val service = QuickSearchService(data)
        query.textProperty().addListener { _, _, n ->
            val res = service.findByName(n ?: "")
            items.setAll(res)
        }

        list.setOnMouseClicked {
            val sel = list.selectionModel.selectedItem
            if (sel != null) {
                onChosen?.accept(sel.id)
                stage.close()
            }
        }

        stage.scene = Scene(root, 400.0, 500.0)
        stage.showAndWait()
    }
}
