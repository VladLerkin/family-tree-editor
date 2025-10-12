package com.pedigree.ui;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.model.Gender;
import com.pedigree.storage.ProjectRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
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
        VBox root = new VBox(grid);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 640, 520);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(580);
        stage.setMinHeight(460);

        final Family[] result = new Family[1];
        Button btnOk = new Button("Create");
        Button btnCancel = new Button("Cancel");
        HBox buttons = new HBox(10, btnOk, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setColumnSpan(buttons, 2);
        grid.add(buttons, 0, 5);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbA = (ComboBox<Individual>) grid.lookup("#husband");
        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbB = (ComboBox<Individual>) grid.lookup("#wife");
        TextField tfMarriageDate = (TextField) grid.lookup("#marriageDate");
        Button btnMarriageDate = (Button) grid.lookup("#marriageDatePicker");
        TextField tfMarriagePlace = (TextField) grid.lookup("#marriagePlace");
        @SuppressWarnings("unchecked")
        ListView<Individual> lvChildrenSelected = (ListView<Individual>) grid.lookup("#childrenSelected");

        DatePhraseDialog dateDlg = new DatePhraseDialog();
        btnMarriageDate.setOnAction(ev -> dateDlg.show(tfMarriageDate.getText()).ifPresent(tfMarriageDate::setText));

        btnOk.setOnAction(e -> {
            Family fam = new Family();
            Individual a = cbA.getValue();
            Individual b = cbB.getValue();
            fam.setHusbandId(a != null ? a.getId() : null);
            fam.setWifeId(b != null ? b.getId() : null);
            fam.setMarriageDate(tfMarriageDate.getText() != null ? tfMarriageDate.getText().trim() : null);
            fam.setMarriagePlace(tfMarriagePlace.getText() != null ? tfMarriagePlace.getText().trim() : null);
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
        VBox root = new VBox(grid);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 640, 520);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(580);
        stage.setMinHeight(460);

        final boolean[] saved = new boolean[]{false};
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        HBox buttons = new HBox(10, btnOk, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        GridPane.setColumnSpan(buttons, 2);
        grid.add(buttons, 0, 5);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbA = (ComboBox<Individual>) grid.lookup("#husband");
        @SuppressWarnings("unchecked")
        ComboBox<Individual> cbB = (ComboBox<Individual>) grid.lookup("#wife");
        TextField tfMarriageDate = (TextField) grid.lookup("#marriageDate");
        Button btnMarriageDate = (Button) grid.lookup("#marriageDatePicker");
        TextField tfMarriagePlace = (TextField) grid.lookup("#marriagePlace");
        @SuppressWarnings("unchecked")
        ListView<Individual> lvChildrenSelected = (ListView<Individual>) grid.lookup("#childrenSelected");

        DatePhraseDialog dateDlg = new DatePhraseDialog();
        btnMarriageDate.setOnAction(ev -> dateDlg.show(tfMarriageDate.getText()).ifPresent(tfMarriageDate::setText));

        btnOk.setOnAction(e -> {
            Individual a = cbA.getValue();
            Individual b = cbB.getValue();
            fam.setHusbandId(a != null ? a.getId() : null);
            fam.setWifeId(b != null ? b.getId() : null);
            fam.setMarriageDate(tfMarriageDate.getText() != null ? tfMarriageDate.getText().trim() : null);
            fam.setMarriagePlace(tfMarriagePlace.getText() != null ? tfMarriagePlace.getText().trim() : null);
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
        // Column 0 = labels (no grow), Column 1 = controls/content (grow)
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setHgrow(Priority.NEVER);
        col0.setFillWidth(false);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        Label lA = new Label("Husband:");
        lA.setMinWidth(Region.USE_PREF_SIZE);
        ComboBox<Individual> cbA = new ComboBox<>();
        cbA.setId("husband");
        // Only males can be selected as husband
                for (Individual i : data.individuals) {
                    if (i.getGender() == Gender.MALE) {
                        cbA.getItems().add(i);
                    }
                }
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
        // Also set a converter to ensure value is always displayed even if not in items
        cbA.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Individual object) {
                return object == null ? "" : displayName(object);
            }
            @Override public Individual fromString(String string) { return null; }
        });

        Label lB = new Label("Wife:");
        lB.setMinWidth(Region.USE_PREF_SIZE);
        ComboBox<Individual> cbB = new ComboBox<>();
        cbB.setId("wife");
        // Only females can be selected as wife
                for (Individual i : data.individuals) {
                    if (i.getGender() == Gender.FEMALE) {
                        cbB.getItems().add(i);
                    }
                }
        cbB.setCellFactory(cbA.getCellFactory());
        cbB.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Individual item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : displayName(item));
            }
        });
        cbB.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Individual object) {
                return object == null ? "" : displayName(object);
            }
            @Override public Individual fromString(String string) { return null; }
        });

        Label lMarriageDate = new Label("Marriage Date:");
        lMarriageDate.setMinWidth(Region.USE_PREF_SIZE);
        TextField tfMarriageDate = new TextField(existing != null ? nullToEmpty(existing.getMarriageDate()) : "");
        tfMarriageDate.setId("marriageDate");
        tfMarriageDate.setPrefWidth(300);
        Button btnMarriageDate = new Button("...");
        btnMarriageDate.setId("marriageDatePicker");
        HBox hbMarriageDate = new HBox(6, tfMarriageDate, btnMarriageDate);

        Label lMarriagePlace = new Label("Marriage Place:");
        lMarriagePlace.setMinWidth(Region.USE_PREF_SIZE);
        TextField tfMarriagePlace = new TextField(existing != null ? nullToEmpty(existing.getMarriagePlace()) : "");
        tfMarriagePlace.setId("marriagePlace");
        tfMarriagePlace.setPrefWidth(300);

        Label lChildren = new Label("Children:");
        lChildren.setMinWidth(Region.USE_PREF_SIZE);
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
                data.individuals.stream()
                        .filter(i -> i.getId().equals(existing.getHusbandId()))
                        .findFirst()
                        .ifPresent(h -> {
                            // Ensure the selected husband is present in the items even if filtered out (e.g., UNKNOWN gender)
                            if (!cbA.getItems().contains(h)) {
                                cbA.getItems().add(0, h);
                            }
                            cbA.setValue(h);
                        });
            }
            if (existing.getWifeId() != null) {
                data.individuals.stream()
                        .filter(i -> i.getId().equals(existing.getWifeId()))
                        .findFirst()
                        .ifPresent(w -> {
                            if (!cbB.getItems().contains(w)) {
                                cbB.getItems().add(0, w);
                            }
                            cbB.setValue(w);
                        });
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
        // Let the controls expand horizontally in column 1
        cbA.setMaxWidth(Double.MAX_VALUE);
        cbB.setMaxWidth(Double.MAX_VALUE);

        grid.add(lA, 0, 0); grid.add(cbA, 1, 0);
        grid.add(lB, 0, 1); grid.add(cbB, 1, 1);
        grid.add(lMarriageDate, 0, 2); grid.add(hbMarriageDate, 1, 2);
        grid.add(lMarriagePlace, 0, 3); grid.add(tfMarriagePlace, 1, 3);
        grid.add(lChildren, 0, 4); grid.add(childrenBox, 1, 4);

        return grid;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String displayName(Individual i) {
        String f = i.getFirstName() != null ? i.getFirstName() : "";
        String l = i.getLastName() != null ? i.getLastName() : "";
        return (f + " " + l).trim();
    }
}
