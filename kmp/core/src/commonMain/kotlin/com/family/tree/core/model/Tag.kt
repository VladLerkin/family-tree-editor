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
data class Tag(
    @SerialName("id")
    val id: TagId = TagId.generate(),
    @SerialName("name")
    val name: String = ""
)

@Serializable(with = TagIdSerializer::class)
@JvmInline
value class TagId(val value: String) {
    companion object {
        fun generate(): TagId = TagId(uuid4())
    }
}

object TagIdSerializer : KSerializer<TagId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TagId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: TagId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): TagId = TagId(decoder.decodeString())
}

// Simple UUID generator for common code
private fun uuid4(): String {
    val chars = "0123456789abcdef"
    return buildString(36) {
        for (i in 0 until 36) {
            if (i == 8 || i == 13 || i == 18 || i == 23) append('-')
            else append(chars.random())
        }
    }
}
