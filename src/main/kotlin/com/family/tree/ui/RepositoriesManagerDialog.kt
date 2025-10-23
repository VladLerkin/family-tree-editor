package com.family.tree.ui

import com.family.tree.model.Address
import com.family.tree.model.Repository
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

class RepositoriesManagerDialog(private val data: ProjectRepository.ProjectData) {

    fun showAndWait() {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Repositories"

        val root = BorderPane()
        root.padding = Insets(10.0)

        val table = TableView<Repository>()
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS

        val colName = TableColumn<Repository, String>("Name")
        colName.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.name)) }

        val colCity = TableColumn<Repository, String>("City")
        colCity.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(c.value.address?.city?.let { nullToEmpty(it) } ?: "") }

        val colPhone = TableColumn<Repository, String>("Phone")
        colPhone.setCellValueFactory { c -> javafx.beans.property.SimpleStringProperty(nullToEmpty(c.value.phone)) }

        table.columns.addAll(colName, colCity, colPhone)

        val items = FXCollections.observableArrayList(data.repositories)
        items.sortWith(Comparator.comparing { r: Repository -> nullToEmpty(r.name).lowercase() })
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
            val r = Repository()
            val res = showEditDialog(r)
            if (res.isPresent && res.get()) {
                data.repositories.add(r)
                items.add(r)
                items.sortWith(Comparator.comparing { x: Repository -> nullToEmpty(x.name).lowercase() })
                table.selectionModel.select(r)
            }
        }

        btnEdit.setOnAction {
            val sel = table.selectionModel.selectedItem ?: return@setOnAction
            val res = showEditDialog(sel)
            if (res.isPresent && res.get()) {
                table.refresh()
                items.sortWith(Comparator.comparing { x: Repository -> nullToEmpty(x.name).lowercase() })
            }
        }

        btnRemove.setOnAction {
            val sel = table.selectionModel.selectedItem ?: return@setOnAction
            val confirm = Alert(Alert.AlertType.CONFIRMATION, "Delete selected repository?", ButtonType.YES, ButtonType.NO)
            confirm.title = "Confirm Delete"
            val ans = confirm.showAndWait()
            if (ans.isPresent && ans.get() == ButtonType.YES) {
                data.repositories.removeIf { r -> r.id == sel.id }
                items.removeIf { r -> r.id == sel.id }
            }
        }

        btnClose.setOnAction { stage.close() }

        stage.scene = Scene(root, 720.0, 420.0)
        stage.showAndWait()
    }

    private fun showEditDialog(repo: Repository): Optional<Boolean> {
        val dlg = Dialog<Boolean>()
        dlg.title = "Repository"
        dlg.dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane()
        grid.hgap = 8.0
        grid.vgap = 8.0
        grid.padding = Insets(10.0)

        val tfName = TextField(nullToEmpty(repo.name))
        val tfPhone = TextField(nullToEmpty(repo.phone))
        val tfEmail = TextField(nullToEmpty(repo.email))
        val tfWebsite = TextField(nullToEmpty(repo.website))

        val addr = repo.address ?: Address()
        val tfAdr1 = TextField(nullToEmpty(addr.line1))
        val tfAdr2 = TextField(nullToEmpty(addr.line2))
        val tfAdr3 = TextField(nullToEmpty(addr.line3))
        val tfCity = TextField(nullToEmpty(addr.city))
        val tfState = TextField(nullToEmpty(addr.state))
        val tfPost = TextField(nullToEmpty(addr.postalCode))
        val tfCountry = TextField(nullToEmpty(addr.country))

        var row = 0
        grid.add(Label("Name:"), 0, row); grid.add(tfName, 1, row++);
        grid.add(Label("Phone:"), 0, row); grid.add(tfPhone, 1, row++);
        grid.add(Label("Email:"), 0, row); grid.add(tfEmail, 1, row++);
        grid.add(Label("Website:"), 0, row); grid.add(tfWebsite, 1, row++);
        grid.add(Label("Address line 1:"), 0, row); grid.add(tfAdr1, 1, row++);
        grid.add(Label("Address line 2:"), 0, row); grid.add(tfAdr2, 1, row++);
        grid.add(Label("Address line 3:"), 0, row); grid.add(tfAdr3, 1, row++);
        grid.add(Label("City:"), 0, row); grid.add(tfCity, 1, row++);
        grid.add(Label("State:"), 0, row); grid.add(tfState, 1, row++);
        grid.add(Label("Postal code:"), 0, row); grid.add(tfPost, 1, row++);
        grid.add(Label("Country:"), 0, row); grid.add(tfCountry, 1, row++);

        dlg.dialogPane.content = grid

        val okBtn = dlg.dialogPane.lookupButton(ButtonType.OK) as Button
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION) { evt ->
            val name = tfName.text?.trim() ?: ""
            if (name.isBlank()) {
                Dialogs.showError("Validation", "Name is required")
                evt.consume()
            }
        }

        dlg.setResultConverter { bt ->
            if (bt == ButtonType.OK) {
                repo.name = trimOrNull(tfName.text)
                repo.phone = trimOrNull(tfPhone.text)
                repo.email = trimOrNull(tfEmail.text)
                repo.website = trimOrNull(tfWebsite.text)
                val a = repo.address ?: Address()
                a.line1 = trimOrNull(tfAdr1.text)
                a.line2 = trimOrNull(tfAdr2.text)
                a.line3 = trimOrNull(tfAdr3.text)
                a.city = trimOrNull(tfCity.text)
                a.state = trimOrNull(tfState.text)
                a.postalCode = trimOrNull(tfPost.text)
                a.country = trimOrNull(tfCountry.text)
                repo.address = a
                true
            } else null
        }

        val res = dlg.showAndWait()
        return res
    }

    private fun nullToEmpty(s: String?): String = s ?: ""
    private fun trimOrNull(s: String?): String? = s?.trim()?.let { if (it.isEmpty()) null else it }
}
