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
    private final Button btnAdd = new Button("+");
    private final Button btnEdit = new Button("Edit");
    private final Button btnDelete = new Button("-");
    private final TableView<Family> table = new TableView<>();

    private Consumer<String> onSelect;
    private Runnable onAdd;
    private Consumer<String> onEdit;
    private Consumer<String> onDelete;

    private ProjectRepository.ProjectData data;

    // Guard to prevent feedback loop and flicker when selection is driven externally (from canvas)
    private boolean suppressOnSelect = false;

    // Track last programmatically selected family id to avoid redundant re-selections
    private String lastProgrammaticSelectionId = null;

    public FamiliesListView() {
        tagFilter.setPromptText("Filter by tag...");
        header.getChildren().addAll(new Label("Tags:"), tagFilter, btnAdd, btnEdit, btnDelete);

        TableColumn<Family, String> colA = new TableColumn<>("Husband");
        colA.setCellValueFactory(c -> new SimpleStringProperty(formatSpouse(c.getValue().getHusbandId())));

        TableColumn<Family, String> colB = new TableColumn<>("Wife");
        colB.setCellValueFactory(c -> new SimpleStringProperty(formatSpouse(c.getValue().getWifeId())));

        TableColumn<Family, String> colChildren = new TableColumn<>("Children");
        colChildren.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getChildrenIds().size()))
        );

        table.getColumns().addAll(colA, colB, colChildren);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            btnEdit.setDisable(n == null);
            btnDelete.setDisable(n == null);
            if (suppressOnSelect) return; // avoid feedback when we select programmatically
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

            // Determine target family to select based on published id (family or individual)
            String familyIdToSelect = null;
            // If a family id is published, select it directly
            if (data.families.stream().anyMatch(f -> id.equals(f.getId()))) {
                familyIdToSelect = id;
            } else {
                // Otherwise, map individual id to its family (husband/wife/child)
                for (Family f : data.families) {
                    if (id.equals(f.getHusbandId()) || id.equals(f.getWifeId()) || (f.getChildrenIds() != null && f.getChildrenIds().contains(id))) {
                        familyIdToSelect = f.getId();
                        break;
                    }
                }
            }

            if (familyIdToSelect == null) return;

            final String toSelect = familyIdToSelect;
            Platform.runLater(() -> {
                // If already selected and equals the last programmatic selection, skip to avoid flicker
                Family currentlySelected = table.getSelectionModel().getSelectedItem();
                if (currentlySelected != null && toSelect.equals(currentlySelected.getId())) {
                    lastProgrammaticSelectionId = toSelect;
                    return;
                }
                for (Family row : table.getItems()) {
                    if (row != null && toSelect.equals(row.getId())) {
                        // Suppress outgoing onSelect while we update selection from the bus
                        suppressOnSelect = true;
                        try {
                            table.getSelectionModel().select(row);
                            table.scrollTo(row);
                            lastProgrammaticSelectionId = toSelect;
                        } finally {
                            suppressOnSelect = false;
                        }
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

    private String formatSpouse(String individualId) {
        if (individualId == null || individualId.isBlank() || data == null || data.individuals == null) {
            return "";
        }
        com.pedigree.model.Individual person = null;
        for (com.pedigree.model.Individual i : data.individuals) {
            if (individualId.equals(i.getId())) { person = i; break; }
        }
        if (person == null) {
            return individualId; // fallback to id if not found
        }
        String last = person.getLastName();
        String first = person.getFirstName();
        StringBuilder sb = new StringBuilder();
        if (last != null && !last.isBlank()) sb.append(last.trim());
        if (first != null && !first.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(first.trim().charAt(0))).append('.');
        }
        String result = sb.toString();
        return result.isBlank() ? individualId : result;
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
                            .filter(fam -> {
                                // Match by tag
                                boolean tagMatch = fam.getTags().stream().anyMatch(t -> t.getName() != null && t.getName().toLowerCase(Locale.ROOT).contains(f));
                                
                                // Match by husband's name
                                boolean husbandMatch = false;
                                if (fam.getHusbandId() != null && data.individuals != null) {
                                    for (com.pedigree.model.Individual ind : data.individuals) {
                                        if (fam.getHusbandId().equals(ind.getId())) {
                                            husbandMatch = (ind.getFirstName() != null && ind.getFirstName().toLowerCase(Locale.ROOT).contains(f)) ||
                                                          (ind.getLastName() != null && ind.getLastName().toLowerCase(Locale.ROOT).contains(f));
                                            break;
                                        }
                                    }
                                }
                                
                                // Match by wife's name
                                boolean wifeMatch = false;
                                if (fam.getWifeId() != null && data.individuals != null) {
                                    for (com.pedigree.model.Individual ind : data.individuals) {
                                        if (fam.getWifeId().equals(ind.getId())) {
                                            wifeMatch = (ind.getFirstName() != null && ind.getFirstName().toLowerCase(Locale.ROOT).contains(f)) ||
                                                       (ind.getLastName() != null && ind.getLastName().toLowerCase(Locale.ROOT).contains(f));
                                            break;
                                        }
                                    }
                                }
                                
                                return tagMatch || husbandMatch || wifeMatch;
                            })
                            .collect(Collectors.toList())
            );
        }
        table.setItems(items);
    }
}
