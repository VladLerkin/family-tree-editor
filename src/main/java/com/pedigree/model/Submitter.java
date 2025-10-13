package com.pedigree.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Represents a GEDCOM submitter record (SUBM at level 0).
 */
public class Submitter {
    private final String id;
    private String name; // NAME
    private Address address; // ADDR structure
    private String phone; // PHON
    private String email; // EMAIL

    public Submitter() {
        this(UUID.randomUUID().toString());
    }

    public Submitter(String id) {
        this.id = id;
    }

    @JsonCreator
    public Submitter(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("address") Address address,
            @JsonProperty("phone") String phone,
            @JsonProperty("email") String email
    ) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
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
}
