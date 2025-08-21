package com.pedigree.editor.commands;

import com.pedigree.editor.Command;
import com.pedigree.model.Individual;
import com.pedigree.model.MediaAttachment;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;

public class AttachMediaToIndividualCommand implements Command {
    private final ProjectRepository.ProjectData data;
    private final String individualId;
    private final MediaAttachment media;
    private boolean attached;

    public AttachMediaToIndividualCommand(ProjectRepository.ProjectData data, String individualId, MediaAttachment media) {
        this.data = Objects.requireNonNull(data, "data");
        this.individualId = Objects.requireNonNull(individualId, "individualId");
        this.media = Objects.requireNonNull(media, "media");
    }

    @Override
    public void execute() {
        Individual i = data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No individual: " + individualId));
        boolean exists = i.getMedia().stream().anyMatch(m -> m.getId().equals(media.getId()));
        if (!exists) {
            i.getMedia().add(media);
            attached = true;
        }
    }

    @Override
    public void undo() {
        if (!attached) { return; }
        data.individuals.stream().filter(ind -> ind.getId().equals(individualId)).findFirst()
                .ifPresent(ind -> ind.getMedia().removeIf(m -> m.getId().equals(media.getId())));
        attached = false;
    }
}



