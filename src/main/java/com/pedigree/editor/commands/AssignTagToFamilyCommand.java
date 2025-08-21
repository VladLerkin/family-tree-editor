package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Family;
import com.pedigree.model.Tag;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AssignTagToFamilyCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String familyId;
    private final Tag tag;
    private boolean addedToFamily;
    private boolean addedToCatalog;

    public AssignTagToFamilyCommand(ProjectRepository.ProjectData data, String familyId, Tag tag) {
        this.data = Objects.requireNonNull(data, "data");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    @Override
    public void execute() {
        Family f = data.families.stream().filter(fam -> fam.getId().equals(familyId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No family: " + familyId));
        if (f.getTags().stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            f.getTags().add(tag);
            addedToFamily = true;
        }
        if (data.tags.stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            data.tags.add(tag);
            addedToCatalog = true;
        }
    }

    @Override
    public void undo() {
        if (addedToFamily) {
            data.families.stream().filter(fam -> fam.getId().equals(familyId)).findFirst()
                    .ifPresent(f -> f.getTags().removeIf(t -> t.getId().equals(tag.getId())));
        }
        if (addedToCatalog) {
            data.tags.removeIf(t -> t.getId().equals(tag.getId()));
        }
    }
}



