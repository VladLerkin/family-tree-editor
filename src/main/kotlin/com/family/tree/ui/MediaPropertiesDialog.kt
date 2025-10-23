package com.family.tree.ui

import com.family.tree.model.MediaAttachment
import com.family.tree.services.MediaManager
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * Kotlin port of MediaPropertiesDialog.
 */
class MediaPropertiesDialog(
    private val projectFilePath: Path,
    private val attachment: MediaAttachment
) : Dialog<Boolean>() {

    private val nameField = TextField()
    private val pathField = TextField()
    private val absPathLabel = Label()
    private val browseBtn = Button("Обзор...")

    init {
        Objects.requireNonNull(attachment, "attachment")
        title = "Свойства медиа"
        headerText = "Просмотр и редактирование свойств медиа"

        dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)

        val grid = GridPane().apply {
            hgap = 8.0
            vgap = 8.0
            padding = Insets(10.0)
        }

        nameField.text = attachment.fileName
        pathField.text = attachment.relativePath
        pathField.promptText = "относительный путь в контейнере медиа"
        pathField.prefColumnCount = 30

        updateAbsPathLabel()

        browseBtn.setOnAction { onBrowse() }

        var r = 0
        grid.add(Label("Название:"), 0, r)
        grid.add(nameField, 1, r++)
        grid.add(Label("Путь файла:"), 0, r)
        val pathBox = HBox(6.0, pathField, browseBtn)
        grid.add(pathBox, 1, r++)
        grid.add(Label("Абс. путь:"), 0, r)
        grid.add(absPathLabel, 1, r)

        dialogPane.content = grid

        // Validate inputs
        val okBtn = dialogPane.lookupButton(ButtonType.OK) as Button
        okBtn.disableProperty().bind(nameField.textProperty().isEmpty.or(pathField.textProperty().isEmpty))

        setResultConverter { btn ->
            if (btn == ButtonType.OK) {
                applyChanges()
                true
            } else false
        }

        nameField.textProperty().addListener { _, _, _ -> updateAbsPathLabel() }
        pathField.textProperty().addListener { _, _, _ -> updateAbsPathLabel() }
    }

    private fun updateAbsPathLabel() {
        try {
            var rel = pathField.text
            if (MediaManager.isExternalLink(rel)) {
                absPathLabel.text = "Внешняя ссылка — будет открыта в браузере"
                return
            }
            val root = MediaManager.getMediaRoot(projectFilePath)
            if (rel.isNullOrBlank()) {
                rel = nameField.text
            }
            if (rel.isNullOrBlank()) {
                absPathLabel.text = "-"
                return
            }
            val abs = root.resolve(rel)
            val exists = if (Files.exists(abs)) " (существует)" else " (не найден)"
            absPathLabel.text = abs.toString() + exists
        } catch (e: IOException) {
            absPathLabel.text = "Ошибка: ${e.message}"
        }
    }

    private fun onBrowse() {
        val fc = FileChooser()
        fc.title = "Выберите файл медиа"
        fc.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif"),
            FileChooser.ExtensionFilter("Все файлы", "*.*")
        )
        val f = fc.showOpenDialog(owner) ?: return
        val chosen = f.toPath()
        try {
            val mediaRoot = MediaManager.getMediaRoot(projectFilePath)
            val abs = chosen.toAbsolutePath().normalize()
            if (abs.startsWith(mediaRoot.toAbsolutePath().normalize())) {
                // Inside media container: use relative path
                val rel = mediaRoot.relativize(abs)
                pathField.text = rel.toString().replace('\\', '/')
            } else {
                // Outside: copy into project and use the new relative path
                val tmp = MediaManager.copyIntoProject(projectFilePath, abs)
                if (nameField.text.isNullOrBlank()) {
                    nameField.text = tmp.fileName
                }
                pathField.text = tmp.relativePath
            }
        } catch (ex: IOException) {
            Dialogs.showError("Выбор файла", ex.message)
        }
    }

    private fun applyChanges() {
        val newName = nameField.text
        val newRel = pathField.text
        if (attachment.fileName != newName) {
            attachment.fileName = newName
        }
        if (attachment.relativePath != newRel) {
            attachment.relativePath = newRel
        }
    }
}
