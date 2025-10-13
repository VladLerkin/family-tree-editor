package com.pedigree.ui;

import com.pedigree.model.Address;
import com.pedigree.model.Repository;
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
 * Manager dialog for GEDCOM Repository records (REPO at level 0).
 * Allows creating, editing, and deleting Repository records stored in ProjectData.
 */
public class RepositoriesManagerDialog {
    private final ProjectRepository.ProjectData data;

    public RepositoriesManagerDialog(ProjectRepository.ProjectData data) {
        this.data = data;
    }

    public void showAndWait() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Repositories");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        TableView<Repository> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Repository, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getName())));

        TableColumn<Repository, String> colCity = new TableColumn<>("City");
        colCity.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getAddress() != null ? nullToEmpty(c.getValue().getAddress().getCity()) : ""));

        TableColumn<Repository, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullToEmpty(c.getValue().getPhone())));

        table.getColumns().addAll(colName, colCity, colPhone);

        ObservableList<Repository> items = FXCollections.observableArrayList(data.repositories);
        items.sort(Comparator.comparing(r -> nullToEmpty(r.getName()).toLowerCase()));
        table.setItems(items);

        Button btnAdd = new Button("Add");
        Button btnEdit = new Button("Edit...");
        Button btnRemove = new Button("Remove");
        Button btnClose = new Button("Close");
        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        btnRemove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(6, btnAdd, btnEdit, btnRemove);
        root.setCenter(table);
        BorderPane.setMargin(table, new Insets(6, 0, 6, 0));
        root.setBottom(new HBox(10, actions, new javafx.scene.layout.Region(), btnClose));
        HBox.setHgrow(((HBox) root.getBottom()).getChildren().get(1), Priority.ALWAYS);

        btnAdd.setOnAction(e -> {
            Repository r = new Repository();
            Optional<Boolean> res = showEditDialog(r);
            if (res.isPresent() && res.get()) {
                data.repositories.add(r);
                items.add(r);
                items.sort(Comparator.comparing(x -> nullToEmpty(x.getName()).toLowerCase()));
                table.getSelectionModel().select(r);
            }
        });

        btnEdit.setOnAction(e -> {
            Repository sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Optional<Boolean> res = showEditDialog(sel);
            if (res.isPresent() && res.get()) {
                table.refresh();
                items.sort(Comparator.comparing(x -> nullToEmpty(x.getName()).toLowerCase()));
            }
        });

        btnRemove.setOnAction(e -> {
            Repository sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete selected repository?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Delete");
            Optional<ButtonType> ans = confirm.showAndWait();
            if (ans.isPresent() && ans.get() == ButtonType.YES) {
                data.repositories.removeIf(r -> r.getId().equals(sel.getId()));
                items.removeIf(r -> r.getId().equals(sel.getId()));
            }
        });

        btnClose.setOnAction(e -> stage.close());

        stage.setScene(new Scene(root, 720, 420));
        stage.showAndWait();
    }

    private Optional<Boolean> showEditDialog(Repository repo) {
        Dialog<Boolean> dlg = new Dialog<>();
        dlg.setTitle("Repository");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField tfName = new TextField(nullToEmpty(repo.getName()));
        TextField tfPhone = new TextField(nullToEmpty(repo.getPhone()));
        TextField tfEmail = new TextField(nullToEmpty(repo.getEmail()));
        TextField tfWebsite = new TextField(nullToEmpty(repo.getWebsite()));

        Address addr = repo.getAddress() != null ? repo.getAddress() : new Address();
        TextField tfAdr1 = new TextField(nullToEmpty(addr.getLine1()));
        TextField tfAdr2 = new TextField(nullToEmpty(addr.getLine2()));
        TextField tfAdr3 = new TextField(nullToEmpty(addr.getLine3()));
        TextField tfCity = new TextField(nullToEmpty(addr.getCity()));
        TextField tfState = new TextField(nullToEmpty(addr.getState()));
        TextField tfPost = new TextField(nullToEmpty(addr.getPostalCode()));
        TextField tfCountry = new TextField(nullToEmpty(addr.getCountry()));

        int row = 0;
        grid.add(new Label("Name:"), 0, row); grid.add(tfName, 1, row++);
        grid.add(new Label("Phone:"), 0, row); grid.add(tfPhone, 1, row++);
        grid.add(new Label("Email:"), 0, row); grid.add(tfEmail, 1, row++);
        grid.add(new Label("Website:"), 0, row); grid.add(tfWebsite, 1, row++);
        grid.add(new Label("Address line 1:"), 0, row); grid.add(tfAdr1, 1, row++);
        grid.add(new Label("Address line 2:"), 0, row); grid.add(tfAdr2, 1, row++);
        grid.add(new Label("Address line 3:"), 0, row); grid.add(tfAdr3, 1, row++);
        grid.add(new Label("City:"), 0, row); grid.add(tfCity, 1, row++);
        grid.add(new Label("State:"), 0, row); grid.add(tfState, 1, row++);
        grid.add(new Label("Postal code:"), 0, row); grid.add(tfPost, 1, row++);
        grid.add(new Label("Country:"), 0, row); grid.add(tfCountry, 1, row++);

        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String name = tfName.getText() != null ? tfName.getText().trim() : "";
            if (name.isBlank()) {
                Dialogs.showError("Validation", "Name is required");
                evt.consume();
            }
        });

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                repo.setName(trimOrNull(tfName.getText()));
                repo.setPhone(trimOrNull(tfPhone.getText()));
                repo.setEmail(trimOrNull(tfEmail.getText()));
                repo.setWebsite(trimOrNull(tfWebsite.getText()));
                Address a = repo.getAddress() != null ? repo.getAddress() : new Address();
                a.setLine1(trimOrNull(tfAdr1.getText()));
                a.setLine2(trimOrNull(tfAdr2.getText()));
                a.setLine3(trimOrNull(tfAdr3.getText()));
                a.setCity(trimOrNull(tfCity.getText()));
                a.setState(trimOrNull(tfState.getText()));
                a.setPostalCode(trimOrNull(tfPost.getText()));
                a.setCountry(trimOrNull(tfCountry.getText()));
                repo.setAddress(a);
                return true;
            }
            return null;
        });

        Optional<Boolean> res = dlg.showAndWait();
        return res;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String trimOrNull(String s) { return s == null ? null : (s.trim().isEmpty() ? null : s.trim()); }
}
