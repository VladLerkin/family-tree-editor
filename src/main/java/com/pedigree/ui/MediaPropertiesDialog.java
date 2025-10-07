package com.pedigree.ui;

import com.pedigree.model.MediaAttachment;
import com.pedigree.services.MediaManager;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Dialog window to view and edit media properties (name and file path).
 * Labels are provided in Russian per the issue description.
 */
public class MediaPropertiesDialog extends Dialog<Boolean> {
    private final TextField nameField = new TextField();
    private final TextField pathField = new TextField();
    private final Label absPathLabel = new Label();
    private final Button browseBtn = new Button("Обзор...");

    private final MediaAttachment attachment;
    private final Path projectFilePath;

    public MediaPropertiesDialog(Path projectFilePath, MediaAttachment attachment) {
        this.projectFilePath = projectFilePath;
        this.attachment = Objects.requireNonNull(attachment, "attachment");
        setTitle("Свойства медиа");
        setHeaderText("Просмотр и редактирование свойств медиа");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        nameField.setText(attachment.getFileName());
        pathField.setText(attachment.getRelativePath());
        pathField.setPromptText("относительный путь в контейнере медиа");
        pathField.setPrefColumnCount(30);

        updateAbsPathLabel();

        browseBtn.setOnAction(e -> onBrowse());

        int r = 0;
        grid.add(new Label("Название:"), 0, r);
        grid.add(nameField, 1, r++);
        grid.add(new Label("Путь файла:"), 0, r);
        HBox pathBox = new HBox(6, pathField, browseBtn);
        grid.add(pathBox, 1, r++);
        grid.add(new Label("Абс. путь:"), 0, r);
        grid.add(absPathLabel, 1, r);

        getDialogPane().setContent(grid);

        // Validate inputs: name not blank, path not blank
        Button okBtn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okBtn.disableProperty().bind(nameField.textProperty().isEmpty().or(pathField.textProperty().isEmpty()));

        setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                applyChanges();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        });

        // Update absolute path preview when fields change
        nameField.textProperty().addListener((o, a, b) -> updateAbsPathLabel());
        pathField.textProperty().addListener((o, a, b) -> updateAbsPathLabel());
    }

    private void updateAbsPathLabel() {
        try {
            String rel = pathField.getText();
            if (MediaManager.isExternalLink(rel)) {
                absPathLabel.setText("Внешняя ссылка — будет открыта в браузере");
                return;
            }
            Path root = MediaManager.getMediaRoot(projectFilePath);
            if (rel == null || rel.isBlank()) {
                rel = nameField.getText();
            }
            if (rel == null || rel.isBlank()) {
                absPathLabel.setText("-");
                return;
            }
            Path abs = root.resolve(rel);
            String exists = Files.exists(abs) ? " (существует)" : " (не найден)";
            absPathLabel.setText(abs.toString() + exists);
        } catch (IOException e) {
            absPathLabel.setText("Ошибка: " + e.getMessage());
        }
    }

    private void onBrowse() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Выберите файл медиа");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        File f = fc.showOpenDialog(getOwner());
        if (f == null) return;
        Path chosen = f.toPath();
        try {
            Path mediaRoot = MediaManager.getMediaRoot(projectFilePath);
            Path abs = chosen.toAbsolutePath().normalize();
            if (abs.startsWith(mediaRoot.toAbsolutePath().normalize())) {
                // Inside media container: use relative path
                Path rel = mediaRoot.relativize(abs);
                pathField.setText(rel.toString().replace('\\', '/'));
            } else {
                // Outside: copy into project and use the new relative path
                MediaAttachment tmp = MediaManager.copyIntoProject(projectFilePath, abs);
                // We only take the relative path and keep current name unless it's empty
                if (nameField.getText() == null || nameField.getText().isBlank()) {
                    nameField.setText(tmp.getFileName());
                }
                pathField.setText(tmp.getRelativePath());
            }
        } catch (IOException ex) {
            Dialogs.showError("Выбор файла", ex.getMessage());
        }
    }

    private void applyChanges() {
        String newName = nameField.getText();
        String newRel = pathField.getText();
        if (!Objects.equals(attachment.getFileName(), newName)) {
            attachment.setFileName(newName);
        }
        if (!Objects.equals(attachment.getRelativePath(), newRel)) {
            attachment.setRelativePath(newRel);
        }
    }
}
