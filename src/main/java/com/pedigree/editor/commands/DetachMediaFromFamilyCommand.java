package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Family;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class DetachMediaFromFamilyCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String familyId;
    private final String mediaId;

    public DetachMediaFromFamilyCommand(ProjectRepository.ProjectData data, String familyId, String mediaId) {
        this.data = Objects.requireNonNull(data, "data");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.mediaId = Objects.requireNonNull(mediaId, "mediaId");
    }

    @Override
    public void execute() {
        Family f = data.families.stream().filter(fam -> fam.getId().equals(familyId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No family: " + familyId));
        f.getMedia().removeIf(m -> m.getId().equals(mediaId));
    }

    @Override
    public void undo() {
        // Not restoring the media payload here; would need snapshot
    }
}



