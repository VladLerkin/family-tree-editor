package com.family.tree.ui

import com.family.tree.model.Repository
import com.family.tree.model.Source
import com.family.tree.storage.ProjectRepository
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.stage.Modality
import javafx.stage.Stage
import java.util.*

class SourcesManagerDialog(private val data: ProjectRepository.ProjectData) {

    fun showAndWait() {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Sources"

        val root = BorderPane()
        root.padding = Insets(10.0)

        val table = TableView<Source>()
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS

        val colTitle = TableColumn<Source, String>("Title")
        colTitle.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.title)) }

        val colAbbr = TableColumn<Source, String>("Abbr")
        colAbbr.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.abbreviation)) }

        val colAgency = TableColumn<Source, String>("Agency")
        colAgency.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.agency)) }

        val colCallNo = TableColumn<Source, String>("Call #")
        colCallNo.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.callNumber)) }

        table.columns.addAll(colTitle, colAbbr, colAgency, colCallNo)

        val items = FXCollections.observableArrayList(data.sources)
        items.sortWith(Comparator.comparing { s: Source -> nullToEmpty(s.title).lowercase() })
        table.items = items

        val btnAdd = Button("Add")
        val btnEdit = Button("Edit...")
        val btnRemove = Button("Remove")
        val btnClose = Button("Close")
        btnEdit.disableProperty().bind(table.selectionModel.selectedItemProperty().isNull)
        btnRemove.disableProperty().bind(table.selectionModel.selectedItemProperty().isNull)

        val actions = HBox(6.0, btnAdd, btnEdit, btnRemove)
        root.center = table
        BorderPane.setMargin(table, Insets(6.0, 0.0, 6.0, 0.0))
        val bottom = HBox(10.0, actions, javafx.scene.layout.Region(), btnClose)
        root.bottom = bottom
        HBox.setHgrow(bottom.children[1], Priority.ALWAYS)

        btnAdd.setOnAction {
            val s = Source()
            val res = showEditDialog(s)
            if (res.isPresent && res.get()) {
                data.sources.add(s)
                items.add(s)
                items.sortWith(Comparator.comparing { x: Source -> nullToEmpty(x.title).lowercase() })
                table.selectionModel.select(s)
            }
        }

        btnEdit.setOnAction {
            val sel = table.selectionModel.selectedItem ?: return@setOnAction
            val res = showEditDialog(sel)
            if (res.isPresent && res.get()) {
                table.refresh()
                items.sortWith(Comparator.comparing { x: Source -> nullToEmpty(x.title).lowercase() })
            }
        }

        btnRemove.setOnAction {
            val sel = table.selectionModel.selectedItem ?: return@setOnAction
            val confirm = Alert(Alert.AlertType.CONFIRMATION, "Delete selected source?", ButtonType.YES, ButtonType.NO)
            confirm.title = "Confirm Delete"
            val ans = confirm.showAndWait()
            if (ans.isPresent && ans.get() == ButtonType.YES) {
                data.sources.removeIf { s -> s.id == sel.id }
                items.removeIf { s -> s.id == sel.id }
            }
        }

        btnClose.setOnAction { stage.close() }

        stage.scene = Scene(root, 720.0, 420.0)
        stage.showAndWait()
    }

    private fun showEditDialog(source: Source): Optional<Boolean> {
        val dlg = Dialog<Boolean>()
        dlg.title = "Source"
        dlg.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 8.0
        grid.vgap = 8.0
        grid.padding = Insets(10.0)

        val tfTitle = TextField(nullToEmpty(source.title))
        val tfAbbr = TextField(nullToEmpty(source.abbreviation))
        val tfAgency = TextField(nullToEmpty(source.agency))
        val tfCallNo = TextField(nullToEmpty(source.callNumber))
        val tfText = TextArea(nullToEmpty(source.text))
        tfText.prefRowCount = 6

        val repoCombo = ComboBox<Repository>()
        repoCombo.items.add(null)
        repoCombo.items.addAll(data.repositories)
        repoCombo.converter = object : javafx.util.StringConverter<Repository?>() {
            override fun toString(r: Repository?): String = r?.name ?: "(none)"
            override fun fromString(string: String?): Repository? = null
        }
        if (source.repositoryId != null) {
            val match = data.repositories.firstOrNull { it.id == source.repositoryId }
            if (match != null) repoCombo.value = match else repoCombo.value = null
        } else repoCombo.value = null

        var row = 0
        grid.add(Label("Title:"), 0, row); grid.add(tfTitle, 1, row++)
        grid.add(Label("Abbreviation:"), 0, row); grid.add(tfAbbr, 1, row++)
        grid.add(Label("Agency:"), 0, row); grid.add(tfAgency, 1, row++)
        grid.add(Label("Call number:"), 0, row); grid.add(tfCallNo, 1, row++)
        grid.add(Label("Repository:"), 0, row); grid.add(repoCombo, 1, row++)
        grid.add(Label("Text:"), 0, row); grid.add(tfText, 1, row++)

        dlg.dialogPane.content = grid

        val okBtn = dlg.dialogPane.lookupButton(ButtonType.OK) as Button
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION) { evt ->
            val t = tfTitle.text?.trim() ?: ""
            if (t.isBlank()) {
                Dialogs.showError("Validation", "Title is required")
                evt.consume()
            }
        }

        dlg.setResultConverter { bt ->
            if (bt == ButtonType.OK) {
                source.title = tfTitle.text?.trim()
                source.abbreviation = tfAbbr.text?.trim()
                source.agency = tfAgency.text?.trim()
                source.callNumber = tfCallNo.text?.trim()
                source.text = tfText.text?.trim()
                source.repositoryId = repoCombo.value?.id
                true
            } else null
        }

        return dlg.showAndWait()
    }

    private fun nullToEmpty(s: String?): String = s ?: ""
}
