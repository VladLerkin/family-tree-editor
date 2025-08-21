package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Family;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AddFamilyCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final Family familyToAdd;
    private boolean added;

    public AddFamilyCommand(ProjectRepository.ProjectData data, Family familyToAdd) {
        this.data = Objects.requireNonNull(data, "data");
        this.familyToAdd = Objects.requireNonNull(familyToAdd, "familyToAdd");
    }

    @Override
    public void execute() {
        boolean exists = data.families.stream().anyMatch(f -> f.getId().equals(familyToAdd.getId()));
        if (exists) {
            throw new IllegalStateException("Family with id already exists: " + familyToAdd.getId());
        }
        data.families.add(familyToAdd);
        added = true;
    }

    @Override
    public void undo() {
        if (!added) { return; }
        data.families.removeIf(f -> f.getId().equals(familyToAdd.getId()));
        added = false;
    }
}



