package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AddIndividualCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final Individual individualToAdd;
    private boolean added;

    public AddIndividualCommand(ProjectRepository.ProjectData data, Individual individualToAdd) {
        this.data = Objects.requireNonNull(data, "data");
        this.individualToAdd = Objects.requireNonNull(individualToAdd, "individualToAdd");
    }

    @Override
    public void execute() {
        boolean exists = data.individuals.stream().anyMatch(i -> i.getId().equals(individualToAdd.getId()));
        if (exists) {
            throw new IllegalStateException("Individual with id already exists: " + individualToAdd.getId());
        }
        data.individuals.add(individualToAdd);
        added = true;
    }

    @Override
    public void undo() {
        if (!added) { return; }
        data.individuals.removeIf(i -> i.getId().equals(individualToAdd.getId()));
        added = false;
    }
}



