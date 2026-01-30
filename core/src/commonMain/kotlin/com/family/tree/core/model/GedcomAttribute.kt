package com.family.tree.core.model

import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class GedcomAttribute(
    @SerialName("id")
    val id: GedcomAttributeId = GedcomAttributeId.generate(),
    @SerialName("tag")
    val tag: String = "",
    @SerialName("value")
    val value: String = ""
)

@Serializable(with = GedcomAttributeIdSerializer::class)
@JvmInline
value class GedcomAttributeId(val value: String) {
    companion object {
        fun generate(): GedcomAttributeId = GedcomAttributeId(com.family.tree.core.utils.uuid4())
    }
}

object GedcomAttributeIdSerializer : KSerializer<GedcomAttributeId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("GedcomAttributeId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: GedcomAttributeId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): GedcomAttributeId = GedcomAttributeId(decoder.decodeString())
}
