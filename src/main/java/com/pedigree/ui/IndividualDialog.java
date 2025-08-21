package com.pedigree.ui;

import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class IndividualDialog {

    public Optional<Individual> showCreate() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Individual");

        GridPane grid = buildForm(null);
        Scene scene = new Scene(grid, 400, 220);
        stage.setScene(scene);

        final Individual[] result = new Individual[1];
        Button btnOk = new Button("Create");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 4);
        grid.add(btnCancel, 1, 4);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        TextField tfFirst = (TextField) grid.lookup("#firstName");
        TextField tfLast = (TextField) grid.lookup("#lastName");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cbGender = (ComboBox<Gender>) grid.lookup("#gender");

        btnOk.setOnAction(e -> {
            String first = tfFirst.getText() != null ? tfFirst.getText().trim() : "";
            String last = tfLast.getText() != null ? tfLast.getText().trim() : "";
            Gender gender = cbGender.getValue();
            if (first.isBlank() || last.isBlank() || gender == null) {
                showAlert("Please fill First Name, Last Name, and Gender.");
                return;
            }
            result[0] = new Individual(first, last, gender);
            stage.close();
        });
        btnCancel.setOnAction(e -> stage.close());

        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    public boolean showEdit(Individual individual) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit Individual");

        GridPane grid = buildForm(individual);
        Scene scene = new Scene(grid, 400, 220);
        stage.setScene(scene);

        final boolean[] saved = new boolean[]{false};
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 4);
        grid.add(btnCancel, 1, 4);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        TextField tfFirst = (TextField) grid.lookup("#firstName");
        TextField tfLast = (TextField) grid.lookup("#lastName");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cbGender = (ComboBox<Gender>) grid.lookup("#gender");

        btnOk.setOnAction(e -> {
            String first = tfFirst.getText() != null ? tfFirst.getText().trim() : "";
            String last = tfLast.getText() != null ? tfLast.getText().trim() : "";
            Gender gender = cbGender.getValue();
            if (first.isBlank() || last.isBlank() || gender == null) {
                showAlert("Please fill First Name, Last Name, and Gender.");
                return;
            }
            individual.setFirstName(first);
            individual.setLastName(last);
            individual.setGender(gender);
            saved[0] = true;
            stage.close();
        });
        btnCancel.setOnAction(e -> stage.close());

        stage.showAndWait();
        return saved[0];
    }

    private GridPane buildForm(Individual existing) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));

        Label lFirst = new Label("First Name:");
        TextField tfFirst = new TextField(existing != null ? existing.getFirstName() : "");
        tfFirst.setId("firstName");

        Label lLast = new Label("Last Name:");
        TextField tfLast = new TextField(existing != null ? existing.getLastName() : "");
        tfLast.setId("lastName");

        Label lGender = new Label("Gender:");
        ComboBox<Gender> cbGender = new ComboBox<>();
        cbGender.getItems().addAll(Gender.values());
        cbGender.setId("gender");
        if (existing != null && existing.getGender() != null) {
            cbGender.setValue(existing.getGender());
        }

        grid.add(lFirst, 0, 0); grid.add(tfFirst, 1, 0);
        grid.add(lLast, 0, 1); grid.add(tfLast, 1, 1);
        grid.add(lGender, 0, 2); grid.add(cbGender, 1, 2);

        return grid;
    }

    private static void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("Validation");
        a.showAndWait();
    }
}
