package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag

/**
 * Represents a structured GEDCOM address (ADDR with sub-tags).
 */
class Address {
    var line1: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var line2: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var line3: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var city: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var state: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var postalCode: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var country: String? = null
        set(value) { field = value; DirtyFlag.setModified() }

    constructor()

    @JsonCreator
    constructor(
        @JsonProperty("line1") line1: String?,
        @JsonProperty("line2") line2: String?,
        @JsonProperty("line3") line3: String?,
        @JsonProperty("city") city: String?,
        @JsonProperty("state") state: String?,
        @JsonProperty("postalCode") postalCode: String?,
        @JsonProperty("country") country: String?
    ) {
        this.line1 = line1
        this.line2 = line2
        this.line3 = line3
        this.city = city
        this.state = state
        this.postalCode = postalCode
        this.country = country
    }
}
