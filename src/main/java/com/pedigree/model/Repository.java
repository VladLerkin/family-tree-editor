package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a GEDCOM repository record (REPO at level 0).
 */
public class Repository {
    private final String id;
    private String name; // NAME
    private Address address; // ADDR structure
    private String phone; // PHON
    private String email; // EMAIL
    private String website; // WWW

    public Repository() {
        this(UUID.randomUUID().toString());
    }

    public Repository(String id) {
        this.id = id;
    }

    @JsonCreator
    public Repository(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("address") Address address,
            @JsonProperty("phone") String phone,
            @JsonProperty("email") String email,
            @JsonProperty("website") String website
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.website = website;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; com.pedigree.util.DirtyFlag.setModified(); }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; com.pedigree.util.DirtyFlag.setModified(); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; com.pedigree.util.DirtyFlag.setModified(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; com.pedigree.util.DirtyFlag.setModified(); }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; com.pedigree.util.DirtyFlag.setModified(); }
}
