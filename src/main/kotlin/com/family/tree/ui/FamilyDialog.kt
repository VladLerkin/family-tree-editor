package com.family.tree.ui

import com.family.tree.model.Family
import com.family.tree.model.Gender
import com.family.tree.model.Individual
import com.family.tree.storage.ProjectRepository
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.stage.Modality
import javafx.stage.Stage
import java.util.*

class FamilyDialog {

    fun showCreate(data: ProjectRepository.ProjectData?): Optional<Family> {
        if (data == null) return Optional.empty()
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "New Family"

        val grid = buildForm(null, data)
        val root = VBox(grid).apply { padding = Insets(8.0) }
        val scene = Scene(root, 640.0, 520.0)
        stage.scene = scene
        stage.isResizable = true
        stage.minWidth = 580.0
        stage.minHeight = 460.0

        var result: Family? = null
        val btnOk = Button("Create")
        val btnCancel = Button("Cancel")
        val buttons = HBox(10.0, btnOk, btnCancel).apply { alignment = Pos.CENTER_RIGHT }
        GridPane.setColumnSpan(buttons, 2)
        grid.add(buttons, 0, 5)

        btnOk.isDefaultButton = true
        btnCancel.isCancelButton = true

        @Suppress("UNCHECKED_CAST")
        val cbA = grid.lookup("#husband") as ComboBox<Individual>
        @Suppress("UNCHECKED_CAST")
        val cbB = grid.lookup("#wife") as ComboBox<Individual>
        @Suppress("UNCHECKED_CAST")
        val lvChildrenSelected = grid.lookup("#childrenSelected") as ListView<Individual>

        btnOk.setOnAction {
            val fam = Family()
            val a = cbA.value
            val b = cbB.value
            fam.husbandId = a?.id
            fam.wifeId = b?.id
            lvChildrenSelected.items.forEach { child -> fam.childrenIds.add(child.id) }
            result = fam
            com.family.tree.util.DirtyFlag.setModified()
            stage.close()
        }
        btnCancel.setOnAction { stage.close() }

        stage.showAndWait()
        return Optional.ofNullable(result)
    }

    fun showEdit(fam: Family?, data: ProjectRepository.ProjectData?): Boolean {
        if (fam == null || data == null) return false
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Edit Family"

        val grid = buildForm(fam, data)
        val root = VBox(grid).apply { padding = Insets(8.0) }
        val scene = Scene(root, 640.0, 480.0)
        stage.scene = scene
        stage.isResizable = true
        stage.minWidth = 580.0
        stage.minHeight = 420.0

        var saved = false
        val btnOk = Button("Save")
        val btnCancel = Button("Cancel")
        val buttons = HBox(10.0, btnOk, btnCancel).apply { alignment = Pos.CENTER_RIGHT }
        GridPane.setColumnSpan(buttons, 2)
        grid.add(buttons, 0, 3)

        btnOk.isDefaultButton = true
        btnCancel.isCancelButton = true

        @Suppress("UNCHECKED_CAST")
        val cbA = grid.lookup("#husband") as ComboBox<Individual>
        @Suppress("UNCHECKED_CAST")
        val cbB = grid.lookup("#wife") as ComboBox<Individual>
        @Suppress("UNCHECKED_CAST")
        val lvChildrenSelected = grid.lookup("#childrenSelected") as ListView<Individual>

        btnOk.setOnAction {
            val a = cbA.value
            val b = cbB.value
            fam.husbandId = a?.id
            fam.wifeId = b?.id
            fam.childrenIds.clear()
            lvChildrenSelected.items.forEach { child -> fam.childrenIds.add(child.id) }
            saved = true
            com.family.tree.util.DirtyFlag.setModified()
            stage.close()
        }
        btnCancel.setOnAction { stage.close() }

        stage.showAndWait()
        return saved
    }

    private fun buildForm(existing: Family?, data: ProjectRepository.ProjectData): GridPane {
        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(12.0)
        val col0 = ColumnConstraints().apply {
            hgrow = Priority.NEVER
            isFillWidth = false
        }
        val col1 = ColumnConstraints().apply { hgrow = Priority.ALWAYS }
        grid.columnConstraints.addAll(col0, col1)

        val lA = Label("Husband:").apply { minWidth = Region.USE_PREF_SIZE }
        val cbA = ComboBox<Individual>().apply { id = "husband" }
        // Only males can be selected as husband
        data.individuals.forEach { i -> if (i.gender == Gender.MALE) cbA.items.add(i) }
        cbA.setCellFactory {
            object : ListCell<Individual>() {
                override fun updateItem(item: Individual?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else displayName(item)
                }
            }
        }
        cbA.buttonCell = object : ListCell<Individual>() {
            override fun updateItem(item: Individual?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) null else displayName(item)
            }
        }
        cbA.converter = object : javafx.util.StringConverter<Individual>() {
            override fun toString(obj: Individual?): String = obj?.let { displayName(it) } ?: ""
            override fun fromString(string: String?): Individual? = null
        }

        val lB = Label("Wife:").apply { minWidth = Region.USE_PREF_SIZE }
        val cbB = ComboBox<Individual>().apply { id = "wife" }
        // Only females can be selected as wife
        data.individuals.forEach { i -> if (i.gender == Gender.FEMALE) cbB.items.add(i) }
        cbB.setCellFactory(cbA.cellFactory)
        cbB.buttonCell = object : ListCell<Individual>() {
            override fun updateItem(item: Individual?, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (empty || item == null) null else displayName(item)
            }
        }
        cbB.converter = object : javafx.util.StringConverter<Individual>() {
            override fun toString(obj: Individual?): String = obj?.let { displayName(it) } ?: ""
            override fun fromString(string: String?): Individual? = null
        }

        val lChildren = Label("Kids:").apply { minWidth = Region.USE_PREF_SIZE }
        val lvChildrenAvailable = ListView<Individual>().apply {
            id = "childrenAvailable"
            items.addAll(data.individuals)
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setCellFactory {
                object : ListCell<Individual>() {
                    override fun updateItem(item: Individual?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else displayName(item)
                    }
                }
            }
        }
        val lvChildrenSelected = ListView<Individual>().apply {
            id = "childrenSelected"
            selectionModel.selectionMode = SelectionMode.MULTIPLE
            setCellFactory {
                object : ListCell<Individual>() {
                    override fun updateItem(item: Individual?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty || item == null) null else displayName(item)
                    }
                }
            }
        }

        val btnAddChild = Button("Add ▶")
        val btnRemoveChild = Button("◀ Remove")
        btnAddChild.setOnAction {
            val selected = ArrayList(lvChildrenAvailable.selectionModel.selectedItems)
            for (ind in selected) {
                if (!lvChildrenSelected.items.contains(ind)) {
                    lvChildrenSelected.items.add(ind)
                }
            }
            lvChildrenAvailable.selectionModel.clearSelection()
        }
        btnRemoveChild.setOnAction {
            val selected = ArrayList(lvChildrenSelected.selectionModel.selectedItems)
            lvChildrenSelected.items.removeAll(selected)
        }

        lvChildrenAvailable.setOnMouseClicked { e -> if (e.clickCount == 2) btnAddChild.fire() }
        lvChildrenSelected.setOnMouseClicked { e -> if (e.clickCount == 2) btnRemoveChild.fire() }

        if (existing != null) {
            existing.husbandId?.let { hid ->
                data.individuals.firstOrNull { it.id == hid }?.let { h ->
                    if (!cbA.items.contains(h)) cbA.items.add(0, h)
                    cbA.value = h
                }
            }
            existing.wifeId?.let { wid ->
                data.individuals.firstOrNull { it.id == wid }?.let { w ->
                    if (!cbB.items.contains(w)) cbB.items.add(0, w)
                    cbB.value = w
                }
            }
            for (cid in existing.childrenIds) {
                data.individuals.firstOrNull { it.id == cid }?.let { i ->
                    if (!lvChildrenSelected.items.contains(i)) lvChildrenSelected.items.add(i)
                }
            }
        }

        val refreshAvailable = {
            lvChildrenAvailable.items.setAll(data.individuals)
            val a = cbA.value
            val b = cbB.value
            if (a != null) lvChildrenAvailable.items.remove(a)
            if (b != null) lvChildrenAvailable.items.remove(b)
            if (a != null) lvChildrenSelected.items.remove(a)
            if (b != null) lvChildrenSelected.items.remove(b)
            lvChildrenAvailable.items.removeAll(lvChildrenSelected.items)
        }

        cbA.valueProperty().addListener { _, _, _ -> refreshAvailable() }
        cbB.valueProperty().addListener { _, _, _ -> refreshAvailable() }
        refreshAvailable()

        val buttonsBox = VBox(8.0, btnAddChild, btnRemoveChild).apply { alignment = Pos.CENTER_LEFT }
        val childrenBox = HBox(10.0, lvChildrenAvailable, buttonsBox, lvChildrenSelected)
        HBox.setHgrow(lvChildrenAvailable, Priority.ALWAYS)
        HBox.setHgrow(lvChildrenSelected, Priority.ALWAYS)
        lvChildrenAvailable.prefHeight = 200.0
        lvChildrenSelected.prefHeight = 200.0
        cbA.maxWidth = Double.MAX_VALUE
        cbB.maxWidth = Double.MAX_VALUE

        grid.add(lA, 0, 0); grid.add(cbA, 1, 0)
        grid.add(lB, 0, 1); grid.add(cbB, 1, 1)
        grid.add(lChildren, 0, 2); grid.add(childrenBox, 1, 2)

        return grid
    }

    private fun displayName(i: Individual): String {
        val fn = i.firstName ?: ""
        val ln = i.lastName ?: ""
        val name = "$fn $ln".trim()
        return if (name.isBlank()) i.id else name
    }
}
