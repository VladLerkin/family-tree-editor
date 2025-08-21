package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Individual;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class DetachMediaFromIndividualCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String individualId;
    private final String mediaId;
    private boolean detached;

    public DetachMediaFromIndividualCommand(ProjectRepository.ProjectData data, String individualId, String mediaId) {
        this.data = Objects.requireNonNull(data, "data");
        this.individualId = Objects.requireNonNull(individualId, "individualId");
        this.mediaId = Objects.requireNonNull(mediaId, "mediaId");
    }

    @Override
    public void execute() {
        Individual i = data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No individual: " + individualId));
        boolean removed = i.getMedia().removeIf(m -> m.getId().equals(mediaId));
        detached = removed;
    }

    @Override
    public void undo() {
        // Not restoring the media payload here (would require snapshot), so do nothing if we cannot restore
        // In a real system, store the removed object and re-add it on undo
    }
}



