package com.family.tree.ui

import com.family.tree.model.Family
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

class FamiliesListView {
    private val root = BorderPane()
    private val header = HBox(6.0)
    private val tagFilter = TextField()
    private val btnAdd = Button("+")
    private val btnEdit = Button("Edit")
    private val btnDelete = Button("-")
    private val table = TableView<Family>()

    private var onSelect: Consumer<String>? = null
    private var onAdd: Runnable? = null
    private var onEdit: Consumer<String>? = null
    private var onDelete: Consumer<String>? = null

    private var data: ProjectRepository.ProjectData? = null

    // Guard to prevent feedback loop and flicker when selection is driven externally (from canvas)
    private var suppressOnSelect = false

    // Track last programmatically selected family id to avoid redundant re-selections
    private var lastProgrammaticSelectionId: String? = null

    init {
        tagFilter.promptText = "Filter by tag..."
        header.children.addAll(Label("Tags:"), tagFilter, btnAdd, btnEdit, btnDelete)

        val colA = TableColumn<Family, String>("Husband")
        colA.setCellValueFactory { c -> SimpleStringProperty(formatSpouse(c.value.husbandId)) }

        val colB = TableColumn<Family, String>("Wife")
        colB.setCellValueFactory { c -> SimpleStringProperty(formatSpouse(c.value.wifeId)) }

        val colChildren = TableColumn<Family, String>("Children")
        colChildren.setCellValueFactory { c -> SimpleStringProperty(c.value.childrenIds.size.toString()) }

        table.columns.addAll(colA, colB, colChildren)
        table.selectionModel.selectedItemProperty().addListener { _, _, n ->
            btnEdit.isDisable = n == null
            btnDelete.isDisable = n == null
            if (suppressOnSelect) return@addListener // avoid feedback when we select programmatically
            if (onSelect != null && n != null) onSelect!!.accept(n.id)
        }

        // Double-click to edit selected family
        table.setRowFactory { tv ->
            val row = TableRow<Family>()
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

        // Subscribe to selection events from canvas and select corresponding family
        SelectionBus.addListener(Consumer { id ->
            if (id == null || data == null) return@Consumer

            // Determine target family to select based on published id (family or individual)
            var familyIdToSelect: String? = null
            // If a family id is published, select it directly
            if (data!!.families.stream().anyMatch { f -> id == f.id }) {
                familyIdToSelect = id
            } else {
                // Otherwise, map individual id to its family (husband/wife/child)
                for (f in data!!.families) {
                    if (id == f.husbandId || id == f.wifeId || (f.childrenIds != null && f.childrenIds.contains(id))) {
                        familyIdToSelect = f.id
                        break
                    }
                }
            }

            if (familyIdToSelect == null) return@Consumer

            val toSelect = familyIdToSelect
            Platform.runLater {
                // If already selected and equals the last programmatic selection, skip to avoid flicker
                val currentlySelected = table.selectionModel.selectedItem
                if (currentlySelected != null && toSelect == currentlySelected.id) {
                    lastProgrammaticSelectionId = toSelect
                    return@runLater
                }
                for (row in table.items) {
                    if (row != null && toSelect == row.id) {
                        // Suppress outgoing onSelect while we update selection from the bus
                        suppressOnSelect = true
                        try {
                            table.selectionModel.select(row)
                            table.scrollTo(row)
                            lastProgrammaticSelectionId = toSelect
                        } finally {
                            suppressOnSelect = false
                        }
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

    private fun formatSpouse(individualId: String?): String {
        if (individualId.isNullOrBlank() || data == null || data!!.individuals == null) {
            return ""
        }
        var person: com.family.tree.model.Individual? = null
        for (i in data!!.individuals) {
            if (individualId == i.id) { person = i; break }
        }
        if (person == null) {
            return individualId // fallback to id if not found
        }
        val last = person.lastName
        val first = person.firstName
        val sb = StringBuilder()
        if (!last.isNullOrBlank()) sb.append(last.trim())
        if (!first.isNullOrBlank()) {
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(first.trim()[0].uppercaseChar()).append('.')
        }
        val result = sb.toString()
        return if (result.isBlank()) individualId else result
    }

    private fun refreshItems() {
        if (data == null || data!!.families == null) {
            table.items = FXCollections.observableArrayList()
            return
        }
        val filter = tagFilter.text
        val items: ObservableList<Family> = if (filter == null || filter.isBlank()) {
            FXCollections.observableArrayList(data!!.families)
        } else {
            val f = filter.lowercase(Locale.ROOT)
            FXCollections.observableArrayList(
                data!!.families.stream()
                    .filter { fam: Family ->
                        // Match by tag
                        val tagMatch = fam.tags.stream().anyMatch { t -> t.name?.lowercase(Locale.ROOT)?.contains(f) == true }

                        // Match by husband's name
                        var husbandMatch = false
                        if (fam.husbandId != null && data!!.individuals != null) {
                            for (ind in data!!.individuals) {
                                if (fam.husbandId == ind.id) {
                                    husbandMatch = (ind.firstName?.lowercase(Locale.ROOT)?.contains(f) == true) ||
                                            (ind.lastName?.lowercase(Locale.ROOT)?.contains(f) == true)
                                    break
                                }
                            }
                        }

                        // Match by wife's name
                        var wifeMatch = false
                        if (fam.wifeId != null && data!!.individuals != null) {
                            for (ind in data!!.individuals) {
                                if (fam.wifeId == ind.id) {
                                    wifeMatch = (ind.firstName?.lowercase(Locale.ROOT)?.contains(f) == true) ||
                                            (ind.lastName?.lowercase(Locale.ROOT)?.contains(f) == true)
                                    break
                                }
                            }
                        }

                        tagMatch || husbandMatch || wifeMatch
                    }
                    .collect(Collectors.toList())
            )
        }
        table.items = items
    }
}
