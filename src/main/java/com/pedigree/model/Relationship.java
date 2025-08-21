package com.pedigree.model;

public class Relationship {
    public enum Type { SPOUSE_TO_FAMILY, FAMILY_TO_CHILD }

    private Type type;
    private String fromId;
    private String toId;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getFromId() { return fromId; }
    public void setFromId(String fromId) { this.fromId = fromId; }
    public String getToId() { return toId; }
    public void setToId(String toId) { this.toId = toId; }
}


