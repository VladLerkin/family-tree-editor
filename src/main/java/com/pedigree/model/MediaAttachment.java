package com.pedigree.model;

import java.util.UUID;

public class MediaAttachment {
    private final String id = UUID.randomUUID().toString();
    private String fileName;
    private String relativePath;

    public String getId() { return id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; com.pedigree.util.DirtyFlag.setModified(); }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; com.pedigree.util.DirtyFlag.setModified(); }
}


