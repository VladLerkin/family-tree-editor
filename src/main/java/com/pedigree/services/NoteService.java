package com.pedigree.services;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.model.Note;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class NoteService {
    private final ProjectRepository.ProjectData data;

    public NoteService(ProjectRepository.ProjectData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public Note addNoteToIndividual(String individualId, Note note) {
        Objects.requireNonNull(individualId, "individualId");
        Objects.requireNonNull(note, "note");
        Individual i = data.individuals.stream()
                .filter(x -> x.getId().equals(individualId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Individual not found: " + individualId));
        i.getNotes().add(note);
        com.pedigree.util.DirtyFlag.setModified();
        return note;
    }

    public Note addNoteToFamily(String familyId, Note note) {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(note, "note");
        Family f = data.families.stream()
                .filter(x -> x.getId().equals(familyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family not found: " + familyId));
        f.getNotes().add(note);
        com.pedigree.util.DirtyFlag.setModified();
        return note;
    }

    public void removeNoteFromIndividual(String individualId, String noteId) {
        Objects.requireNonNull(individualId, "individualId");
        Objects.requireNonNull(noteId, "noteId");
        Individual i = data.individuals.stream()
                .filter(x -> x.getId().equals(individualId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Individual not found: " + individualId));
        if (i.getNotes().removeIf(n -> n.getId().equals(noteId))) {
            com.pedigree.util.DirtyFlag.setModified();
        }
    }

    public void removeNoteFromFamily(String familyId, String noteId) {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(noteId, "noteId");
        Family f = data.families.stream()
                .filter(x -> x.getId().equals(familyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Family not found: " + familyId));
        if (f.getNotes().removeIf(n -> n.getId().equals(noteId))) {
            com.pedigree.util.DirtyFlag.setModified();
        }
    }
}
