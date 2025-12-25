package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.layout.ProjectLayout
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Web (WasmJs) implementation of RelRepository.
 * Uses pako library for DEFLATE decompression (simpler than full ZIP parsing).
 */
actual class RelRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual fun read(bytes: ByteArray): LoadedProject {
        println("[DEBUG_LOG] RelRepository.wasmJs.read: Starting to read ${bytes.size} bytes")
        
        var data: ProjectData? = null
        var layout: ProjectLayout? = null
        var meta: ProjectMetadata? = null
        
        try {
            // Parse ZIP file manually
            val result = parseZipFile(bytes)
            
            // Parse data.json
            result[RelFormat.DATA_JSON]?.let { content ->
                println("[DEBUG_LOG] RelRepository.wasmJs.read: Parsing data.json (${content.length} chars)...")
                try {
                    data = json.decodeFromString<ProjectData>(content)
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: data.json parsed - individuals=${data?.individuals?.size}, families=${data?.families?.size}")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: ERROR parsing data.json: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            // Parse layout.json
            result[RelFormat.LAYOUT_JSON]?.let { content ->
                println("[DEBUG_LOG] RelRepository.wasmJs.read: Parsing layout.json...")
                try {
                    layout = json.decodeFromString<ProjectLayout>(content)
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: layout.json parsed")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: ERROR parsing layout.json: ${e.message}")
                }
            }
            
            // Parse meta.json
            result[RelFormat.META_JSON]?.let { content ->
                println("[DEBUG_LOG] RelRepository.wasmJs.read: Parsing meta.json...")
                try {
                    meta = json.decodeFromString<ProjectMetadata>(content)
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: meta.json parsed")
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.wasmJs.read: ERROR parsing meta.json: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.wasmJs.read: ERROR: ${e.message}")
            e.printStackTrace()
        }
        
        val result = LoadedProject(
            data = data ?: ProjectData(),
            layout = layout,
            meta = meta
        )
        println("[DEBUG_LOG] RelRepository.wasmJs.read: Returning LoadedProject with ${result.data.individuals.size} individuals, ${result.data.families.size} families")
        return result
    }

    actual fun write(data: ProjectData, layout: ProjectLayout?, meta: ProjectMetadata?): ByteArray {
        println("[DEBUG_LOG] RelRepository.wasmJs.write: ZIP writing not implemented yet")
        // TODO: Реализовать создание ZIP архива для веб
        return ByteArray(0)
    }
    
    /**
     * Simple ZIP file parser for reading uncompressed or DEFLATE-compressed entries.
     * Returns a map of filename -> content (as String).
     * Reads from central directory to get correct sizes.
     */
    private fun parseZipFile(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        try {
            // Find End of Central Directory Record (EOCD)
            // Signature: 0x06054b50
            var eocdOffset = -1
            for (i in bytes.size - 22 downTo 0) {
                if (readInt(bytes, i) == 0x06054b50) {
                    eocdOffset = i
                    break
                }
            }
            
            if (eocdOffset == -1) {
                println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: EOCD not found")
                return result
            }
            
            println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Found EOCD at offset $eocdOffset")
            
            // Read central directory offset from EOCD
            val cdOffset = readInt(bytes, eocdOffset + 16)
            println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Central directory at offset $cdOffset")
            
            // Parse central directory entries
            var offset = cdOffset
            while (offset < eocdOffset) {
                val sig = readInt(bytes, offset)
                if (sig != 0x02014b50) {
                    // Not a central directory file header
                    break
                }
                
                offset += 4
                val versionMadeBy = readShort(bytes, offset)
                offset += 2
                val versionNeeded = readShort(bytes, offset)
                offset += 2
                val flags = readShort(bytes, offset)
                offset += 2
                val compressionMethod = readShort(bytes, offset)
                offset += 2
                val modTime = readShort(bytes, offset)
                offset += 2
                val modDate = readShort(bytes, offset)
                offset += 2
                val crc32 = readInt(bytes, offset)
                offset += 4
                val compressedSize = readInt(bytes, offset)
                offset += 4
                val uncompressedSize = readInt(bytes, offset)
                offset += 4
                val fileNameLength = readShort(bytes, offset)
                offset += 2
                val extraFieldLength = readShort(bytes, offset)
                offset += 2
                val fileCommentLength = readShort(bytes, offset)
                offset += 2
                val diskNumberStart = readShort(bytes, offset)
                offset += 2
                val internalFileAttributes = readShort(bytes, offset)
                offset += 2
                val externalFileAttributes = readInt(bytes, offset)
                offset += 4
                val localHeaderOffset = readInt(bytes, offset)
                offset += 4
                
                // Read filename
                val fileName = readString(bytes, offset, fileNameLength)
                offset += fileNameLength
                
                // Skip extra field and comment
                offset += extraFieldLength + fileCommentLength
                
                println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Found entry '$fileName', compressed=$compressedSize, uncompressed=$uncompressedSize, method=$compressionMethod, localOffset=$localHeaderOffset")
                
                // Now read the actual file data from local header
                if (compressedSize > 0) {
                    // Skip local header to get to file data
                    var dataOffset = localHeaderOffset + 30 // Fixed part of local header
                    val localFileNameLength = readShort(bytes, localHeaderOffset + 26)
                    val localExtraFieldLength = readShort(bytes, localHeaderOffset + 28)
                    dataOffset += localFileNameLength + localExtraFieldLength
                    
                    println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Reading data from offset $dataOffset, size $compressedSize")
                    
                    val fileData = bytes.copyOfRange(dataOffset, dataOffset + compressedSize)
                    
                    // Decompress if needed
                    val decompressed = if (compressionMethod == 8) {
                        // DEFLATE compression
                        println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Decompressing DEFLATE data...")
                        decompressDeflate(fileData)
                    } else if (compressionMethod == 0) {
                        // No compression
                        fileData
                    } else {
                        println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Unsupported compression method: $compressionMethod")
                        null
                    }
                    
                    if (decompressed != null) {
                        val content = decompressed.decodeToString()
                        result[fileName] = content
                        println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: Successfully read '$fileName' (${content.length} chars)")
                    }
                }
            }
            
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.wasmJs.parseZipFile: ERROR: ${e.message}")
            e.printStackTrace()
        }
        
        return result
    }
    
    private fun readShort(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }
    
    private fun readInt(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
    
    private fun readString(bytes: ByteArray, offset: Int, length: Int): String {
        return bytes.copyOfRange(offset, offset + length).decodeToString()
    }
    
    /**
     * Decompress DEFLATE-compressed data using pako library.
     */
    private fun decompressDeflate(compressed: ByteArray): ByteArray? {
        return try {
            // Use pako.inflateRaw for raw DEFLATE data (without zlib wrapper)
            val result = inflatePako(compressed)
            println("[DEBUG_LOG] RelRepository.wasmJs.decompressDeflate: Decompressed ${compressed.size} -> ${result.size} bytes")
            result
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.wasmJs.decompressDeflate: ERROR: ${e.message}")
            null
        }
    }
}

/**
 * External function to call pako.inflateRaw from JavaScript.
 * Takes a string representation of bytes and returns decompressed string.
 */
@JsFun("""
(bytesStr) => {
    const bytes = bytesStr.split(',').map(s => parseInt(s) & 0xFF);
    const uint8 = new Uint8Array(bytes);
    const decompressed = pako.inflateRaw(uint8);
    return Array.from(decompressed).join(',');
}
""")
private external fun inflatePakoString(bytesStr: String): String

private fun inflatePako(compressed: ByteArray): ByteArray {
    // Convert ByteArray to comma-separated string
    val bytesStr = compressed.joinToString(",") { (it.toInt() and 0xFF).toString() }
    
    // Call JS function
    val resultStr = inflatePakoString(bytesStr)
    
    // Convert result back to ByteArray
    val parts = resultStr.split(',')
    return ByteArray(parts.size) { parts[it].toInt().toByte() }
}
