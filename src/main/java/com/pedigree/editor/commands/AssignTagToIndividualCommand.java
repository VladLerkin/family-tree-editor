package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Individual;
import com.pedigree.model.Tag;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AssignTagToIndividualCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String individualId;
    private final Tag tag;
    private boolean addedToIndividual;
    private boolean addedToCatalog;

    public AssignTagToIndividualCommand(ProjectRepository.ProjectData data, String individualId, Tag tag) {
        this.data = Objects.requireNonNull(data, "data");
        this.individualId = Objects.requireNonNull(individualId, "individualId");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    @Override
    public void execute() {
        Individual i = data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No individual: " + individualId));

        if (i.getTags().stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            i.getTags().add(tag);
            addedToIndividual = true;
        }
        if (data.tags.stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            data.tags.add(tag);
            addedToCatalog = true;
        }
    }

    @Override
    public void undo() {
        if (addedToIndividual) {
            data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                    .ifPresent(ind -> ind.getTags().removeIf(t -> t.getId().equals(tag.getId())));
        }
        if (addedToCatalog) {
            data.tags.removeIf(t -> t.getId().equals(tag.getId()));
        }
    }
}



