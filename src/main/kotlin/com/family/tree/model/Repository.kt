package com.family.tree.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.family.tree.util.DirtyFlag
import java.util.UUID

/**
 * Kotlin version of Repository. Preserves Jackson-annotated ctor and setters marking DirtyFlag.
 */
class Repository {
    val id: String
    var name: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var address: Address? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var phone: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var email: String? = null
        set(value) { field = value; DirtyFlag.setModified() }
    var website: String? = null
        set(value) { field = value; DirtyFlag.setModified() }

    constructor() : this(UUID.randomUUID().toString())

    constructor(id: String) {
        this.id = id
    }

    @JsonCreator
    constructor(
        @JsonProperty("id") id: String?,
        @JsonProperty("name") name: String?,
        @JsonProperty("address") address: Address?,
        @JsonProperty("phone") phone: String?,
        @JsonProperty("email") email: String?,
        @JsonProperty("website") website: String?
    ) {
        this.id = id ?: UUID.randomUUID().toString()
        this.name = name
        this.address = address
        this.phone = phone
        this.email = email
        this.website = website
    }
}
