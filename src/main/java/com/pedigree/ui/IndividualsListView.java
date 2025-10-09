package com.pedigree.ui;

import com.pedigree.model.Individual;
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

public class IndividualsListView {
    private final BorderPane root = new BorderPane();
    private final HBox header = new HBox(6);
    private final TextField tagFilter = new TextField();
    private final Button btnAdd = new Button("+");
    private final Button btnEdit = new Button("Edit");
    private final Button btnDelete = new Button("-");
    private final TableView<Individual> table = new TableView<>();

    private Consumer<String> onSelect;
    private Runnable onAdd;
    private Consumer<String> onEdit;
    private Consumer<String> onDelete;

    private ProjectRepository.ProjectData data;

    public IndividualsListView() {
        tagFilter.setPromptText("Filter by tag...");
        header.getChildren().addAll(new Label("Tags:"), tagFilter, btnAdd, btnEdit, btnDelete);

        TableColumn<Individual, String> colFirst = new TableColumn<>("First Name");
        colFirst.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFirstName()));

        TableColumn<Individual, String> colLast = new TableColumn<>("Last Name");
        colLast.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLastName()));

        TableColumn<Individual, String> colGender = new TableColumn<>("Gender");
        colGender.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getGender() != null ? c.getValue().getGender().name() : "")
        );

        table.getColumns().addAll(colFirst, colLast, colGender);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            btnEdit.setDisable(n == null);
            btnDelete.setDisable(n == null);
            if (onSelect != null && n != null) onSelect.accept(n.getId());
        });

        // Double-click to edit selected person
        table.setRowFactory(tv -> {
            TableRow<Individual> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Individual item = row.getItem();
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
            Individual sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onEdit != null) onEdit.accept(sel.getId());
        });
        btnDelete.setOnAction(e -> {
            Individual sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onDelete != null) onDelete.accept(sel.getId());
        });

        // Subscribe to selection events from canvas and select corresponding person
        SelectionBus.addListener(id -> {
            if (id == null || data == null) return;
            boolean isPerson = data.individuals.stream().anyMatch(i -> id.equals(i.getId()));
            if (!isPerson) return;
            Platform.runLater(() -> {
                for (Individual row : table.getItems()) {
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
        if (data == null || data.individuals == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }
        String filter = tagFilter.getText();
        ObservableList<Individual> items;
        if (filter == null || filter.isBlank()) {
            items = FXCollections.observableArrayList(data.individuals);
        } else {
            String f = filter.toLowerCase(Locale.ROOT);
            items = FXCollections.observableArrayList(
                    data.individuals.stream()
                            .filter(i -> {
                                // Match by tag
                                boolean tagMatch = i.getTags().stream().anyMatch(t -> t.getName() != null && t.getName().toLowerCase(Locale.ROOT).contains(f));
                                // Match by first name
                                boolean firstNameMatch = i.getFirstName() != null && i.getFirstName().toLowerCase(Locale.ROOT).contains(f);
                                // Match by last name
                                boolean lastNameMatch = i.getLastName() != null && i.getLastName().toLowerCase(Locale.ROOT).contains(f);
                                return tagMatch || firstNameMatch || lastNameMatch;
                            })
                            .collect(Collectors.toList())
            );
        }
        table.setItems(items);
    }
}
