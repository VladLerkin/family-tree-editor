package com.pedigree.ui;

import com.pedigree.model.Individual;
import com.pedigree.search.QuickSearchService;
import com.pedigree.storage.ProjectRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class QuickSearchDialog {
    private final ProjectRepository.ProjectData data;
    private final Consumer<String> onChosen;

    public QuickSearchDialog(ProjectRepository.ProjectData data, Consumer<String> onChosen) {
        this.data = data;
        this.onChosen = onChosen;
    }

    public void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Quick Search");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        TextField query = new TextField();
        query.setPromptText("Type a name...");
        ListView<Individual> list = new ListView<>();
        ObservableList<Individual> items = FXCollections.observableArrayList();
        list.setItems(items);

        root.setTop(query);
        root.setCenter(list);

        QuickSearchService service = new QuickSearchService(data);
        query.textProperty().addListener((obs, o, n) -> {
            List<Individual> res = service.findByName(n != null ? n : "");
            items.setAll(res);
        });

        list.setOnMouseClicked(e -> {
            Individual sel = list.getSelectionModel().getSelectedItem();
            if (sel != null) {
                if (onChosen != null) onChosen.accept(sel.getId());
                stage.close();
            }
        });

        stage.setScene(new Scene(root, 400, 500));
        stage.showAndWait();
    }
}
