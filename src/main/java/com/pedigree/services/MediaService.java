package com.pedigree.services;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.model.MediaAttachment;
import com.pedigree.storage.ProjectRepository;

import java.util.Objects;
import java.util.Optional;

public class MediaService {
    private final ProjectRepository.ProjectData data;

    public MediaService(ProjectRepository.ProjectData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public void attachToIndividual(String individualId, MediaAttachment media) {
        Individual individual = findIndividual(individualId).orElseThrow();
        boolean exists = individual.getMedia().stream().anyMatch(m -> m.getId().equals(media.getId()));
        if (!exists) {
            individual.getMedia().add(media);
        }
    }

    public void attachToFamily(String familyId, MediaAttachment media) {
        Family family = findFamily(familyId).orElseThrow();
        boolean exists = family.getMedia().stream().anyMatch(m -> m.getId().equals(media.getId()));
        if (!exists) {
            family.getMedia().add(media);
        }
    }

    public void detachFromIndividual(String individualId, String mediaId) {
        findIndividual(individualId).ifPresent(i -> i.getMedia().removeIf(m -> m.getId().equals(mediaId)));
    }

    public void detachFromFamily(String familyId, String mediaId) {
        findFamily(familyId).ifPresent(f -> f.getMedia().removeIf(m -> m.getId().equals(mediaId)));
    }

    public byte[] getThumbnail(String mediaId) {
        throw new UnsupportedOperationException("Thumbnail generation not yet implemented");
    }

    private Optional<Individual> findIndividual(String id) {
        return data.individuals.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    private Optional<Family> findFamily(String id) {
        return data.families.stream().filter(f -> f.getId().equals(id)).findFirst();
    }
}



