package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents any GEDCOM event (BIRT, DEAT, BURI, MARR, ADOP, RESI, etc.)
 * with support for date, place, sources, notes, and custom attributes.
 */
public class GedcomEvent {
    private final String id;
    private String type; // BIRT, DEAT, BURI, MARR, ADOP, RESI, etc.
    private String date; // Free-form date string as in GEDCOM
    private String place;
    private final List<SourceCitation> sources;
    private final List<Note> notes;
    private final List<GedcomAttribute> attributes; // For custom sub-tags like PEDI, AGE, etc.

    public GedcomEvent() {
        this(UUID.randomUUID().toString());
    }

    public GedcomEvent(String id) {
        this.id = id;
        this.sources = new com.pedigree.util.DirtyObservableList<>();
        this.notes = new com.pedigree.util.DirtyObservableList<>();
        this.attributes = new com.pedigree.util.DirtyObservableList<>();
    }

    @JsonCreator
    public GedcomEvent(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("date") String date,
            @JsonProperty("place") String place,
            @JsonProperty("sources") List<SourceCitation> sources,
            @JsonProperty("notes") List<Note> notes,
            @JsonProperty("attributes") List<GedcomAttribute> attributes
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.type = type;
        this.date = date;
        this.place = place;
        this.sources = sources != null ? new com.pedigree.util.DirtyObservableList<>(sources) : new com.pedigree.util.DirtyObservableList<>();
        this.notes = notes != null ? new com.pedigree.util.DirtyObservableList<>(notes) : new com.pedigree.util.DirtyObservableList<>();
        this.attributes = attributes != null ? new com.pedigree.util.DirtyObservableList<>(attributes) : new com.pedigree.util.DirtyObservableList<>();
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; com.pedigree.util.DirtyFlag.setModified(); }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; com.pedigree.util.DirtyFlag.setModified(); }
    public List<SourceCitation> getSources() { return sources; }
    public List<Note> getNotes() { return notes; }
    public List<GedcomAttribute> getAttributes() { return attributes; }
}
