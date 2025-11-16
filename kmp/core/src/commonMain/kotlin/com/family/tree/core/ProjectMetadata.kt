package com.family.tree.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Minimal project metadata persisted with KMP JSON and prepared for
 * future .rel alignment. Timestamps are epoch millis to avoid extra deps.
 */
@Serializable
data class ProjectMetadata(
    val name: String = "",
    @Serializable(with = FlexibleTimestampSerializer::class)
    val createdAt: Long = 0L,
    @Serializable(with = FlexibleTimestampSerializer::class)
    val modifiedAt: Long = 0L,
    val formatVersion: Int = 1
)

/**
 * Custom serializer that can read both:
 * - Long (epoch millis) from KMP version
 * - Double (seconds.nanos) from JavaFX/Jackson Instant serialization
 */
object FlexibleTimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleTimestamp", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
    
    override fun deserialize(decoder: Decoder): Long {
        return try {
            // First, try as JsonElement to check the actual type
            val jsonDecoder = decoder as? kotlinx.serialization.json.JsonDecoder
            if (jsonDecoder != null) {
                val element = jsonDecoder.decodeJsonElement()
                when {
                    element is kotlinx.serialization.json.JsonPrimitive && element.isString -> 0L
                    element is kotlinx.serialization.json.JsonPrimitive -> {
                        // Try to parse as long first (KMP format)
                        val content = element.content
                        content.toLongOrNull() ?: run {
                            // If not a long, try double and convert (JavaFX format: seconds.nanos)
                            val doubleVal = content.toDoubleOrNull() ?: 0.0
                            (doubleVal * 1000).toLong()
                        }
                    }
                    else -> 0L
                }
            } else {
                // Fallback for non-JSON decoders
                decoder.decodeLong()
            }
        } catch (e: Exception) {
            0L
        }
    }
}