package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class EditIndividualNameCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String individualId;
    private final String newFirstName;
    private final String newLastName;
    private String oldFirstName;
    private String oldLastName;

    public EditIndividualNameCommand(ProjectRepository.ProjectData data, String individualId, String newFirstName, String newLastName) {
        this.data = Objects.requireNonNull(data, "data");
        this.individualId = Objects.requireNonNull(individualId, "individualId");
        this.newFirstName = newFirstName;
        this.newLastName = newLastName;
    }

    @Override
    public void execute() {
        Individual i = data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No individual: " + individualId));
        oldFirstName = i.getFirstName();
        oldLastName = i.getLastName();
        i.setFirstName(newFirstName);
        i.setLastName(newLastName);
    }

    @Override
    public void undo() {
        Individual i = data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No individual: " + individualId));
        i.setFirstName(oldFirstName);
        i.setLastName(oldLastName);
    }
}



