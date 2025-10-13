package com.pedigree.ui;

import com.pedigree.model.Repository;
import com.pedigree.model.Source;
import com.pedigree.storage.ProjectRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.Optional;

/**
 * Simple manager dialog for GEDCOM Source records (SOUR at level 0).
 * Allows creating, editing, and deleting Source records stored in ProjectData.
 */
public class SourcesManagerDialog {
    private final ProjectRepository.ProjectData data;

    public SourcesManagerDialog(ProjectRepository.ProjectData data) {
        this.data = data;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Sources");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        TableView<Source> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Source, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getTitle())));

        TableColumn<Source, String> colAbbr = new TableColumn<>("Abbr");
        colAbbr.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getAbbreviation())));

        TableColumn<Source, String> colAgency = new TableColumn<>("Agency");
        colAgency.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getAgency())));

        TableColumn<Source, String> colCallNo = new TableColumn<>("Call #");
        colCallNo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getCallNumber())));

        table.getColumns().addAll(colTitle, colAbbr, colAgency, colCallNo);

        ObservableList<Source> items = FXCollections.observableArrayList(data.sources);
        // Sort by title for nicer view
        items.sort(Comparator.comparing(s -> nullToEmpty(s.getTitle()).toLowerCase()));
        table.setItems(items);

        // Buttons
        Button btnAdd = new Button("Add");
        Button btnEdit = new Button("Edit...");
        Button btnRemove = new Button("Remove");
        Button btnClose = new Button("Close");
        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnRemove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(6, btnAdd, btnEdit, btnRemove);
        HBox bottom = new HBox();
        bottom.setSpacing(6);
        bottom.getChildren().addAll(actions);

        root.setCenter(table);
        BorderPane.setMargin(table, new Insets(6, 0, 6, 0));
        root.setBottom(new HBox(10, actions, new javafx.scene.layout.Region(), btnClose));
        HBox.setHgrow(((HBox) root.getBottom()).getChildren().get(1), Priority.ALWAYS);

        btnAdd.setOnAction(e -> {
            Source s = new Source();
            Optional<Boolean> res = showEditDialog(s);
            if (res.isPresent() && res.get()) {
                data.sources.add(s);
                items.add(s);
                items.sort(Comparator.comparing(x -> nullToEmpty(x.getTitle()).toLowerCase()));
                table.getSelectionModel().select(s);
            }
        });

        btnEdit.setOnAction(e -> {
            Source sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String oldRepo = sel.getRepositoryId();
            Optional<Boolean> res = showEditDialog(sel);
            if (res.isPresent() && res.get()) {
                // Already updated via setters
                table.refresh();
                items.sort(Comparator.comparing(x -> nullToEmpty(x.getTitle()).toLowerCase()));
                // No extra
            } else {
                // Restore anything if needed (not necessary, setters already changed only on save)
            }
        });

        btnRemove.setOnAction(e -> {
            Source sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected source?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Delete");
            Optional<ButtonType> ans = confirm.showAndWait();
            if (ans.isPresent() && ans.get() == ButtonType.YES) {
                data.sources.removeIf(s -> s.getId().equals(sel.getId()));
                items.removeIf(s -> s.getId().equals(sel.getId()));
            }
        });

        btnClose.setOnAction(e -> stage.close());

        stage.setScene(new Scene(root, 720, 420));
        stage.showAndWait();
    }

    private Optional<Boolean> showEditDialog(Source source) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Source");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField tfTitle = new TextField(nullToEmpty(source.getTitle()));
        TextField tfAbbr = new TextField(nullToEmpty(source.getAbbreviation()));
        TextField tfAgency = new TextField(nullToEmpty(source.getAgency()));
        TextField tfCall = new TextField(nullToEmpty(source.getCallNumber()));

        ComboBox<Repository> cbRepo = new ComboBox<>();
        cbRepo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Repository r) { return r == null ? "(None)" : nullToEmpty(r.getName()); }
            @Override public Repository fromString(String s) { return null; }
        });
        cbRepo.getItems().add(null); // None option
        cbRepo.getItems().addAll(data.repositories);
        if (source.getRepositoryId() == null) cbRepo.setValue(null);
        else {
            for (Repository r : data.repositories) {
                if (source.getRepositoryId().equals(r.getId())) { cbRepo.setValue(r); break; }
            }
        }
        // Manage repositories button
        Button btnManageRepos = new Button("...");
        btnManageRepos.setTooltip(new Tooltip("Manage Repositories…"));
        btnManageRepos.setOnAction(ev -> {
            try {
                new RepositoriesManagerDialog(data).showAndWait();
                // Refresh repository list items and preserve selection
                String selectedRepoId = cbRepo.getValue() != null ? cbRepo.getValue().getId() : null;
                cbRepo.getItems().clear();
                cbRepo.getItems().add(null);
                cbRepo.getItems().addAll(data.repositories);
                if (selectedRepoId != null) {
                    for (Repository r : data.repositories) {
                        if (selectedRepoId.equals(r.getId())) { cbRepo.setValue(r); break; }
                    }
                } else {
                    cbRepo.setValue(null);
                }
            } catch (Throwable ex) {
                Dialogs.showError("Repositories", ex.getMessage());
            }
        });
        HBox repoBox = new HBox(6, cbRepo, btnManageRepos);

        int row = 0;
        grid.add(new Label("Title:"), 0, row); grid.add(tfTitle, 1, row++);
        grid.add(new Label("Abbreviation:"), 0, row); grid.add(tfAbbr, 1, row++);
        grid.add(new Label("Agency:"), 0, row); grid.add(tfAgency, 1, row++);
        grid.add(new Label("Call Number:"), 0, row); grid.add(tfCall, 1, row++);
        grid.add(new Label("Repository:"), 0, row); grid.add(repoBox, 1, row++);

        dlg.getDialogPane().setContent(grid);

        // Validate title not empty
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String title = tfTitle.getText() != null ? tfTitle.getText().trim() : "";
            if (title.isBlank()) {
                Dialogs.showError("Validation", "Title is required");
                evt.consume();
            }
        });

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                source.setTitle(tfTitle.getText() != null ? tfTitle.getText().trim() : null);
                source.setAbbreviation(tfAbbr.getText() != null ? tfAbbr.getText().trim() : null);
                source.setAgency(tfAgency.getText() != null ? tfAgency.getText().trim() : null);
                source.setCallNumber(tfCall.getText() != null ? tfCall.getText().trim() : null);
                Repository r = cbRepo.getValue();
                source.setRepositoryId(r != null ? r.getId() : null);
                return true;
            }
            return null;
        });

        Optional<Boolean> res = dlg.showAndWait();
        return res;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
