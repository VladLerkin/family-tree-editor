package com.pedigree.model;

import java.util.UUID;

public class Tag {
    private final String id = UUID.randomUUID().toString();
    private String name;

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        com.pedigree.util.DirtyFlag.setModified();
    }
}


