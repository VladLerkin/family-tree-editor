package com.pedigree.ui;

import com.pedigree.model.Gender;
import com.pedigree.model.Individual;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

public class IndividualDialog {

    public Optional<Individual> showCreate() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("New Individual");

        GridPane grid = buildForm(null);
        Scene scene = new Scene(grid, 420, 300);
        stage.setScene(scene);

        final Individual[] result = new Individual[1];
        Button btnOk = new Button("Create");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 6);
        grid.add(btnCancel, 1, 6);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        TextField tfFirst = (TextField) grid.lookup("#firstName");
        TextField tfLast = (TextField) grid.lookup("#lastName");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cbGender = (ComboBox<Gender>) grid.lookup("#gender");
        TextField tfBirth = (TextField) grid.lookup("#birthDate");
        Button btnBirth = (Button) grid.lookup("#birthPicker");
        TextField tfDeath = (TextField) grid.lookup("#deathDate");
        Button btnDeath = (Button) grid.lookup("#deathPicker");

        DatePhraseDialog dateDlg = new DatePhraseDialog();
        btnBirth.setOnAction(ev -> dateDlg.show(tfBirth.getText()).ifPresent(tfBirth::setText));
        btnDeath.setOnAction(ev -> dateDlg.show(tfDeath.getText()).ifPresent(tfDeath::setText));

        btnOk.setOnAction(e -> {
            String first = tfFirst.getText() != null ? tfFirst.getText().trim() : "";
            String last = tfLast.getText() != null ? tfLast.getText().trim() : "";
            Gender gender = cbGender.getValue();
            if (first.isBlank() || last.isBlank() || gender == null) {
                showAlert("Please fill First Name, Last Name, and Gender.");
                return;
            }
            Individual ind = new Individual(first, last, gender);
            String bd = tfBirth.getText();
            String dd = tfDeath.getText();
            if (bd != null && !bd.isBlank()) ind.setBirthDate(bd.trim());
            if (dd != null && !dd.isBlank()) ind.setDeathDate(dd.trim());
            result[0] = ind;
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
        Scene scene = new Scene(grid, 420, 300);
        stage.setScene(scene);

        final boolean[] saved = new boolean[]{false};
        Button btnOk = new Button("Save");
        Button btnCancel = new Button("Cancel");
        grid.add(btnOk, 0, 6);
        grid.add(btnCancel, 1, 6);

        btnOk.setDefaultButton(true);
        btnCancel.setCancelButton(true);

        TextField tfFirst = (TextField) grid.lookup("#firstName");
        TextField tfLast = (TextField) grid.lookup("#lastName");
        @SuppressWarnings("unchecked")
        ComboBox<Gender> cbGender = (ComboBox<Gender>) grid.lookup("#gender");
        TextField tfBirth = (TextField) grid.lookup("#birthDate");
        Button btnBirth = (Button) grid.lookup("#birthPicker");
        TextField tfDeath = (TextField) grid.lookup("#deathDate");
        Button btnDeath = (Button) grid.lookup("#deathPicker");

        DatePhraseDialog dateDlg = new DatePhraseDialog();
        btnBirth.setOnAction(ev -> dateDlg.show(tfBirth.getText()).ifPresent(tfBirth::setText));
        btnDeath.setOnAction(ev -> dateDlg.show(tfDeath.getText()).ifPresent(tfDeath::setText));

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
            individual.setBirthDate(tfBirth.getText() != null ? tfBirth.getText().trim() : null);
            individual.setDeathDate(tfDeath.getText() != null ? tfDeath.getText().trim() : null);
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

        Label lBirth = new Label("Birth Date:");
        TextField tfBirth = new TextField(existing != null ? nullToEmpty(existing.getBirthDate()) : "");
        tfBirth.setId("birthDate");
        Button btnBirth = new Button("..."); btnBirth.setId("birthPicker");
        HBox hbBirth = new HBox(6, tfBirth, btnBirth);

        Label lDeath = new Label("Death Date:");
        TextField tfDeath = new TextField(existing != null ? nullToEmpty(existing.getDeathDate()) : "");
        tfDeath.setId("deathDate");
        Button btnDeath = new Button("..."); btnDeath.setId("deathPicker");
        HBox hbDeath = new HBox(6, tfDeath, btnDeath);

        grid.add(lFirst, 0, 0); grid.add(tfFirst, 1, 0);
        grid.add(lLast, 0, 1); grid.add(tfLast, 1, 1);
        grid.add(lGender, 0, 2); grid.add(cbGender, 1, 2);
        grid.add(lBirth, 0, 3); grid.add(hbBirth, 1, 3);
        grid.add(lDeath, 0, 4); grid.add(hbDeath, 1, 4);

        return grid;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setTitle("Validation");
        a.showAndWait();
    }
}
