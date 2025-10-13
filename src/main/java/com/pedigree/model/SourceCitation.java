package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a citation to a source (SOUR tag) with optional page/detail information.
 */
public class SourceCitation {
    private final String id;
    private String sourceId; // Reference to Source object
    private String page; // PAGE sub-tag value
    private String text; // Embedded text if inline source

    public SourceCitation() {
        this(UUID.randomUUID().toString());
    }

    public SourceCitation(String id) {
        this.id = id;
    }

    @JsonCreator
    public SourceCitation(
            @JsonProperty("id") String id,
            @JsonProperty("sourceId") String sourceId,
            @JsonProperty("page") String page,
            @JsonProperty("text") String text
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.sourceId = sourceId;
        this.page = page;
        this.text = text;
    }

    public String getId() { return id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPage() { return page; }
    public void setPage(String page) { this.page = page; com.pedigree.util.DirtyFlag.setModified(); }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; com.pedigree.util.DirtyFlag.setModified(); }
}
