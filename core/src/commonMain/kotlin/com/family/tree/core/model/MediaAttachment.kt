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
data class MediaAttachment(
    @SerialName("id")
    val id: MediaAttachmentId = MediaAttachmentId.generate(),
    @SerialName("fileName")
    val fileName: String = "",
    @SerialName("relativePath")
    val relativePath: String = ""
)

@Serializable(with = MediaAttachmentIdSerializer::class)
@JvmInline
value class MediaAttachmentId(val value: String) {
    companion object {
        fun generate(): MediaAttachmentId = MediaAttachmentId(com.family.tree.core.utils.uuid4())
    }
}

object MediaAttachmentIdSerializer : KSerializer<MediaAttachmentId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MediaAttachmentId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MediaAttachmentId) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): MediaAttachmentId = MediaAttachmentId(decoder.decodeString())
}
