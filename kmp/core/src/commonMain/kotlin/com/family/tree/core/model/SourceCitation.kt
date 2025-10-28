package com.family.tree.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class SourceCitation(
    @SerialName("id")
    val id: SourceCitationId = SourceCitationId.generate(),
    @SerialName("sourceId")
    val sourceId: SourceId? = null,
    @SerialName("page")
    val page: String = "",
    @SerialName("text")
    val text: String = ""
)

@Serializable(with = SourceCitationIdSerializer::class)
@JvmInline
value class SourceCitationId(val value: String) {
    companion object {
        fun generate(): SourceCitationId = SourceCitationId(uuid4())
    }
}

@Serializable(with = SourceIdSerializer::class)
@JvmInline
value class SourceId(val value: String) {
    companion object {
        fun generate(): SourceId = SourceId(uuid4())
    }
}

object SourceCitationIdSerializer : KSerializer<SourceCitationId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SourceCitationId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: SourceCitationId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): SourceCitationId = SourceCitationId(decoder.decodeString())
}

object SourceIdSerializer : KSerializer<SourceId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SourceId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: SourceId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): SourceId = SourceId(decoder.decodeString())
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
