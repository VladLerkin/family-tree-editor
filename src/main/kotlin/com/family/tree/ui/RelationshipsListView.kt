package com.family.tree.ui

import com.family.tree.model.Relationship
import com.family.tree.storage.ProjectRepository
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.BorderPane
import java.util.function.Consumer

class RelationshipsListView {
    private val root = BorderPane()
    private val table = TableView<Relationship>()
    private var onSelect: Consumer<Relationship>? = null

    init {
        val colType = TableColumn<Relationship, String>("Type")
        colType.setCellValueFactory { c ->
            SimpleStringProperty(c.value.type?.name ?: "")
        }

        val colFrom = TableColumn<Relationship, String>("From")
        colFrom.setCellValueFactory { c -> SimpleStringProperty(c.value.fromId) }

        val colTo = TableColumn<Relationship, String>("To")
        colTo.setCellValueFactory { c -> SimpleStringProperty(c.value.toId) }

        table.columns.addAll(colType, colFrom, colTo)
        table.selectionModel.selectedItemProperty().addListener { _, _, n ->
            if (n != null) onSelect?.accept(n)
        }
        root.center = table
    }

    fun getView(): Node = root

    fun setOnSelect(onSelect: Consumer<Relationship>?) {
        this.onSelect = onSelect
    }

    fun setData(data: ProjectRepository.ProjectData?) {
        if (data == null || data.relationships == null) {
            table.items = FXCollections.observableArrayList()
            return
        }
        table.items = FXCollections.observableArrayList(data.relationships)
    }
}
