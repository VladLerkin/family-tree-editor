package com.pedigree.model;

import java.time.LocalDate;

public class Event {
    private String type; // e.g., BIRTH, DEATH, MARRIAGE
    private LocalDate date;
    private String place;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
}


