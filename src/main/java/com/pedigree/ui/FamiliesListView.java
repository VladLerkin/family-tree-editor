package com.pedigree.ui;

import com.pedigree.model.Family;
import com.pedigree.storage.ProjectRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.application.Platform;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FamiliesListView {
    private final BorderPane root = new BorderPane();
    private final HBox header = new HBox(6);
    private final TextField tagFilter = new TextField();
    private final Button btnAdd = new Button("Add");
    private final Button btnEdit = new Button("Edit");
    private final Button btnDelete = new Button("Delete");
    private final TableView<Family> table = new TableView<>();

    private Consumer<String> onSelect;
    private Runnable onAdd;
    private Consumer<String> onEdit;
    private Consumer<String> onDelete;

    private ProjectRepository.ProjectData data;

    public FamiliesListView() {
        tagFilter.setPromptText("Filter by tag...");
        header.getChildren().addAll(new Label("Tags:"), tagFilter, btnAdd, btnEdit, btnDelete);

        TableColumn<Family, String> colA = new TableColumn<>("Husband");
        colA.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHusbandId()));

        TableColumn<Family, String> colB = new TableColumn<>("Wife");
        colB.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getWifeId()));

        TableColumn<Family, String> colChildren = new TableColumn<>("Children");
        colChildren.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getChildrenIds().size()))
        );

        table.getColumns().addAll(colA, colB, colChildren);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            btnEdit.setDisable(n == null);
            btnDelete.setDisable(n == null);
            if (onSelect != null && n != null) onSelect.accept(n.getId());
        });

        // Double-click to edit selected family
        table.setRowFactory(tv -> {
            TableRow<Family> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Family item = row.getItem();
                    if (item != null && onEdit != null) {
                        onEdit.accept(item.getId());
                    }
                }
            });
            return row;
        });

        btnEdit.setDisable(true);
        btnDelete.setDisable(true);

        tagFilter.textProperty().addListener((obs, o, n) -> refreshItems());

        btnAdd.setOnAction(e -> { if (onAdd != null) onAdd.run(); });
        btnEdit.setOnAction(e -> {
            Family sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onEdit != null) onEdit.accept(sel.getId());
        });
        btnDelete.setOnAction(e -> {
            Family sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onDelete != null) onDelete.accept(sel.getId());
        });

        // Subscribe to selection events from canvas and select corresponding family
        SelectionBus.addListener(id -> {
            if (id == null || data == null) return;
            boolean isFamily = data.families.stream().anyMatch(f -> id.equals(f.getId()));
            if (!isFamily) return;
            Platform.runLater(() -> {
                for (Family row : table.getItems()) {
                    if (row != null && id.equals(row.getId())) {
                        table.getSelectionModel().select(row);
                        table.scrollTo(row);
                        break;
                    }
                }
            });
        });

        root.setTop(header);
        root.setCenter(table);
    }

    public Node getView() {
        return root;
    }

    public void setOnSelect(Consumer<String> onSelect) { this.onSelect = onSelect; }
    public void setOnAdd(Runnable onAdd) { this.onAdd = onAdd; }
    public void setOnEdit(Consumer<String> onEdit) { this.onEdit = onEdit; }
    public void setOnDelete(Consumer<String> onDelete) { this.onDelete = onDelete; }

    public void setData(ProjectRepository.ProjectData data) {
        this.data = data;
        refreshItems();
    }

    private void refreshItems() {
        if (data == null || data.families == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }
        String filter = tagFilter.getText();
        ObservableList<Family> items;
        if (filter == null || filter.isBlank()) {
            items = FXCollections.observableArrayList(data.families);
        } else {
            String f = filter.toLowerCase(Locale.ROOT);
            items = FXCollections.observableArrayList(
                    data.families.stream()
                            .filter(fam -> fam.getTags().stream().anyMatch(t -> t.getName() != null && t.getName().toLowerCase(Locale.ROOT).contains(f)))
                            .collect(Collectors.toList())
            );
        }
        table.setItems(items);
    }
}
