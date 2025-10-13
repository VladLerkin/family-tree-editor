package com.pedigree.model;

import java.util.List;
import java.util.UUID;

public class Note {
    private final String id = UUID.randomUUID().toString();
    private String text;
    private final List<SourceCitation> sources = new com.pedigree.util.DirtyObservableList<>();

    public String getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
        com.pedigree.util.DirtyFlag.setModified();
    }
    public List<SourceCitation> getSources() { return sources; }
}


