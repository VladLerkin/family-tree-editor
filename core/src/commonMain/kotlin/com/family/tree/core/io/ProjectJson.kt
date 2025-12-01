package com.family.tree.core.io

import com.family.tree.core.ProjectData
import kotlinx.serialization.json.Json

object ProjectJson {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: ProjectData): ByteArray {
        val dto = ProjectDto.fromDomain(data)
        return json.encodeToString(ProjectDto.serializer(), dto).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): ProjectData {
        val text = bytes.decodeToString()
        val dto = json.decodeFromString(ProjectDto.serializer(), text)
        return dto.toDomain()
    }
}
