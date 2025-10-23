package com.family.tree.ui

import com.family.tree.model.Gender
import com.family.tree.model.Individual
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.Modality
import javafx.stage.Stage
import java.util.*

class IndividualDialog {

    fun showCreate(): Optional<Individual> {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "New Individual"

        val grid = buildForm(null)
        val scene = Scene(grid, 520.0, 240.0)
        stage.scene = scene

        var result: Individual? = null
        val btnOk = Button("Create")
        val btnCancel = Button("Cancel")
        grid.add(btnOk, 0, 4)
        grid.add(btnCancel, 1, 4)

        btnOk.isDefaultButton = true
        btnCancel.isCancelButton = true

        val tfFirst = grid.lookup("#firstName") as TextField
        val tfLast = grid.lookup("#lastName") as TextField
        @Suppress("UNCHECKED_CAST")
        val cbGender = grid.lookup("#gender") as ComboBox<Gender>

        btnOk.setOnAction {
            val first = tfFirst.text?.trim() ?: ""
            val last = tfLast.text?.trim() ?: ""
            val gender = cbGender.value
            if (first.isBlank() || last.isBlank() || gender == null) {
                showAlert("Please fill First Name, Last Name, and Gender.")
                return@setOnAction
            }
            val ind = Individual(first, last, gender)
            result = ind
            stage.close()
        }
        btnCancel.setOnAction { stage.close() }

        stage.showAndWait()
        return Optional.ofNullable(result)
    }

    fun showEdit(individual: Individual): Boolean {
        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Edit Individual"

        val grid = buildForm(individual)
        val scene = Scene(grid, 520.0, 240.0)
        stage.scene = scene

        var saved = false
        val btnOk = Button("Save")
        val btnCancel = Button("Cancel")
        grid.add(btnOk, 0, 4)
        grid.add(btnCancel, 1, 4)

        btnOk.isDefaultButton = true
        btnCancel.isCancelButton = true

        val tfFirst = grid.lookup("#firstName") as TextField
        val tfLast = grid.lookup("#lastName") as TextField
        @Suppress("UNCHECKED_CAST")
        val cbGender = grid.lookup("#gender") as ComboBox<Gender>

        btnOk.setOnAction {
            val first = tfFirst.text?.trim() ?: ""
            val last = tfLast.text?.trim() ?: ""
            val gender = cbGender.value
            if (first.isBlank() || last.isBlank() || gender == null) {
                showAlert("Please fill First Name, Last Name, and Gender.")
                return@setOnAction
            }
            individual.firstName = first
            individual.lastName = last
            individual.gender = gender
            saved = true
            com.family.tree.util.DirtyFlag.setModified()
            stage.close()
        }
        btnCancel.setOnAction { stage.close() }

        stage.showAndWait()
        return saved
    }

    private fun buildForm(existing: Individual?): GridPane {
        val grid = GridPane()
        grid.hgap = 10.0
        grid.vgap = 10.0
        grid.padding = Insets(12.0)

        val lFirst = Label("First Name:")
        val tfFirst = TextField(existing?.firstName ?: "").apply {
            id = "firstName"
            prefWidth = 300.0
        }

        val lLast = Label("Last Name:")
        val tfLast = TextField(existing?.lastName ?: "").apply {
            id = "lastName"
            prefWidth = 300.0
        }

        val lGender = Label("Gender:")
        val cbGender = ComboBox<Gender>().apply {
            items.addAll(*Gender.values())
            id = "gender"
            if (existing?.gender != null) value = existing.gender
        }

        grid.add(lFirst, 0, 0); grid.add(tfFirst, 1, 0)
        grid.add(lLast, 0, 1); grid.add(tfLast, 1, 1)
        grid.add(lGender, 0, 2); grid.add(cbGender, 1, 2)

        return grid
    }

    private fun showAlert(msg: String) {
        val a = Alert(Alert.AlertType.WARNING, msg, ButtonType.OK)
        a.title = "Validation"
        a.showAndWait()
    }
}
