package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Represents a GEDCOM source record (SOUR at level 0).
 */
public class Source {
    private final String id;
    private String title; // TITL
    private String abbreviation; // ABBR
    private String text; // TEXT
    private String publicationFacts; // PUBL
    private String agency; // AGNC (from DATA sub-structure)
    private String repositoryId; // Reference to Repository
    private String callNumber; // CALN
    private final List<GedcomAttribute> attributes; // For other tags like DATA/EVEN, etc.

    public Source() {
        this(UUID.randomUUID().toString());
    }

    public Source(String id) {
        this.id = id;
        this.attributes = new com.pedigree.util.DirtyObservableList<>();
    }

    @JsonCreator
    public Source(
            @JsonProperty("id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("abbreviation") String abbreviation,
            @JsonProperty("text") String text,
            @JsonProperty("publicationFacts") String publicationFacts,
            @JsonProperty("agency") String agency,
            @JsonProperty("repositoryId") String repositoryId,
            @JsonProperty("callNumber") String callNumber,
            @JsonProperty("attributes") List<GedcomAttribute> attributes
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title;
        this.abbreviation = abbreviation;
        this.text = text;
        this.publicationFacts = publicationFacts;
        this.agency = agency;
        this.repositoryId = repositoryId;
        this.callNumber = callNumber;
        this.attributes = attributes != null ? new com.pedigree.util.DirtyObservableList<>(attributes) : new com.pedigree.util.DirtyObservableList<>();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; com.pedigree.util.DirtyFlag.setModified(); }
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; com.pedigree.util.DirtyFlag.setModified(); }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPublicationFacts() { return publicationFacts; }
    public void setPublicationFacts(String publicationFacts) { this.publicationFacts = publicationFacts; com.pedigree.util.DirtyFlag.setModified(); }
    public String getAgency() { return agency; }
    public void setAgency(String agency) { this.agency = agency; com.pedigree.util.DirtyFlag.setModified(); }
    public String getRepositoryId() { return repositoryId; }
    public void setRepositoryId(String repositoryId) { this.repositoryId = repositoryId; com.pedigree.util.DirtyFlag.setModified(); }
    public String getCallNumber() { return callNumber; }
    public void setCallNumber(String callNumber) { this.callNumber = callNumber; com.pedigree.util.DirtyFlag.setModified(); }
    public List<GedcomAttribute> getAttributes() { return attributes; }
}
