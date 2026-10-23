package com.family.tree.core.ai

/** Location of the raw PCM payload inside a container file. */
internal data class PcmRange(val offset: Int, val size: Int)

/** True if the bytes at [offset] spell [ascii] (e.g. a RIFF/caff/fLaC magic). */
internal fun ByteArray.hasAscii(ascii: String, offset: Int = 0): Boolean {
    if (size < offset + ascii.length) return false
    return ascii.indices.all { this[offset + it] == ascii[it].code.toByte() }
}

/** Helpers for the iOS CAF → WAV conversion shared by the transcription clients. */
internal object WavAudio {
    private const val HEADER_SIZE = 44
    private const val CAF_CHUNK_HEADER_SIZE = 12
    private const val CAF_EDIT_COUNT_SIZE = 4

    /** Finds the `data` chunk of a CAF file, or null if there is none. */
    fun findCafPcm(caf: ByteArray): PcmRange? {
        var i = 8 // skip "caff" + version + flags
        while (i < caf.size - CAF_CHUNK_HEADER_SIZE) {
            val chunkSize = caf.readBigEndianLong(i + 4).toInt()
            if (caf.hasAscii("data", i)) {
                val size = chunkSize - CAF_EDIT_COUNT_SIZE
                return if (size > 0) PcmRange(i + CAF_CHUNK_HEADER_SIZE + CAF_EDIT_COUNT_SIZE, size) else null
            }
            i += CAF_CHUNK_HEADER_SIZE + chunkSize
        }
        return null
    }

    /** Wraps PCM bytes of [source] in a canonical 44-byte WAV header. */
    fun fromPcm(
        source: ByteArray,
        pcm: PcmRange = PcmRange(0, source.size),
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16,
    ): ByteArray {
        val dataSize = pcm.size.coerceAtMost(source.size - pcm.offset)
        val blockAlign = channels * bitsPerSample / 8

        val wav = ByteArray(HEADER_SIZE + dataSize)
        "RIFF".encodeToByteArray().copyInto(wav, 0)
        wav.writeIntLE(4, 36 + dataSize)
        "WAVEfmt ".encodeToByteArray().copyInto(wav, 8)
        wav.writeIntLE(16, 16) // fmt chunk size for PCM
        wav.writeShortLE(20, 1) // audio format: PCM
        wav.writeShortLE(22, channels)
        wav.writeIntLE(24, sampleRate)
        wav.writeIntLE(28, sampleRate * blockAlign)
        wav.writeShortLE(32, blockAlign)
        wav.writeShortLE(34, bitsPerSample)
        "data".encodeToByteArray().copyInto(wav, 36)
        wav.writeIntLE(40, dataSize)
        source.copyInto(wav, HEADER_SIZE, pcm.offset, pcm.offset + dataSize)
        return wav
    }

    private fun ByteArray.readBigEndianLong(offset: Int): Long =
        (0 until 8).fold(0L) { acc, i -> (acc shl 8) or (this[offset + i].toLong() and 0xFF) }

    private fun ByteArray.writeShortLE(offset: Int, value: Int) {
        this[offset] = value.toByte()
        this[offset + 1] = (value shr 8).toByte()
    }

    private fun ByteArray.writeIntLE(offset: Int, value: Int) {
        writeShortLE(offset, value)
        writeShortLE(offset + 2, value shr 16)
    }
}
