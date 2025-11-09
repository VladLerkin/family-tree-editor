package com.family.tree.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class GedcomEvent(
    @SerialName("id")
    val id: GedcomEventId = GedcomEventId.generate(),
    @SerialName("type")
    val type: String = "", // BIRT, DEAT, BURI, MARR, ADOP, RESI, etc.
    @SerialName("date")
    val date: String = "",
    @SerialName("place")
    val place: String = "",
    @SerialName("sources")
    val sources: List<SourceCitation> = emptyList(),
    @SerialName("notes")
    val notes: List<Note> = emptyList(),
    @SerialName("attributes")
    val attributes: List<GedcomAttribute> = emptyList()
)

@Serializable(with = GedcomEventIdSerializer::class)
@JvmInline
value class GedcomEventId(val value: String) {
    companion object {
        fun generate(): GedcomEventId = GedcomEventId(uuid4())
    }
}

object GedcomEventIdSerializer : KSerializer<GedcomEventId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GedcomEventId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: GedcomEventId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): GedcomEventId = GedcomEventId(decoder.decodeString())
}

private fun uuid4(): String {
    val chars = "0123456789abcdef"
    return buildString(36) {
        for (i in 0 until 36) {
            if (i == 8 || i == 13 || i == 18 || i == 23) append('-')
            else append(chars.random())
        }
    }
}
