package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a structured GEDCOM address (ADDR with sub-tags).
 */
public class Address {
    private String line1; // ADR1
    private String line2; // ADR2
    private String line3; // ADR3
    private String city; // CITY
    private String state; // STAE
    private String postalCode; // POST
    private String country; // CTRY

    public Address() {
    }

    @JsonCreator
    public Address(
            @JsonProperty("line1") String line1,
            @JsonProperty("line2") String line2,
            @JsonProperty("line3") String line3,
            @JsonProperty("city") String city,
            @JsonProperty("state") String state,
            @JsonProperty("postalCode") String postalCode,
            @JsonProperty("country") String country
    ) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; com.pedigree.util.DirtyFlag.setModified(); }
    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; com.pedigree.util.DirtyFlag.setModified(); }
    public String getLine3() { return line3; }
    public void setLine3(String line3) { this.line3 = line3; com.pedigree.util.DirtyFlag.setModified(); }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; com.pedigree.util.DirtyFlag.setModified(); }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; com.pedigree.util.DirtyFlag.setModified(); }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; com.pedigree.util.DirtyFlag.setModified(); }
}
