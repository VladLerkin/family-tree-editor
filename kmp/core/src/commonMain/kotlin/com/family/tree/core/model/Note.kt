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
data class Note(
    @SerialName("id")
    val id: NoteId = NoteId.generate(),
    @SerialName("text")
    val text: String = "",
    @SerialName("sources")
    val sources: List<SourceCitation> = emptyList()
)

@Serializable(with = NoteIdSerializer::class)
@JvmInline
value class NoteId(val value: String) {
    companion object {
        fun generate(): NoteId = NoteId(uuid4())
    }
}

object NoteIdSerializer : KSerializer<NoteId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NoteId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: NoteId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): NoteId = NoteId(decoder.decodeString())
}

private fun uuid4(): String {
    val chars = "0123456789abcdef"
    return buildString(36) {
        for (i in 0 until 36) {
            if (i == 8 || i == 13 || i == 18 || i == 23) append('-')
            else append(chars[Random.nextInt(chars.length)])
        }
    }
}
