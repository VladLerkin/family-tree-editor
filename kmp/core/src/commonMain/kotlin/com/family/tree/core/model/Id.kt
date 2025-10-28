package com.family.tree.core.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = IndividualIdSerializer::class)
@JvmInline
value class IndividualId(val value: String)

@Serializable(with = FamilyIdSerializer::class)
@JvmInline
value class FamilyId(val value: String)

@Serializable(with = RelationshipIdSerializer::class)
@JvmInline
value class RelationshipId(val value: String)

// Custom serializers to handle plain string format from JavaFX .rel files
object IndividualIdSerializer : KSerializer<IndividualId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IndividualId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: IndividualId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): IndividualId = IndividualId(decoder.decodeString())
}

object FamilyIdSerializer : KSerializer<FamilyId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FamilyId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: FamilyId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): FamilyId = FamilyId(decoder.decodeString())
}

object RelationshipIdSerializer : KSerializer<RelationshipId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RelationshipId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: RelationshipId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): RelationshipId = RelationshipId(decoder.decodeString())
}
