package com.family.tree.ui

import com.family.tree.model.Individual
import com.family.tree.storage.ProjectRepository
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import java.util.Locale
import java.util.function.Consumer
import java.util.stream.Collectors

class IndividualsListView {
    private val root = BorderPane()
    private val header = HBox(6.0)
    private val tagFilter = TextField()
    private val btnAdd = Button("+")
    private val btnEdit = Button("Edit")
    private val btnDelete = Button("-")
    private val table = TableView<Individual>()

    private var onSelect: Consumer<String>? = null
    private var onAdd: Runnable? = null
    private var onEdit: Consumer<String>? = null
    private var onDelete: Consumer<String>? = null

    private var data: ProjectRepository.ProjectData? = null

    init {
        tagFilter.promptText = "Filter by tag..."
        header.children.addAll(Label("Tags:"), tagFilter, btnAdd, btnEdit, btnDelete)

        val colFirst = TableColumn<Individual, String>("First Name")
        colFirst.setCellValueFactory { c -> SimpleStringProperty(c.value.firstName) }

        val colLast = TableColumn<Individual, String>("Last Name")
        colLast.setCellValueFactory { c -> SimpleStringProperty(c.value.lastName) }

        val colGender = TableColumn<Individual, String>("Gender")
        colGender.setCellValueFactory { c -> SimpleStringProperty(c.value.gender?.name ?: "") }

        table.columns.addAll(colFirst, colLast, colGender)
        table.selectionModel.selectedItemProperty().addListener { _, _, n ->
            btnEdit.isDisable = n == null
            btnDelete.isDisable = n == null
            if (onSelect != null && n != null) onSelect!!.accept(n.id)
        }

        // Double-click to edit selected person
        table.setRowFactory { tv ->
            val row = TableRow<Individual>()
            row.setOnMouseClicked { event ->
                if (!row.isEmpty && event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                    val item = row.item
                    if (item != null && onEdit != null) {
                        onEdit!!.accept(item.id)
                    }
                }
            }
            row
        }

        btnEdit.isDisable = true
        btnDelete.isDisable = true

        tagFilter.textProperty().addListener { _, _, _ -> refreshItems() }

        btnAdd.setOnAction { onAdd?.run() }
        btnEdit.setOnAction {
            val sel = table.selectionModel.selectedItem
            if (sel != null && onEdit != null) onEdit!!.accept(sel.id)
        }
        btnDelete.setOnAction {
            val sel = table.selectionModel.selectedItem
            if (sel != null && onDelete != null) onDelete!!.accept(sel.id)
        }

        // Subscribe to selection events from canvas and select corresponding person
        SelectionBus.addListener(Consumer { id ->
            if (id == null || data == null) return@Consumer
            val isPerson = data!!.individuals.stream().anyMatch { i -> id == i.id }
            if (!isPerson) return@Consumer
            Platform.runLater {
                for (row in table.items) {
                    if (row != null && id == row.id) {
                        table.selectionModel.select(row)
                        table.scrollTo(row)
                        break
                    }
                }
            }
        })

        root.top = header
        root.center = table
    }

    fun getView(): Node = root

    fun setOnSelect(onSelect: Consumer<String>?) { this.onSelect = onSelect }
    fun setOnAdd(onAdd: Runnable?) { this.onAdd = onAdd }
    fun setOnEdit(onEdit: Consumer<String>?) { this.onEdit = onEdit }
    fun setOnDelete(onDelete: Consumer<String>?) { this.onDelete = onDelete }

    fun setData(data: ProjectRepository.ProjectData) {
        this.data = data
        refreshItems()
    }

    private fun refreshItems() {
        if (data == null || data!!.individuals == null) {
            table.items = FXCollections.observableArrayList()
            return
        }
        val filter = tagFilter.text
        val items: ObservableList<Individual> = if (filter == null || filter.isBlank()) {
            FXCollections.observableArrayList(data!!.individuals)
        } else {
            val f = filter.lowercase(Locale.ROOT)
            FXCollections.observableArrayList(
                data!!.individuals.stream()
                    .filter { i: Individual ->
                        val tagMatch = i.tags.stream().anyMatch { t -> t.name?.lowercase(Locale.ROOT)?.contains(f) == true }
                        val firstNameMatch = i.firstName?.lowercase(Locale.ROOT)?.contains(f) == true
                        val lastNameMatch = i.lastName?.lowercase(Locale.ROOT)?.contains(f) == true
                        tagMatch || firstNameMatch || lastNameMatch
                    }
                    .collect(Collectors.toList())
            )
        }
        table.items = items
    }
}
