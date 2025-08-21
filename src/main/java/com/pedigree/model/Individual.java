package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Individual {
    private final String id;
    private String firstName;
    private String lastName;
    private Gender gender;
    private LocalDate birthDate;
    private String birthPlace;
    private LocalDate deathDate;
    private String deathPlace;
    private final List<Note> notes;
    private final List<MediaAttachment> media;
    private final List<Tag> tags;

    public Individual(String firstName, String lastName, Gender gender) {
        this(UUID.randomUUID().toString(), firstName, lastName, gender);
    }

    public Individual(String id, String firstName, String lastName, Gender gender) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.gender = Objects.requireNonNull(gender, "gender");
        this.notes = new com.pedigree.util.DirtyObservableList<>();
        this.media = new com.pedigree.util.DirtyObservableList<>();
        this.tags = new com.pedigree.util.DirtyObservableList<>();
    }

    @JsonCreator
    public Individual(
            @JsonProperty("id") String id,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("gender") Gender gender,
            @JsonProperty("birthDate") LocalDate birthDate,
            @JsonProperty("birthPlace") String birthPlace,
            @JsonProperty("deathDate") LocalDate deathDate,
            @JsonProperty("deathPlace") String deathPlace,
            @JsonProperty("notes") List<Note> notes,
            @JsonProperty("media") List<MediaAttachment> media,
            @JsonProperty("tags") List<Tag> tags
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.gender = Objects.requireNonNull(gender, "gender");
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.deathDate = deathDate;
        this.deathPlace = deathPlace;
        this.notes = (notes != null) ? new com.pedigree.util.DirtyObservableList<>(notes) : new com.pedigree.util.DirtyObservableList<>();
        this.media = (media != null) ? new com.pedigree.util.DirtyObservableList<>(media) : new com.pedigree.util.DirtyObservableList<>();
        this.tags = (tags != null) ? new com.pedigree.util.DirtyObservableList<>(tags) : new com.pedigree.util.DirtyObservableList<>();
    }

    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; com.pedigree.util.DirtyFlag.setModified(); }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; com.pedigree.util.DirtyFlag.setModified(); }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; com.pedigree.util.DirtyFlag.setModified(); }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; com.pedigree.util.DirtyFlag.setModified(); }
    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; com.pedigree.util.DirtyFlag.setModified(); }
    public LocalDate getDeathDate() { return deathDate; }
    public void setDeathDate(LocalDate deathDate) { this.deathDate = deathDate; com.pedigree.util.DirtyFlag.setModified(); }
    public String getDeathPlace() { return deathPlace; }
    public void setDeathPlace(String deathPlace) { this.deathPlace = deathPlace; com.pedigree.util.DirtyFlag.setModified(); }
    public List<Note> getNotes() { return notes; }
    public List<MediaAttachment> getMedia() { return media; }
    public List<Tag> getTags() { return tags; }
}


