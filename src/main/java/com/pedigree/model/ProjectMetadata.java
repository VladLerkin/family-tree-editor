package com.pedigree.model;

import java.time.Instant;

public class ProjectMetadata {
    private String version = "0.1";
    private Instant createdAt = Instant.now();
    private Instant modifiedAt = Instant.now();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
}


