package com.pedigree.services;

import com.pedigree.model.Family;
import com.pedigree.model.Individual;
import com.pedigree.model.Tag;
import com.pedigree.storage.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagService {
    private final ProjectRepository.ProjectData data;

    public TagService(ProjectRepository.ProjectData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public void assignTagToIndividual(String individualId, Tag tag) {
        Individual individual = findIndividual(individualId).orElseThrow();
        if (individual.getTags().stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            individual.getTags().add(tag);
        }
        if (data.tags.stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            data.tags.add(tag);
        }
    }

    public void assignTagToFamily(String familyId, Tag tag) {
        Family family = findFamily(familyId).orElseThrow();
        if (family.getTags().stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            family.getTags().add(tag);
        }
        if (data.tags.stream().noneMatch(t -> t.getId().equals(tag.getId()))) {
            data.tags.add(tag);
        }
    }

    public void removeTagFromIndividual(String individualId, String tagId) {
        findIndividual(individualId).ifPresent(i -> i.getTags().removeIf(t -> t.getId().equals(tagId)));
    }

    public void removeTagFromFamily(String familyId, String tagId) {
        findFamily(familyId).ifPresent(f -> f.getTags().removeIf(t -> t.getId().equals(tagId)));
    }

    public List<Individual> filterIndividualsByTagName(String tagName) {
        return data.individuals.stream()
                .filter(i -> i.getTags().stream().anyMatch(t -> tagName.equalsIgnoreCase(t.getName())))
                .collect(Collectors.toList());
    }

    public List<Family> filterFamiliesByTagName(String tagName) {
        return data.families.stream()
                .filter(f -> f.getTags().stream().anyMatch(t -> tagName.equalsIgnoreCase(t.getName())))
                .collect(Collectors.toList());
    }

    public List<Tag> getAllTags() { return new ArrayList<>(data.tags); }

    private Optional<Individual> findIndividual(String id) {
        return data.individuals.stream().filter(i -> i.getId().equals(id)).findFirst();
    }

    private Optional<Family> findFamily(String id) {
        return data.families.stream().filter(f -> f.getId().equals(id)).findFirst();
    }
}



