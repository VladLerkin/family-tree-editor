package com.pedigree.ui;

import com.pedigree.model.Family;
import com.pedigree.model.GedcomEvent;
import com.pedigree.model.Individual;
import com.pedigree.model.MediaAttachment;
import com.pedigree.model.Note;
import com.pedigree.model.Source;
import com.pedigree.model.SourceCitation;
import com.pedigree.model.Tag;
import com.pedigree.services.MediaManager;
import com.pedigree.services.MediaService;
import com.pedigree.services.TagService;
import com.pedigree.storage.ProjectRepository;
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
    private final ScrollPane scroller = new ScrollPane();

    private ProjectRepository.ProjectData data;
    private Path projectFilePath;

    // Header
    private final Label header = new Label("Properties");
    private final Label selectionInfo = new Label("(no selection)");

    // Tags
    private final ListView<Tag> tagList = new ListView<>();
    private final TextField tagInput = new TextField();
    private final Button addTagBtn = new Button("+");
    private final Button removeTagBtn = new Button("-");

    // Notes
    private final ListView<Note> noteList = new ListView<>();
    private final TextArea noteEditor = new TextArea();
    private final ListView<SourceCitation> noteSourcesList = new ListView<>();
    private final TextField noteSourcePageField = new TextField();
    private final Button addNoteBtn = new Button("+");
    private final Button removeNoteBtn = new Button("-");
    private final Button addNoteSourceBtn = new Button("+");
    private final Button removeNoteSourceBtn = new Button("-");
    private final Button manageNoteSourcesBtn = new Button("...");

    // Media
    private final ListView<MediaAttachment> mediaList = new ListView<>();
    private final Button addMediaBtn = new Button("+");
    private final Button editMediaBtn = new Button("Edit...");
    private final Button openMediaBtn = new Button("Open");
    private final Button removeMediaBtn = new Button("-");

    // Events
    private final ListView<GedcomEvent> eventsList = new ListView<>();
    private final TextField evTypeField = new TextField();
    private final TextField evDateField = new TextField();
    private final TextField evPlaceField = new TextField();
    private final ListView<SourceCitation> evSourcesList = new ListView<>();
    private final TextField evSourcePageField = new TextField();
    private final Button addEventBtn = new Button("+");
    private final Button removeEventBtn = new Button("-");
    private final Button addEvSourceBtn = new Button("+");
    private final Button removeEvSourceBtn = new Button("-");
    private final Button manageSourcesBtn = new Button("...");

    // Layout helpers
    private final TitledPane eventsPane = new TitledPane("Events (GEDCOM)", buildEventsPane());
    private final TitledPane tagsPane = new TitledPane("Tags", buildTagsPane());
    private final TitledPane notesPane = new TitledPane("Notes", buildNotesPane());
    private final TitledPane mediaPane = new TitledPane("Media", buildMediaPane());

    private String currentId; // selected entity id

    public PropertiesInspector() {
        // Configure scrollable container
        root.setPadding(new Insets(10));
        root.setFillWidth(true);
        scroller.setContent(root);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.getChildren().addAll(header, selectionInfo, eventsPane, tagsPane, notesPane, mediaPane);
        eventsPane.setExpanded(true);
        tagsPane.setExpanded(false);
        notesPane.setExpanded(false);
        mediaPane.setExpanded(false);

        // Events selection listeners
        eventsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                evTypeField.setText(n.getType() != null ? n.getType() : "");
                evDateField.setText(n.getDate() != null ? n.getDate() : "");
                evPlaceField.setText(n.getPlace() != null ? n.getPlace() : "");
                evSourcesList.getItems().setAll(n.getSources());
            } else {
                evTypeField.clear();
                evDateField.clear();
                evPlaceField.clear();
                evSourcesList.getItems().clear();
            }
        });
        evTypeField.textProperty().addListener((obs, o, n) -> {
            GedcomEvent ev = eventsList.getSelectionModel().getSelectedItem();
            if (ev != null) { ev.setType(n != null ? n.trim() : null); eventsList.refresh(); }
        });
        evDateField.textProperty().addListener((obs, o, n) -> {
            GedcomEvent ev = eventsList.getSelectionModel().getSelectedItem();
            if (ev != null) { ev.setDate(n != null ? n.trim() : null); eventsList.refresh(); }
        });
        evPlaceField.textProperty().addListener((obs, o, n) -> {
            GedcomEvent ev = eventsList.getSelectionModel().getSelectedItem();
            if (ev != null) { ev.setPlace(n != null ? n.trim() : null); eventsList.refresh(); }
        });
        removeEventBtn.disableProperty().bind(eventsList.getSelectionModel().selectedItemProperty().isNull());
        // Disable Add Source when no event is selected to avoid a no-op click
        addEvSourceBtn.disableProperty().bind(eventsList.getSelectionModel().selectedItemProperty().isNull());
        removeEvSourceBtn.disableProperty().bind(evSourcesList.getSelectionModel().selectedItemProperty().isNull());

        // PAGE editor wiring for selected source citation
        evSourcePageField.setPromptText("PAGE");
        evSourcePageField.setDisable(true);
        evSourcesList.getSelectionModel().selectedItemProperty().addListener((obs2, o2, n2) -> {
            evSourcePageField.setDisable(n2 == null);
            evSourcePageField.setText(n2 != null && n2.getPage() != null ? n2.getPage() : "");
        });
        evSourcePageField.textProperty().addListener((obs3, o3, n3) -> {
            SourceCitation scSel = evSourcesList.getSelectionModel().getSelectedItem();
            if (scSel != null) {
                scSel.setPage(n3 != null ? n3.trim() : null);
                evSourcesList.refresh();
            }
        });

        // Wire up note editor to update selected note's text
        noteList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            noteEditor.setText(n != null ? n.getText() : "");
            // Load sources of the selected note
            if (n != null) noteSourcesList.getItems().setAll(n.getSources());
            else noteSourcesList.getItems().clear();
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
        editMediaBtn.disableProperty().bind(mediaList.getSelectionModel().selectedItemProperty().isNull());
        openMediaBtn.disableProperty().bind(mediaList.getSelectionModel().selectedItemProperty().isNull());

        // Open link on double click if external; otherwise open edit dialog
        mediaList.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                MediaAttachment sel = mediaList.getSelectionModel().getSelectedItem();
                if (sel != null) {
                    String rel = sel.getRelativePath();
                    if (com.pedigree.services.MediaManager.isExternalLink(rel)) {
                        var uri = com.pedigree.services.MediaManager.toExternalUri(rel);
                        if (uri != null) {
                            try {
                                if (java.awt.Desktop.isDesktopSupported()) {
                                    java.awt.Desktop.getDesktop().browse(uri);
                                }
                            } catch (Exception ex) {
                                Dialogs.showError("Open Link", ex.getMessage());
                            }
                        }
                    } else {
                        MediaPropertiesDialog dlg = new MediaPropertiesDialog(projectFilePath, sel);
                        var res = dlg.showAndWait();
                        if (res.isPresent() && Boolean.TRUE.equals(res.get())) {
                            mediaList.refresh();
                        }
                    }
                }
            }
        });
    }

    public Node getView() {
        return scroller;
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
        selectionInfo.setText("Selected: " + buildSelectionLabel(currentId));
        populateDetails();
    }

    private String buildSelectionLabel(String id) {
        if (data == null || id == null) return id;
        // Try individual first
        for (Individual i : data.individuals) {
            if (id.equals(i.getId())) {
                String first = i.getFirstName();
                String last = i.getLastName();
                String full = ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
                return full.isBlank() ? id : full;
            }
        }
        // Try family
        for (Family f : data.families) {
            if (id.equals(f.getId())) {
                Individual husband = findIndividualById(f.getHusbandId());
                Individual wife = findIndividualById(f.getWifeId());
                String surname = deriveFamilySurname(husband, wife);
                String initials = buildSpousesInitials(husband, wife);
                String label;
                if (!surname.isBlank() && !initials.isBlank()) label = surname + " — " + initials;
                else if (!surname.isBlank()) label = surname;
                else if (!initials.isBlank()) label = initials;
                else label = id;
                return label;
            }
        }
        return id;
    }

    private Individual findIndividualById(String indId) {
        if (indId == null || indId.isBlank() || data == null) return null;
        for (Individual i : data.individuals) {
            if (indId.equals(i.getId())) return i;
        }
        return null;
    }

    private String deriveFamilySurname(Individual husband, Individual wife) {
        String hLast = husband != null ? husband.getLastName() : null;
        String wLast = wife != null ? wife.getLastName() : null;
        String h = hLast != null ? hLast.trim() : "";
        String w = wLast != null ? wLast.trim() : "";
        if (!h.isBlank() && !w.isBlank()) {
            if (h.equalsIgnoreCase(w)) return h; // same surname
            // different surnames: prefer husband's surname by convention, else wife's
            return h;
        }
        if (!h.isBlank()) return h;
        if (!w.isBlank()) return w;
        return "";
    }

    private String buildSpousesInitials(Individual husband, Individual wife) {
        String hi = buildInitials(husband);
        String wi = buildInitials(wife);
        if (!hi.isBlank() && !wi.isBlank()) return hi + " и " + wi;
        if (!hi.isBlank()) return hi;
        if (!wi.isBlank()) return wi;
        return "";
    }

    private String buildInitials(Individual person) {
        if (person == null) return "";
        String first = person.getFirstName();
        if (first != null && !first.isBlank()) {
            char c = Character.toUpperCase(first.trim().charAt(0));
            return c + ".";
        }
        return "";
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
            eventsList.getItems().setAll(i.getEvents());
        } else if (fam.isPresent()) {
            Family f = fam.get();
            tagList.getItems().setAll(f.getTags());
            noteList.getItems().setAll(f.getNotes());
            mediaList.getItems().setAll(f.getMedia());
            eventsList.getItems().setAll(f.getEvents());
        } else {
            clearDetails();
        }
        // Auto-select a meaningful event to show its sources immediately
        if (!eventsList.getItems().isEmpty()) {
            GedcomEvent sel = eventsList.getSelectionModel().getSelectedItem();
            if (sel == null) {
                // Prefer DEAT, then BIRT, else first
                GedcomEvent prefer = null;
                for (GedcomEvent ev : eventsList.getItems()) {
                    String t = ev.getType() != null ? ev.getType().trim().toUpperCase(java.util.Locale.ROOT) : "";
                    if (t.equals("DEAT")) { prefer = ev; break; }
                    if (prefer == null && t.equals("BIRT")) { prefer = ev; }
                }
                if (prefer == null) prefer = eventsList.getItems().get(0);
                eventsList.getSelectionModel().select(prefer);
                evSourcesList.getItems().setAll(prefer.getSources());
                evTypeField.setText(prefer.getType() != null ? prefer.getType() : "");
                evDateField.setText(prefer.getDate() != null ? prefer.getDate() : "");
                evPlaceField.setText(prefer.getPlace() != null ? prefer.getPlace() : "");
            }
        }
        // Auto-expand sections that have content; collapse those that are empty
        try {
            boolean hasEvents = !eventsList.getItems().isEmpty();
            boolean hasTags = !tagList.getItems().isEmpty();
            boolean hasNotes = !noteList.getItems().isEmpty();
            boolean hasMedia = !mediaList.getItems().isEmpty();
            eventsPane.setExpanded(hasEvents);
            tagsPane.setExpanded(!hasEvents && hasTags);
            notesPane.setExpanded(!hasEvents && !hasTags && hasNotes);
            mediaPane.setExpanded(!hasEvents && !hasTags && !hasNotes && hasMedia);
        } catch (Throwable ignore) { }
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
        eventsList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(GedcomEvent item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    String type = item.getType() != null ? item.getType() : "";
                    String date = item.getDate() != null ? item.getDate() : "";
                    String place = item.getPlace() != null ? item.getPlace() : "";
                    StringBuilder sb = new StringBuilder();
                    if (!type.isBlank()) sb.append(type);
                    if (!date.isBlank()) sb.append(sb.length()>0?" — ":"").append(date);
                    if (!place.isBlank()) sb.append(sb.length()>0?" — ":"").append(place);
                    setText(sb.toString());
                }
            }
        });
        evSourcesList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(SourceCitation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    String label;
                    String inlineText = item.getText();
                    if (inlineText != null && !inlineText.isBlank()) {
                        label = inlineText.trim();
                    } else {
                        String title = "Unknown source";
                        if (item.getSourceId() != null && data != null) {
                            for (Source s : data.sources) {
                                if (s.getId().equals(item.getSourceId())) { title = s.getTitle() != null && !s.getTitle().isBlank() ? s.getTitle() : title; break; }
                            }
                        }
                        label = title;
                    }
                    String page = item.getPage() != null ? item.getPage() : "";
                    setText(label + (page.isBlank()?"":" — PAGE: " + page));
                }
            }
        });
    }

    private void clearDetails() {
        tagList.getItems().clear();
        noteList.getItems().clear();
        noteEditor.clear();
        noteSourcesList.getItems().clear();
        noteSourcePageField.clear();
        mediaList.getItems().clear();
        eventsList.getItems().clear();
        evSourcesList.getItems().clear();
        evTypeField.clear();
        evDateField.clear();
        evPlaceField.clear();
    }

    private Pane buildTagsPane() {
        VBox box = new VBox(6);
        tagInput.setPromptText("New tag...");
        HBox actions = new HBox(6, tagInput, addTagBtn, removeTagBtn);
        // Make the tags list compact
        tagList.setPrefHeight(100);
        VBox.setVgrow(tagList, Priority.NEVER);
        box.getChildren().addAll(tagList, actions);

        addTagBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            String name = tagInput.getText();
            if (name == null || name.isBlank()) return;
            try {
                Tag tag = new Tag();
                tag.setName(name.trim());
                TagService ts = new TagService(data);
                if (isIndividual(currentId)) {
                    ts.assignTagToIndividual(currentId, tag);
                } else if (isFamily(currentId)) {
                    ts.assignTagToFamily(currentId, tag);
                }
                tagInput.clear();
                populateDetails();
            } catch (Exception ex) {
                Dialogs.showError("Add Tag Failed", ex.getMessage());
            }
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
        noteEditor.setWrapText(true);
        noteEditor.setPrefRowCount(8);
        noteEditor.setMinHeight(120);
        HBox actions = new HBox(6, addNoteBtn, removeNoteBtn);
        // Make the notes list compact
        noteList.setPrefHeight(100);
        VBox.setVgrow(noteList, Priority.NEVER);
        VBox.setVgrow(noteEditor, Priority.ALWAYS);

        // Note sources UI
        Label lNoteSrc = new Label("Sources:");
        addNoteSourceBtn.setTooltip(new Tooltip("Add Source…"));
        manageNoteSourcesBtn.setTooltip(new Tooltip("Manage Sources…"));
        removeNoteSourceBtn.setTooltip(new Tooltip("Remove citation"));
        HBox noteSrcActions = new HBox(6, addNoteSourceBtn, manageNoteSourcesBtn, removeNoteSourceBtn);
        VBox noteSrcBox = new VBox(4, lNoteSrc, noteSrcActions, noteSourcesList, new Label("PAGE:"), noteSourcePageField);
        // Make the note sources list compact
        noteSourcesList.setPrefHeight(80);
        VBox.setVgrow(noteSourcesList, Priority.NEVER);

        // Disable/enable controls
        addNoteSourceBtn.disableProperty().bind(noteList.getSelectionModel().selectedItemProperty().isNull());
        removeNoteSourceBtn.disableProperty().bind(noteSourcesList.getSelectionModel().selectedItemProperty().isNull());
        noteSourcePageField.setPromptText("PAGE");
        noteSourcePageField.setDisable(true);
        noteSourcesList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            noteSourcePageField.setDisable(n == null);
            noteSourcePageField.setText(n != null && n.getPage() != null ? n.getPage() : "");
        });
        noteSourcePageField.textProperty().addListener((obs, o, n) -> {
            SourceCitation scSel = noteSourcesList.getSelectionModel().getSelectedItem();
            if (scSel != null) {
                scSel.setPage(n != null ? n.trim() : null);
                noteSourcesList.refresh();
            }
        });

        // Actions
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

        addNoteSourceBtn.setOnAction(e -> {
            Note note = noteList.getSelectionModel().getSelectedItem();
            if (note == null) {
                Dialogs.showError("Add Source", "Сначала выберите заметку в списке.");
                return;
            }
            if (data == null) return;
            if (data.sources.isEmpty()) {
                Dialogs.showError("Add Source", "В проекте нет записей источников (SOUR). Добавьте источник в проект, затем повторите попытку.");
                return;
            }
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (int i = 0; i < data.sources.size(); i++) {
                Source s = data.sources.get(i);
                String t = s.getTitle() != null && !s.getTitle().isBlank() ? s.getTitle() : s.getId();
                labels.add((i+1) + ". " + t);
            }
            ChoiceDialog<String> dlg = new ChoiceDialog<>(labels.get(0), labels);
            dlg.setTitle("Attach Source");
            dlg.setHeaderText("Select a Source to cite");
            dlg.setContentText("Source:");
            var srcSel = dlg.showAndWait();
            if (srcSel.isEmpty()) return;
            int idx = labels.indexOf(srcSel.get());
            if (idx < 0) return;
            Source chosen = data.sources.get(idx);
            TextInputDialog pageDlg = new TextInputDialog("");
            pageDlg.setTitle("Source Page");
            pageDlg.setHeaderText("Optional: specify PAGE or details");
            pageDlg.setContentText("PAGE:");
            var pageRes = pageDlg.showAndWait();
            SourceCitation sc = new SourceCitation();
            sc.setSourceId(chosen.getId());
            if (pageRes.isPresent()) sc.setPage(pageRes.get().trim());
            note.getSources().add(sc);
            noteSourcesList.getItems().setAll(note.getSources());
        });
        removeNoteSourceBtn.setOnAction(e -> {
            Note note = noteList.getSelectionModel().getSelectedItem();
            SourceCitation sc = noteSourcesList.getSelectionModel().getSelectedItem();
            if (note == null || sc == null) return;
            note.getSources().removeIf(x -> x.getId().equals(sc.getId()));
            noteSourcesList.getItems().setAll(note.getSources());
        });
        manageNoteSourcesBtn.setOnAction(e -> {
            if (data == null) return;
            try {
                new SourcesManagerDialog(data).showAndWait();
                noteSourcesList.refresh();
            } catch (Throwable ex) {
                Dialogs.showError("Sources", ex.getMessage());
            }
        });

        // Cells
        noteSourcesList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(SourceCitation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); }
                else {
                    String label;
                    String inlineText = item.getText();
                    if (inlineText != null && !inlineText.isBlank()) {
                        label = inlineText.trim();
                    } else {
                        String title = "Unknown source";
                        if (item.getSourceId() != null && data != null) {
                            for (Source s : data.sources) {
                                if (s.getId().equals(item.getSourceId())) { title = s.getTitle() != null && !s.getTitle().isBlank() ? s.getTitle() : title; break; }
                            }
                        }
                        label = title;
                    }
                    String page = item.getPage() != null ? item.getPage() : "";
                    setText(label + (page.isBlank()?"":" — PAGE: " + page));
                }
            }
        });

        box.getChildren().addAll(noteList, noteEditor, actions, noteSrcBox);
        return box;
    }

    private Pane buildMediaPane() {
        VBox box = new VBox(6);
        HBox actions = new HBox(6, addMediaBtn, editMediaBtn, openMediaBtn, removeMediaBtn);
        // Make the media list more compact (~2x smaller)
        mediaList.setPrefHeight(150);
        VBox.setVgrow(mediaList, Priority.NEVER);
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

        editMediaBtn.setOnAction(e -> {
            MediaAttachment sel = mediaList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            MediaPropertiesDialog dlg = new MediaPropertiesDialog(projectFilePath, sel);
            var res = dlg.showAndWait();
            if (res.isPresent() && Boolean.TRUE.equals(res.get())) {
                mediaList.refresh();
            }
        });

        openMediaBtn.setOnAction(e -> {
            MediaAttachment sel = mediaList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            String rel = sel.getRelativePath();
            if (com.pedigree.services.MediaManager.isExternalLink(rel)) {
                var uri = com.pedigree.services.MediaManager.toExternalUri(rel);
                if (uri == null) {
                    Dialogs.showError("Open Link", "Неверная ссылка: " + rel);
                    return;
                }
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(uri);
                    } else {
                        Dialogs.showError("Open Link", "Desktop API не поддерживается на этой платформе");
                    }
                } catch (Exception ex) {
                    Dialogs.showError("Open Link", ex.getMessage());
                }
            } else {
                try {
                    java.nio.file.Path p = com.pedigree.services.MediaManager.resolveAttachmentPath(projectFilePath, sel);
                    if (p != null && java.nio.file.Files.exists(p)) {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().open(p.toFile());
                        }
                    } else {
                        Dialogs.showError("Open Media", "Файл не найден");
                    }
                } catch (Exception ex) {
                    Dialogs.showError("Open Media", ex.getMessage());
                }
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

    private Pane buildEventsPane() {
        VBox box = new VBox(8);
        // Top: events list with add/remove
        HBox evActions = new HBox(6, addEventBtn, removeEventBtn);
        // Make the events list compact
        eventsList.setPrefHeight(100);
        VBox.setVgrow(eventsList, Priority.NEVER);
        // Details: labels above fields (avoid truncated labels next to fields)
        VBox fieldsBox = new VBox(6);
        evTypeField.setPromptText("Type (e.g., BIRT, DEAT, MARR)");
        evDateField.setPromptText("Date (e.g., 5 JAN 1980, ABT 1900)");
        evPlaceField.setPromptText("Place");
        // Add date picker button like in Individual/Family dialogs
        Button evDatePickerBtn = new Button("...");
        HBox hbEvDate = new HBox(6, evDateField, evDatePickerBtn);
        evDatePickerBtn.setOnAction(evt -> {
            DatePhraseDialog dateDlg = new DatePhraseDialog();
            String initial = evDateField.getText();
            var res = dateDlg.show(initial);
            if (res.isPresent()) {
                String val = res.get();
                evDateField.setText(val);
                GedcomEvent selEv = eventsList.getSelectionModel().getSelectedItem();
                if (selEv != null) {
                    selEv.setDate(val);
                    eventsList.refresh();
                }
            }
        });
        fieldsBox.getChildren().addAll(new Label("Type:"), evTypeField, new Label("Date:"), hbEvDate, new Label("Place:"), evPlaceField);

        // Event sources
        Label lSrc = new Label("Sources:");
        addEvSourceBtn.setTooltip(new Tooltip("Add Source…"));
        manageSourcesBtn.setTooltip(new Tooltip("Manage Sources…"));
        removeEvSourceBtn.setTooltip(new Tooltip("Remove citation"));
        HBox srcActions = new HBox(6, addEvSourceBtn, manageSourcesBtn, removeEvSourceBtn);
        VBox evSrcBox = new VBox(4, lSrc, srcActions, evSourcesList, new Label("PAGE:"), evSourcePageField);
        // Make the event sources list compact
        evSourcesList.setPrefHeight(80);
        VBox.setVgrow(evSourcesList, Priority.NEVER);

        box.getChildren().addAll(eventsList, evActions, fieldsBox, evSrcBox);

        // Actions
        addEventBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            // Choose type
            var types = javafx.collections.FXCollections.observableArrayList("BIRT", "DEAT", "BURI", "MARR", "ADOP", "RESI", "EVEN");
            ChoiceDialog<String> dlg = new ChoiceDialog<>("EVEN", types);
            dlg.setTitle("Add Event");
            dlg.setHeaderText("Select GEDCOM event type");
            var sel = dlg.showAndWait();
            if (sel.isEmpty()) return;
            GedcomEvent ev = new GedcomEvent();
            ev.setType(sel.get());
            if (isIndividual(currentId)) {
                data.individuals.stream().filter(i -> i.getId().equals(currentId)).findFirst().ifPresent(i -> i.getEvents().add(ev));
            } else if (isFamily(currentId)) {
                data.families.stream().filter(f -> f.getId().equals(currentId)).findFirst().ifPresent(f -> f.getEvents().add(ev));
            }
            populateDetails();
            eventsList.getSelectionModel().select(ev);
            eventsList.scrollTo(ev);
            evTypeField.requestFocus();
        });
        removeEventBtn.setOnAction(e -> {
            if (data == null || currentId == null) return;
            GedcomEvent sel = eventsList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (isIndividual(currentId)) {
                data.individuals.stream().filter(i -> i.getId().equals(currentId)).findFirst().ifPresent(i -> i.getEvents().removeIf(ev -> ev.getId().equals(sel.getId())));
            } else if (isFamily(currentId)) {
                data.families.stream().filter(f -> f.getId().equals(currentId)).findFirst().ifPresent(f -> f.getEvents().removeIf(ev -> ev.getId().equals(sel.getId())));
            }
            populateDetails();
        });
        addEvSourceBtn.setOnAction(e -> {
            GedcomEvent ev = eventsList.getSelectionModel().getSelectedItem();
            if (ev == null) {
                Dialogs.showError("Add Source", "Сначала выберите событие в списке.");
                return;
            }
            if (data == null) return;
            if (data.sources.isEmpty()) {
                Dialogs.showError("Add Source", "В проекте нет записей источников (SOUR). Добавьте источник в проект, затем повторите попытку.");
                return;
            }
            // Choose source
            // Build labels for sources
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (int i = 0; i < data.sources.size(); i++) {
                Source s = data.sources.get(i);
                String t = s.getTitle() != null && !s.getTitle().isBlank() ? s.getTitle() : s.getId();
                labels.add((i+1) + ". " + t);
            }
            ChoiceDialog<String> dlg = new ChoiceDialog<>(labels.get(0), labels);
            dlg.setTitle("Attach Source");
            dlg.setHeaderText("Select a Source to cite");
            dlg.setContentText("Source:");
            var srcSel = dlg.showAndWait();
            if (srcSel.isEmpty()) return;
            int idx = labels.indexOf(srcSel.get());
            if (idx < 0) return;
            Source chosen = data.sources.get(idx);
            TextInputDialog pageDlg = new TextInputDialog("");
            pageDlg.setTitle("Source Page");
            pageDlg.setHeaderText("Optional: specify PAGE or details");
            pageDlg.setContentText("PAGE:");
            var pageRes = pageDlg.showAndWait();
            SourceCitation sc = new SourceCitation();
            sc.setSourceId(chosen.getId());
            if (pageRes.isPresent()) sc.setPage(pageRes.get().trim());
            ev.getSources().add(sc);
            evSourcesList.getItems().setAll(ev.getSources());
        });
        removeEvSourceBtn.setOnAction(e -> {
            GedcomEvent ev = eventsList.getSelectionModel().getSelectedItem();
            SourceCitation sc = evSourcesList.getSelectionModel().getSelectedItem();
            if (ev == null || sc == null) return;
            ev.getSources().removeIf(x -> x.getId().equals(sc.getId()));
            evSourcesList.getItems().setAll(ev.getSources());
        });
        manageSourcesBtn.setOnAction(e -> {
            if (data == null) return;
            try {
                new SourcesManagerDialog(data).showAndWait();
                // Refresh current lists to reflect possibly changed titles
                evSourcesList.refresh();
            } catch (Throwable ex) {
                Dialogs.showError("Sources", ex.getMessage());
            }
        });

        return box;
    }

    private javafx.stage.Stage getActiveWindow() {
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
            String rel = item.getRelativePath();
            boolean isLink = MediaManager.isExternalLink(rel);
            String display = item.getFileName() != null && !item.getFileName().isBlank()
                    ? item.getFileName()
                    : (rel != null ? rel : "(media)");
            iv.setImage(null);
            if (isLink) {
                name.setText(display + " (link)");
                setGraphic(root);
                return;
            }
            boolean missing = false;
            try {
                Path p = MediaManager.resolveAttachmentPath(projectFilePath, item);
                if (p != null && Files.exists(p)) {
                    String lower = p.getFileName().toString().toLowerCase();
                    if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")) {
                        Image img = new Image(p.toUri().toString(), 128, 128, true, true, true);
                        iv.setImage(img);
                    }
                } else {
                    missing = true;
                }
            } catch (IOException ex) {
                missing = true;
            }
            if (missing) {
                display = display + " (missing)";
            }
            name.setText(display);
            setGraphic(root);
        }
    }
}
