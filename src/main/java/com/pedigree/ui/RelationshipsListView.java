package com.pedigree.ui;

import com.pedigree.model.Relationship;
import com.pedigree.storage.ProjectRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import java.util.function.Consumer;

public class RelationshipsListView {
    private final BorderPane root = new BorderPane();
    private final TableView<Relationship> table = new TableView<>();
    private Consumer<Relationship> onSelect;

    public RelationshipsListView() {
        TableColumn<Relationship, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getType() != null ? c.getValue().getType().name() : "")
        );

        TableColumn<Relationship, String> colFrom = new TableColumn<>("From");
        colFrom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFromId()));

        TableColumn<Relationship, String> colTo = new TableColumn<>("To");
        colTo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getToId()));

        table.getColumns().addAll(colType, colFrom, colTo);
        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (onSelect != null && n != null) onSelect.accept(n);
        });
        root.setCenter(table);
    }

    public Node getView() {
        return root;
    }

    public void setOnSelect(Consumer<Relationship> onSelect) {
        this.onSelect = onSelect;
    }

    public void setData(ProjectRepository.ProjectData data) {
        if (data == null || data.relationships == null) {
            table.setItems(FXCollections.observableArrayList());
            return;
        }
        table.setItems(FXCollections.observableArrayList(data.relationships));
    }
}
