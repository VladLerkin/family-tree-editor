package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Family;
import com.pedigree.model.MediaAttachment;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AttachMediaToFamilyCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String familyId;
    private final MediaAttachment media;
    private boolean attached;

    public AttachMediaToFamilyCommand(ProjectRepository.ProjectData data, String familyId, MediaAttachment media) {
        this.data = Objects.requireNonNull(data, "data");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.media = Objects.requireNonNull(media, "media");
    }

    @Override
    public void execute() {
        Family f = data.families.stream().filter(fam -> fam.getId().equals(familyId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No family: " + familyId));
        boolean exists = f.getMedia().stream().anyMatch(m -> m.getId().equals(media.getId()));
        if (!exists) {
            f.getMedia().add(media);
            attached = true;
        }
    }

    @Override
    public void undo() {
        if (!attached) { return; }
        data.families.stream().filter(fam -> fam.getId().equals(familyId)).findFirst()
                .ifPresent(f -> f.getMedia().removeIf(m -> m.getId().equals(media.getId())));
        attached = false;
    }
}



