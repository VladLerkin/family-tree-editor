package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Family {
    private final String id;
    private String husbandId;
    private String wifeId;
    private final List<String> childrenIds;
    private String marriageDate;
    private String marriagePlace;
    private final List<GedcomEvent> events; // All family events (MARR, DIV, ANUL, etc.)
    private final List<SourceCitation> sources; // Source citations
    private final List<Note> notes;
    private final List<MediaAttachment> media;
    private final List<Tag> tags;

    public Family() {
        this(UUID.randomUUID().toString());
    }

    public Family(String id) {
        this.id = Objects.requireNonNull(id, "id");
        this.childrenIds = new com.pedigree.util.DirtyObservableList<>();
        this.events = new com.pedigree.util.DirtyObservableList<>();
        this.sources = new com.pedigree.util.DirtyObservableList<>();
        this.notes = new com.pedigree.util.DirtyObservableList<>();
        this.media = new com.pedigree.util.DirtyObservableList<>();
        this.tags = new com.pedigree.util.DirtyObservableList<>();
    }

    @JsonCreator
    public Family(
            @JsonProperty("id") String id,
            @JsonProperty("husbandId") String husbandId,
            @JsonProperty("wifeId") String wifeId,
            @JsonProperty("childrenIds") List<String> childrenIds,
            @JsonProperty("marriageDate") String marriageDate,
            @JsonProperty("marriagePlace") String marriagePlace,
            @JsonProperty("marriage") Event marriage,
            @JsonProperty("events") List<GedcomEvent> events,
            @JsonProperty("sources") List<SourceCitation> sources,
            @JsonProperty("notes") List<Note> notes,
            @JsonProperty("media") List<MediaAttachment> media,
            @JsonProperty("tags") List<Tag> tags
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.husbandId = husbandId;
        this.wifeId = wifeId;
        this.childrenIds = (childrenIds != null) ? new com.pedigree.util.DirtyObservableList<>(childrenIds) : new com.pedigree.util.DirtyObservableList<>();
        // Support backward compatibility: if old Event marriage exists, convert it
        if (marriage != null) {
            this.marriageDate = marriage.getDate() != null ? marriage.getDate().toString() : null;
            this.marriagePlace = marriage.getPlace();
        } else {
            this.marriageDate = marriageDate;
            this.marriagePlace = marriagePlace;
        }
        this.events = (events != null) ? new com.pedigree.util.DirtyObservableList<>(events) : new com.pedigree.util.DirtyObservableList<>();
        this.sources = (sources != null) ? new com.pedigree.util.DirtyObservableList<>(sources) : new com.pedigree.util.DirtyObservableList<>();
        this.notes = (notes != null) ? new com.pedigree.util.DirtyObservableList<>(notes) : new com.pedigree.util.DirtyObservableList<>();
        this.media = (media != null) ? new com.pedigree.util.DirtyObservableList<>(media) : new com.pedigree.util.DirtyObservableList<>();
        this.tags = (tags != null) ? new com.pedigree.util.DirtyObservableList<>(tags) : new com.pedigree.util.DirtyObservableList<>();
    }

    public String getId() { return id; }

    @JsonProperty("husbandId")
    public String getHusbandId() { return husbandId; }

    public void setHusbandId(String husbandId) {
        this.husbandId = husbandId;
        com.pedigree.util.DirtyFlag.setModified();
    }

    @JsonProperty("wifeId")
    public String getWifeId() { return wifeId; }

    public void setWifeId(String wifeId) {
        this.wifeId = wifeId;
        com.pedigree.util.DirtyFlag.setModified();
    }

    public List<String> getChildrenIds() { return childrenIds; }

    public String getMarriageDate() { return marriageDate; }

    public void setMarriageDate(String marriageDate) {
        this.marriageDate = marriageDate;
        com.pedigree.util.DirtyFlag.setModified();
    }

    public String getMarriagePlace() { return marriagePlace; }

    public void setMarriagePlace(String marriagePlace) {
        this.marriagePlace = marriagePlace;
        com.pedigree.util.DirtyFlag.setModified();
    }

    public List<GedcomEvent> getEvents() { return events; }

    public List<SourceCitation> getSources() { return sources; }

    public List<Note> getNotes() { return notes; }

    public List<MediaAttachment> getMedia() { return media; }

    public List<Tag> getTags() { return tags; }
}


