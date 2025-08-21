package com.pedigree.ui;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class FamilyDialog {

    public Optional<Family> showCreate(ProjectRepository.ProjectData data) {
        if (data == null) return Optional.empty();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Family");

        GridPane grid = buildForm(null, data);
        Scene scene = new Scene(new VBox(grid), 600, 420);
        stage.setScene(scene);

        final Family[] result = new Family[1];
        Button btnOk = new Button("Create");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 4);
        grid.add(btnCancel, 1, 4);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbA = (ComboBox<Individual>) grid.lookup("#husband");
        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbB = (ComboBox<Individual>) grid.lookup("#wife");
        @SuppressWarnings("unchecked")
        ListView<Individual> lvChildrenSelected = (ListView<Individual>) grid.lookup("#childrenSelected");

        btnOk.setOnAction(e -> {
            Family fam = new Family();
            Individual a = cbA.getValue();
            Individual b = cbB.getValue();
            fam.setHusbandId(a != null ? a.getId() : null);
            fam.setWifeId(b != null ? b.getId() : null);
            lvChildrenSelected.getItems()
                    .forEach(child -> fam.getChildrenIds().add(child.getId()));
            result[0] = fam;
            com.pedigree.util.DirtyFlag.setModified();
            stage.close();
        });
        btnCancel.setOnAction(e -> stage.close());

        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    public boolean showEdit(Family fam, ProjectRepository.ProjectData data) {
        if (fam == null || data == null) return false;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Family");

        GridPane grid = buildForm(fam, data);
        Scene scene = new Scene(new VBox(grid), 600, 420);
        stage.setScene(scene);

        final boolean[] saved = new boolean[]{false};
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 4);
        grid.add(btnCancel, 1, 4);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbA = (ComboBox<Individual>) grid.lookup("#husband");
        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbB = (ComboBox<Individual>) grid.lookup("#wife");
        @SuppressWarnings("unchecked")
        ListView<Individual> lvChildrenSelected = (ListView<Individual>) grid.lookup("#childrenSelected");

        btnOk.setOnAction(e -> {
            Individual a = cbA.getValue();
            Individual b = cbB.getValue();
            fam.setHusbandId(a != null ? a.getId() : null);
            fam.setWifeId(b != null ? b.getId() : null);
            fam.getChildrenIds().clear();
            lvChildrenSelected.getItems()
                    .forEach(child -> fam.getChildrenIds().add(child.getId()));
            saved[0] = true;
            com.pedigree.util.DirtyFlag.setModified();
            stage.close();
        });
        btnCancel.setOnAction(e -> stage.close());

        stage.showAndWait();
        return saved[0];
    }

    private GridPane buildForm(Family existing, ProjectRepository.ProjectData data) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        Label lA = new Label("Husband:");
        ComboBox<Individual> cbA = new ComboBox<>();
        cbA.setId("husband");
        cbA.getItems().addAll(data.individuals);
        cbA.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Individual item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });
        cbA.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Individual item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });

        Label lB = new Label("Wife:");
        ComboBox<Individual> cbB = new ComboBox<>();
        cbB.setId("wife");
        cbB.getItems().addAll(data.individuals);
        cbB.setCellFactory(cbA.getCellFactory());
        cbB.setButtonCell(cbA.getButtonCell());

        Label lChildren = new Label("Children:");
        // Dual-list UI: Available individuals -> Selected children
        ListView<Individual> lvChildrenAvailable = new ListView<>();
        lvChildrenAvailable.setId("childrenAvailable");
        lvChildrenAvailable.getItems().addAll(data.individuals);
        lvChildrenAvailable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvChildrenAvailable.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Individual item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });

        ListView<Individual> lvChildrenSelected = new ListView<>();
        lvChildrenSelected.setId("childrenSelected");
        lvChildrenSelected.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        lvChildrenSelected.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Individual item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });

        Button btnAddChild = new Button("Add ▶");
        Button btnRemoveChild = new Button("◀ Remove");
        btnAddChild.setOnAction(e -> {
            var selected = new java.util.ArrayList<>(lvChildrenAvailable.getSelectionModel().getSelectedItems());
            for (Individual ind : selected) {
                if (!lvChildrenSelected.getItems().contains(ind)) {
                    lvChildrenSelected.getItems().add(ind);
                }
            }
            lvChildrenAvailable.getSelectionModel().clearSelection();
        });
        btnRemoveChild.setOnAction(e -> {
            var selected = new java.util.ArrayList<>(lvChildrenSelected.getSelectionModel().getSelectedItems());
            lvChildrenSelected.getItems().removeAll(selected);
        });

        // Double-click convenience
        lvChildrenAvailable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) btnAddChild.fire();
        });
        lvChildrenSelected.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) btnRemoveChild.fire();
        });

        // Initialize existing values and keep spouses out of children
        if (existing != null) {
            if (existing.getHusbandId() != null) {
                data.individuals.stream().filter(i -> i.getId().equals(existing.getHusbandId())).findFirst().ifPresent(cbA::setValue);
            }
            if (existing.getWifeId() != null) {
                data.individuals.stream().filter(i -> i.getId().equals(existing.getWifeId())).findFirst().ifPresent(cbB::setValue);
            }
            for (String cid : existing.getChildrenIds()) {
                data.individuals.stream().filter(i -> i.getId().equals(cid)).findFirst().ifPresent(i -> {
                    if (!lvChildrenSelected.getItems().contains(i)) {
                        lvChildrenSelected.getItems().add(i);
                    }
                });
            }
        }

        // Ensure spouses are not listed as available children and are removed if selected
        Runnable refreshAvailable = () -> {
            lvChildrenAvailable.getItems().setAll(data.individuals);
            Individual a = cbA.getValue();
            Individual b = cbB.getValue();
            if (a != null) lvChildrenAvailable.getItems().remove(a);
            if (b != null) lvChildrenAvailable.getItems().remove(b);
            // Also make sure spouses are not in selected
            if (a != null) lvChildrenSelected.getItems().remove(a);
            if (b != null) lvChildrenSelected.getItems().remove(b);
            // And avoid duplicates between lists
            lvChildrenAvailable.getItems().removeAll(lvChildrenSelected.getItems());
        };

        cbA.valueProperty().addListener((obs, oldV, newV) -> refreshAvailable.run());
        cbB.valueProperty().addListener((obs, oldV, newV) -> refreshAvailable.run());
        refreshAvailable.run();

        // Layout: Children area with two lists and add/remove buttons in between
        VBox buttonsBox = new VBox(8, btnAddChild, btnRemoveChild);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);
        HBox childrenBox = new HBox(10, lvChildrenAvailable, buttonsBox, lvChildrenSelected);
        HBox.setHgrow(lvChildrenAvailable, Priority.ALWAYS);
        HBox.setHgrow(lvChildrenSelected, Priority.ALWAYS);
        lvChildrenAvailable.setPrefHeight(200);
        lvChildrenSelected.setPrefHeight(200);

        grid.add(lA, 0, 0); grid.add(cbA, 1, 0);
        grid.add(lB, 0, 1); grid.add(cbB, 1, 1);
        grid.add(lChildren, 0, 2); grid.add(childrenBox, 1, 2);

        return grid;
    }

    private static String displayName(Individual i) {
        String f = i.getFirstName() != null ? i.getFirstName() : "";
        String l = i.getLastName() != null ? i.getLastName() : "";
        return (f + " " + l).trim();
    }
}
