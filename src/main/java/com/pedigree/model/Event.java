package com.pedigree.model;

import java.time.LocalDate;

public class Event {
    private String type; // e.g., BIRTH, DEATH, MARRIAGE
    private LocalDate date;
    private String place;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; com.pedigree.util.DirtyFlag.setModified(); }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; com.pedigree.util.DirtyFlag.setModified(); }
}


