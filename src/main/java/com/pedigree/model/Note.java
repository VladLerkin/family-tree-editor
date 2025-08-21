package com.pedigree.model;

import java.util.UUID;

public class Note {
    private final String id = UUID.randomUUID().toString();
    private String text;

    public String getId() { return id; }
    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
        com.pedigree.util.DirtyFlag.setModified();
    }
}


