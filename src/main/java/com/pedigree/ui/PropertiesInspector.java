package com.pedigree.ui;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.model.MediaAttachment;
import com.pedigree.model.Note;
import com.pedigree.model.Tag;
import com.pedigree.services.MediaManager;
import com.pedigree.services.MediaService;
import com.pedigree.services.TagService;
import com.pedigree.services.NoteService;
import com.pedigree.storage.ProjectRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public class PropertiesInspector {
    private final VBox root = new VBox(10);

    private ProjectRepository.ProjectData data;
    private Path projectFilePath;

    // Header
    private final Label header = new Label("Properties");
    private final Label selectionInfo = new Label("(no selection)");

    // Tags
    private final ListView<Tag> tagList = new ListView<>();
    private final TextField tagInput = new TextField();
    private final Button addTagBtn = new Button("Add Tag");
    private final Button removeTagBtn = new Button("Remove Selected Tag");

    // Notes
    private final ListView<Note> noteList = new ListView<>();
    private final TextArea noteEditor = new TextArea();
    private final Button addNoteBtn = new Button("Add Note");
    private final Button removeNoteBtn = new Button("Remove Selected Note");

    // Media
    private final ListView<MediaAttachment> mediaList = new ListView<>();
    private final Button addMediaBtn = new Button("Add Media...");
    private final Button removeMediaBtn = new Button("Remove Selected Media");

    // Layout helpers
    private final TitledPane tagsPane = new TitledPane("Tags", buildTagsPane());
    private final TitledPane notesPane = new TitledPane("Notes", buildNotesPane());
    private final TitledPane mediaPane = new TitledPane("Media", buildMediaPane());

    private String currentId; // selected entity id

    public PropertiesInspector() {
        root.setPadding(new Insets(10));
        root.getChildren().addAll(header, selectionInfo, tagsPane, notesPane, mediaPane);
        tagsPane.setExpanded(true);
        notesPane.setExpanded(false);
        mediaPane.setExpanded(false);

        // Wire up note editor to update selected note's text
        noteList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            noteEditor.setText(n != null ? n.getText() : "");
        });
        noteEditor.textProperty().addListener((obs, o, n) -> {
            Note sel = noteList.getSelectionModel().getSelectedItem();
            if (sel != null) {
                sel.setText(n);
                // Update preview in the list
                noteList.refresh();
            }
        });

        // Remove buttons disabled when nothing selected
        removeTagBtn.disableProperty().bind(tagList.getSelectionModel().selectedItemProperty().isNull());
        removeNoteBtn.disableProperty().bind(noteList.getSelectionModel().selectedItemProperty().isNull());
        removeMediaBtn.disableProperty().bind(mediaList.getSelectionModel().selectedItemProperty().isNull());
    }

    public Node getView() {
        return root;
    }

    public void setProjectContext(ProjectRepository.ProjectData data, Path projectFilePath) {
        this.data = data;
        this.projectFilePath = projectFilePath;
        clearDetails();
    }

    public void setSelection(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            selectionInfo.setText("(no selection)");
            currentId = null;
            clearDetails();
            return;
        }
        if (ids.size() > 1) {
            selectionInfo.setText("Multiple selected (" + ids.size() + ")");
            currentId = null;
            clearDetails();
            return;
        }
        currentId = ids.iterator().next();
        selectionInfo.setText("Selected: " + currentId);
        populateDetails();
    }

    private void populateDetails() {
        if (data == null || currentId == null) {
            clearDetails();
            return;
        }
        Optional<Individual> ind = data.individuals.stream().filter(i -> i.getId().equals(currentId)).findFirst();
        Optional<Family> fam = data.families.stream().filter(f -> f.getId().equals(currentId)).findFirst();

        if (ind.isPresent()) {
            Individual i = ind.get();
            tagList.getItems().setAll(i.getTags());
            noteList.getItems().setAll(i.getNotes());
            mediaList.getItems().setAll(i.getMedia());
        } else if (fam.isPresent()) {
            Family f = fam.get();
            tagList.getItems().setAll(f.getTags());
            noteList.getItems().setAll(f.getNotes());
            mediaList.getItems().setAll(f.getMedia());
        } else {
            clearDetails();
        }
        mediaList.setCellFactory(list -> new MediaCell(projectFilePath));
        tagList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Tag item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        noteList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : preview(item.getText()));
            }
            private String preview(String s) {
                if (s == null) return "";
                return s.length() > 60 ? s.substring(0, 60) + "…" : s;
            }
        });
    }

    private void clearDetails() {
        tagList.getItems().clear();
        noteList.getItems().clear();
        noteEditor.clear();
        mediaList.getItems().clear();
    }

    private Pane buildTagsPane() {
        VBox box = new VBox(6);
        tagInput.setPromptText("New tag...");
        HBox actions = new HBox(6, tagInput, addTagBtn, removeTagBtn);
        VBox.setVgrow(tagList, Priority.ALWAYS);
        box.getChildren().addAll(tagList, actions);

        addTagBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            String name = tagInput.getText();
            if (name == null || name.isBlank()) return;
            Tag tag = new Tag();
            tag.setName(name.trim());
            TagService ts = new TagService(data);
            if (isIndividual(currentId)) ts.assignTagToIndividual(currentId, tag);
            else if (isFamily(currentId)) ts.assignTagToFamily(currentId, tag);
            tagInput.clear();
            populateDetails();
        });

        removeTagBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            Tag sel = tagList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            TagService ts = new TagService(data);
            if (isIndividual(currentId)) ts.removeTagFromIndividual(currentId, sel.getId());
            else if (isFamily(currentId)) ts.removeTagFromFamily(currentId, sel.getId());
            populateDetails();
        });

        return box;
    }

    private Pane buildNotesPane() {
        VBox box = new VBox(6);
        noteEditor.setPromptText("Note text...");
        HBox actions = new HBox(6, addNoteBtn, removeNoteBtn);
        VBox.setVgrow(noteList, Priority.ALWAYS);
        VBox.setVgrow(noteEditor, Priority.SOMETIMES);
        box.getChildren().addAll(noteList, noteEditor, actions);

        addNoteBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            Note note = new Note();
            note.setText("New note");
            if (isIndividual(currentId)) {
                data.individuals.stream().filter(i -> i.getId().equals(currentId)).findFirst().ifPresent(i -> i.getNotes().add(note));
            } else if (isFamily(currentId)) {
                data.families.stream().filter(f -> f.getId().equals(currentId)).findFirst().ifPresent(f -> f.getNotes().add(note));
            }
            populateDetails();
            // Select the newly added note and focus editor for immediate editing
            if (!noteList.getItems().isEmpty()) {
                int lastIdx = noteList.getItems().size() - 1;
                noteList.getSelectionModel().select(lastIdx);
                noteList.scrollTo(lastIdx);
                noteEditor.requestFocus();
                noteEditor.selectAll();
            }
        });

        removeNoteBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            Note sel = noteList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (isIndividual(currentId)) {
                data.individuals.stream().filter(i -> i.getId().equals(currentId)).findFirst().ifPresent(i -> i.getNotes().removeIf(n -> n.getId().equals(sel.getId())));
            } else if (isFamily(currentId)) {
                data.families.stream().filter(f -> f.getId().equals(currentId)).findFirst().ifPresent(f -> f.getNotes().removeIf(n -> n.getId().equals(sel.getId())));
            }
            noteEditor.clear();
            populateDetails();
        });

        return box;
    }

    private Pane buildMediaPane() {
        VBox box = new VBox(6);
        HBox actions = new HBox(6, addMediaBtn, removeMediaBtn);
        VBox.setVgrow(mediaList, Priority.ALWAYS);
        box.getChildren().addAll(mediaList, actions);

        addMediaBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Add Media");
            chooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                    new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );
            var file = chooser.showOpenDialog(getActiveWindow());
            if (file == null) return;
            try {
                MediaAttachment att = MediaManager.copyIntoProject(projectFilePath, file.toPath());
                MediaService ms = new MediaService(data);
                if (isIndividual(currentId)) ms.attachToIndividual(currentId, att);
                else if (isFamily(currentId)) ms.attachToFamily(currentId, att);
                populateDetails();
            } catch (IOException ex) {
                Dialogs.showError("Add Media Failed", ex.getMessage());
            }
        });

        removeMediaBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            MediaAttachment sel = mediaList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            MediaService ms = new MediaService(data);
            if (isIndividual(currentId)) ms.detachFromIndividual(currentId, sel.getId());
            else if (isFamily(currentId)) ms.detachFromFamily(currentId, sel.getId());
            // Leave file on disk to avoid accidental data loss
            populateDetails();
        });

        return box;
    }

    private static javafx.stage.Stage getActiveWindow() {
        for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
            if (w instanceof javafx.stage.Stage s && s.isShowing()) return s;
        }
        return null;
    }

    private boolean isIndividual(String id) {
        return data != null && data.individuals.stream().anyMatch(i -> i.getId().equals(id));
    }

    private boolean isFamily(String id) {
        return data != null && data.families.stream().anyMatch(f -> f.getId().equals(id));
    }

    private static class MediaCell extends ListCell<MediaAttachment> {
        private final HBox root = new HBox(8);
        private final ImageView iv = new ImageView();
        private final Label name = new Label();
        private final Path projectFilePath;

        MediaCell(Path projectFilePath) {
            this.projectFilePath = projectFilePath;
            iv.setFitWidth(64);
            iv.setFitHeight(48);
            iv.setPreserveRatio(true);
            root.getChildren().addAll(iv, name);
        }

        @Override
        protected void updateItem(MediaAttachment item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            name.setText(item.getFileName() != null ? item.getFileName() : item.getRelativePath());
            iv.setImage(null);
            try {
                Path p = MediaManager.resolveAttachmentPath(projectFilePath, item);
                if (p != null && Files.exists(p)) {
                    String lower = p.getFileName().toString().toLowerCase();
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                        Image img = new Image(p.toUri().toString(), 128, 128, true, true, true);
                        iv.setImage(img);
                    }
                }
            } catch (IOException ignored) { }
            setGraphic(root);
        }
    }
}
