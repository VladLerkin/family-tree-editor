package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a generic GEDCOM attribute (tag-value pair with optional sub-attributes).
 * Used for custom tags or attributes like PEDI, AGE, CAUS, etc.
 */
public class GedcomAttribute {
    private final String id;
    private String tag;
    private String value;

    public GedcomAttribute() {
        this(UUID.randomUUID().toString());
    }

    public GedcomAttribute(String id) {
        this.id = id;
    }

    @JsonCreator
    public GedcomAttribute(
            @JsonProperty("id") String id,
            @JsonProperty("tag") String tag,
            @JsonProperty("value") String value
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.tag = tag;
        this.value = value;
    }

    public String getId() { return id; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; com.pedigree.util.DirtyFlag.setModified(); }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; com.pedigree.util.DirtyFlag.setModified(); }
}
