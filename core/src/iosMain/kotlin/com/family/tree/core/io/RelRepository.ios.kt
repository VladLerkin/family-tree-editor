package com.family.tree.core.io

import com.family.tree.core.ProjectData
import com.family.tree.core.ProjectMetadata
import com.family.tree.core.layout.ProjectLayout
import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*
import platform.posix.*
import platform.zlib.*

/**
 * iOS implementation of RelRepository using libz for ZIP decompression.
 */
@OptIn(ExperimentalForeignApi::class)
actual class RelRepository {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual fun read(bytes: ByteArray): LoadedProject {
        println("[DEBUG_LOG] RelRepository.read: iOS - Starting to read ${bytes.size} bytes")
        
        // Check ZIP magic number (should be PK\x03\x04 = 0x50 0x4B 0x03 0x04)
        if (bytes.size >= 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            val b2 = bytes[2].toInt() and 0xFF
            val b3 = bytes[3].toInt() and 0xFF
            println("[DEBUG_LOG] RelRepository.read: iOS - First 4 bytes (hex): ${b0.toString(16).uppercase().padStart(2, '0')} ${b1.toString(16).uppercase().padStart(2, '0')} ${b2.toString(16).uppercase().padStart(2, '0')} ${b3.toString(16).uppercase().padStart(2, '0')}")
            println("[DEBUG_LOG] RelRepository.read: iOS - Expected ZIP header: 50 4B 03 04")
            
            // Check if this is NOT a ZIP file (magic number check)
            if (b0 != 0x50 || b1 != 0x4B || b2 != 0x03 || b3 != 0x04) {
                println("[DEBUG_LOG] RelRepository.read: iOS - Not a ZIP file, throwing exception for fallback to RelImporter")
                throw IllegalArgumentException("Not a valid ZIP file (magic number mismatch)")
            }
        }
        
        var data: ProjectData? = null
        var layout: ProjectLayout? = null
        var meta: ProjectMetadata? = null

        try {
            println("[DEBUG_LOG] RelRepository.read: iOS - Using manual ZIP parsing")
            // Parse ZIP manually using libz
            val entries = parseZipManually(bytes)
            
            // Check if ZIP parsing found any entries
            if (entries.isEmpty()) {
                println("[DEBUG_LOG] RelRepository.read: iOS - No entries found in ZIP, throwing exception")
                throw IllegalArgumentException("No valid ZIP entries found")
            }
            
            entries.forEach { (name, content) ->
                println("[DEBUG_LOG] RelRepository.read: iOS - Found entry '$name', size=${content.size} bytes")
                try {
                    when (name) {
                        RelFormat.DATA_JSON -> {
                            println("[DEBUG_LOG] RelRepository.read: iOS - Parsing data.json...")
                            data = json.decodeFromString<ProjectData>(content.decodeToString())
                            println("[DEBUG_LOG] RelRepository.read: iOS - data.json parsed - individuals=${data?.individuals?.size}, families=${data?.families?.size}")
                        }
                        RelFormat.LAYOUT_JSON -> {
                            println("[DEBUG_LOG] RelRepository.read: iOS - Parsing layout.json...")
                            layout = json.decodeFromString<ProjectLayout>(content.decodeToString())
                            println("[DEBUG_LOG] RelRepository.read: iOS - layout.json parsed")
                        }
                        RelFormat.META_JSON -> {
                            println("[DEBUG_LOG] RelRepository.read: iOS - Parsing meta.json...")
                            meta = json.decodeFromString<ProjectMetadata>(content.decodeToString())
                            println("[DEBUG_LOG] RelRepository.read: iOS - meta.json parsed")
                        }
                        else -> println("[DEBUG_LOG] RelRepository.read: iOS - Skipping unknown entry '$name'")
                    }
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.read: iOS - ERROR parsing entry '$name': ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.read: iOS - ERROR: ${e.message}")
            e.printStackTrace()
            throw e  // Re-throw to trigger fallback to RelImporter
        }
        
        // Check if we successfully loaded data
        if (data == null) {
            println("[DEBUG_LOG] RelRepository.read: iOS - No data.json found or parsed, throwing exception")
            throw IllegalArgumentException("No valid data.json found in ZIP")
        }

        val result = LoadedProject(
            data = data,
            layout = layout,
            meta = meta
        )
        println("[DEBUG_LOG] RelRepository.read: iOS - Returning LoadedProject with ${result.data.individuals.size} individuals, ${result.data.families.size} families")
        return result
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun parseZipManually(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        
        println("[DEBUG_LOG] RelRepository.parseZipManually: Starting to parse ${bytes.size} bytes")
        
        // Find End of Central Directory Record (EOCD) by searching backwards from the end
        // EOCD signature: 0x06054b50
        var eocdOffset = -1
        for (i in bytes.size - 22 downTo 0) {
            if (bytes[i].toInt() and 0xFF == 0x50 &&
                bytes[i + 1].toInt() and 0xFF == 0x4b &&
                bytes[i + 2].toInt() and 0xFF == 0x05 &&
                bytes[i + 3].toInt() and 0xFF == 0x06) {
                eocdOffset = i
                println("[DEBUG_LOG] RelRepository.parseZipManually: Found EOCD at offset $eocdOffset")
                break
            }
        }
        
        if (eocdOffset == -1) {
            println("[DEBUG_LOG] RelRepository.parseZipManually: EOCD not found")
            return entries
        }
        
        // Read central directory offset from EOCD (offset + 16, 4 bytes, little-endian)
        val centralDirOffset = ((bytes[eocdOffset + 19].toInt() and 0xFF) shl 24) or
                              ((bytes[eocdOffset + 18].toInt() and 0xFF) shl 16) or
                              ((bytes[eocdOffset + 17].toInt() and 0xFF) shl 8) or
                              (bytes[eocdOffset + 16].toInt() and 0xFF)
        println("[DEBUG_LOG] RelRepository.parseZipManually: Central directory offset = $centralDirOffset")
        
        // Read number of entries from EOCD (offset + 10, 2 bytes, little-endian)
        val numEntries = ((bytes[eocdOffset + 11].toInt() and 0xFF) shl 8) or
                        (bytes[eocdOffset + 10].toInt() and 0xFF)
        println("[DEBUG_LOG] RelRepository.parseZipManually: Number of entries = $numEntries")
        
        // Parse central directory entries
        var cdOffset = centralDirOffset
        repeat(numEntries) {
            // Check for central directory file header signature: 0x02014b50
            if (cdOffset + 46 > bytes.size) {
                println("[DEBUG_LOG] RelRepository.parseZipManually: Not enough bytes for central directory header")
                return@repeat
            }
            
            if (bytes[cdOffset].toInt() and 0xFF == 0x50 &&
                bytes[cdOffset + 1].toInt() and 0xFF == 0x4b &&
                bytes[cdOffset + 2].toInt() and 0xFF == 0x01 &&
                bytes[cdOffset + 3].toInt() and 0xFF == 0x02) {
                
                println("[DEBUG_LOG] RelRepository.parseZipManually: Found central directory header at offset $cdOffset")
                
                // Read compression method (offset + 10, 2 bytes)
                val compressionMethod = ((bytes[cdOffset + 11].toInt() and 0xFF) shl 8) or (bytes[cdOffset + 10].toInt() and 0xFF)
                
                // Read compressed size (offset + 20, 4 bytes, little-endian)
                val compressedSize = ((bytes[cdOffset + 23].toInt() and 0xFF) shl 24) or
                                   ((bytes[cdOffset + 22].toInt() and 0xFF) shl 16) or
                                   ((bytes[cdOffset + 21].toInt() and 0xFF) shl 8) or
                                   (bytes[cdOffset + 20].toInt() and 0xFF)
                
                // Read uncompressed size (offset + 24, 4 bytes, little-endian)
                val uncompressedSize = ((bytes[cdOffset + 27].toInt() and 0xFF) shl 24) or
                                      ((bytes[cdOffset + 26].toInt() and 0xFF) shl 16) or
                                      ((bytes[cdOffset + 25].toInt() and 0xFF) shl 8) or
                                      (bytes[cdOffset + 24].toInt() and 0xFF)
                
                // Read filename length (offset + 28, 2 bytes, little-endian)
                val filenameLength = ((bytes[cdOffset + 29].toInt() and 0xFF) shl 8) or (bytes[cdOffset + 28].toInt() and 0xFF)
                
                // Read extra field length (offset + 30, 2 bytes, little-endian)
                val extraLength = ((bytes[cdOffset + 31].toInt() and 0xFF) shl 8) or (bytes[cdOffset + 30].toInt() and 0xFF)
                
                // Read file comment length (offset + 32, 2 bytes, little-endian)
                val commentLength = ((bytes[cdOffset + 33].toInt() and 0xFF) shl 8) or (bytes[cdOffset + 32].toInt() and 0xFF)
                
                // Read local header offset (offset + 42, 4 bytes, little-endian)
                val localHeaderOffset = ((bytes[cdOffset + 45].toInt() and 0xFF) shl 24) or
                                       ((bytes[cdOffset + 44].toInt() and 0xFF) shl 16) or
                                       ((bytes[cdOffset + 43].toInt() and 0xFF) shl 8) or
                                       (bytes[cdOffset + 42].toInt() and 0xFF)
                
                // Read filename
                val filenameStart = cdOffset + 46
                if (filenameStart + filenameLength > bytes.size) {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Filename extends beyond array bounds")
                    return@repeat
                }
                
                val filename = try {
                    bytes.sliceArray(filenameStart until filenameStart + filenameLength).decodeToString()
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Error decoding filename: ${e.message}")
                    cdOffset += 46 + filenameLength + extraLength + commentLength
                    return@repeat
                }
                
                println("[DEBUG_LOG] RelRepository.parseZipManually: filename='$filename', compressionMethod=$compressionMethod, compressedSize=$compressedSize, uncompressedSize=$uncompressedSize, localHeaderOffset=$localHeaderOffset")
                
                // Now read the actual file data from local file header
                val lfhOffset = localHeaderOffset
                if (lfhOffset + 30 > bytes.size) {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Local file header beyond array bounds")
                    cdOffset += 46 + filenameLength + extraLength + commentLength
                    return@repeat
                }
                
                // Read local header filename and extra field lengths to skip them
                val lfhFilenameLength = ((bytes[lfhOffset + 27].toInt() and 0xFF) shl 8) or (bytes[lfhOffset + 26].toInt() and 0xFF)
                val lfhExtraLength = ((bytes[lfhOffset + 29].toInt() and 0xFF) shl 8) or (bytes[lfhOffset + 28].toInt() and 0xFF)
                
                val dataStart = lfhOffset + 30 + lfhFilenameLength + lfhExtraLength
                println("[DEBUG_LOG] RelRepository.parseZipManually: dataStart=$dataStart, need ${dataStart + compressedSize}, have ${bytes.size}")
                
                if (dataStart + compressedSize > bytes.size) {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Compressed data extends beyond array bounds")
                    cdOffset += 46 + filenameLength + extraLength + commentLength
                    return@repeat
                }
                
                val compressedData = try {
                    bytes.sliceArray(dataStart until dataStart + compressedSize)
                } catch (e: Exception) {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Error slicing compressed data: ${e.message}")
                    cdOffset += 46 + filenameLength + extraLength + commentLength
                    return@repeat
                }
                
                // Decompress if needed
                val decompressedData = if (compressionMethod == 8) {
                    // DEFLATE compression
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Decompressing with DEFLATE")
                    decompressDeflate(compressedData, uncompressedSize)
                } else if (compressionMethod == 0) {
                    // No compression
                    println("[DEBUG_LOG] RelRepository.parseZipManually: No compression (stored)")
                    compressedData
                } else {
                    println("[DEBUG_LOG] RelRepository.parseZipManually: Unknown compression method: $compressionMethod")
                    compressedData
                }
                
                println("[DEBUG_LOG] RelRepository.parseZipManually: Decompressed data size = ${decompressedData.size}")
                entries[filename] = decompressedData
                
                // Move to next central directory entry
                cdOffset += 46 + filenameLength + extraLength + commentLength
            } else {
                println("[DEBUG_LOG] RelRepository.parseZipManually: Invalid central directory signature at offset $cdOffset")
                return@repeat
            }
        }
        
        println("[DEBUG_LOG] RelRepository.parseZipManually: Finished parsing, found ${entries.size} entries")
        return entries
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun decompressDeflate(compressed: ByteArray, uncompressedSize: Int): ByteArray {
        return memScoped {
            val destBuffer = ByteArray(uncompressedSize)
            
            compressed.usePinned { compressedPinned ->
                destBuffer.usePinned { destPinned ->
                    val stream = alloc<z_stream>()
                    memset(stream.ptr, 0, sizeOf<z_stream>().toULong())
                    
                    stream.next_in = compressedPinned.addressOf(0).reinterpret()
                    stream.avail_in = compressed.size.toUInt()
                    stream.next_out = destPinned.addressOf(0).reinterpret()
                    stream.avail_out = uncompressedSize.toUInt()
                    
                    // Initialize with negative windowBits for raw DEFLATE (no zlib header)
                    val initResult = inflateInit2_(stream.ptr, -15, ZLIB_VERSION, sizeOf<z_stream>().toInt())
                    if (initResult != Z_OK) {
                        println("[DEBUG_LOG] RelRepository.read: iOS - inflateInit2 failed: $initResult")
                        return compressed
                    }
                    
                    val inflateResult = inflate(stream.ptr, Z_FINISH)
                    if (inflateResult != Z_STREAM_END) {
                        println("[DEBUG_LOG] RelRepository.read: iOS - inflate failed: $inflateResult")
                        inflateEnd(stream.ptr)
                        return compressed
                    }
                    
                    inflateEnd(stream.ptr)
                }
            }
            
            destBuffer
        }
    }

    actual fun write(data: ProjectData, layout: ProjectLayout?, meta: ProjectMetadata?): ByteArray {
        println("[DEBUG_LOG] RelRepository.write: iOS - Starting to write ZIP")
        
        try {
            // Prepare entries to write
            val entries = mutableListOf<Pair<String, ByteArray>>()
            
            // Add data.json
            val dataJson = json.encodeToString(data)
            entries.add(RelFormat.DATA_JSON to dataJson.encodeToByteArray())
            println("[DEBUG_LOG] RelRepository.write: iOS - Added data.json (${dataJson.length} chars)")
            
            // Add layout.json if present
            if (layout != null) {
                val layoutJson = json.encodeToString(layout)
                entries.add(RelFormat.LAYOUT_JSON to layoutJson.encodeToByteArray())
                println("[DEBUG_LOG] RelRepository.write: iOS - Added layout.json (${layoutJson.length} chars)")
            }
            
            // Add meta.json if present
            if (meta != null) {
                val metaJson = json.encodeToString(meta)
                entries.add(RelFormat.META_JSON to metaJson.encodeToByteArray())
                println("[DEBUG_LOG] RelRepository.write: iOS - Added meta.json (${metaJson.length} chars)")
            }
            
            // Create ZIP manually
            val zipBytes = createZipManually(entries)
            println("[DEBUG_LOG] RelRepository.write: iOS - Created ZIP of ${zipBytes.size} bytes")
            return zipBytes
        } catch (e: Exception) {
            println("[DEBUG_LOG] RelRepository.write: iOS - ERROR: ${e.message}")
            e.printStackTrace()
            return ByteArray(0)
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun createZipManually(entries: List<Pair<String, ByteArray>>): ByteArray {
        println("[DEBUG_LOG] RelRepository.createZipManually: Creating ZIP with ${entries.size} entries")
        
        val localHeaders = mutableListOf<ByteArray>()
        val centralHeaders = mutableListOf<ByteArray>()
        var localHeadersSize = 0
        var currentOffset = 0
        
        entries.forEach { (filename, content) ->
            println("[DEBUG_LOG] RelRepository.createZipManually: Processing '$filename', size=${content.size}")
            
            // Compress content using DEFLATE
            val compressed = compressDeflate(content)
            println("[DEBUG_LOG] RelRepository.createZipManually: Compressed to ${compressed.size} bytes")
            
            val filenameBytes = filename.encodeToByteArray()
            
            // Create local file header
            val localHeader = ByteArray(30 + filenameBytes.size + compressed.size)
            var pos = 0
            
            // Local file header signature: 0x04034b50
            localHeader[pos++] = 0x50.toByte()
            localHeader[pos++] = 0x4b.toByte()
            localHeader[pos++] = 0x03.toByte()
            localHeader[pos++] = 0x04.toByte()
            
            // Version needed to extract: 2.0
            localHeader[pos++] = 0x14.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // General purpose bit flag
            localHeader[pos++] = 0x00.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // Compression method: 8 (DEFLATE)
            localHeader[pos++] = 0x08.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // Last mod file time (DOS format)
            localHeader[pos++] = 0x00.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // Last mod file date (DOS format)
            localHeader[pos++] = 0x21.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // CRC-32
            val crc = calculateCRC32(content)
            localHeader[pos++] = (crc and 0xFF).toByte()
            localHeader[pos++] = ((crc shr 8) and 0xFF).toByte()
            localHeader[pos++] = ((crc shr 16) and 0xFF).toByte()
            localHeader[pos++] = ((crc shr 24) and 0xFF).toByte()
            
            // Compressed size
            localHeader[pos++] = (compressed.size and 0xFF).toByte()
            localHeader[pos++] = ((compressed.size shr 8) and 0xFF).toByte()
            localHeader[pos++] = ((compressed.size shr 16) and 0xFF).toByte()
            localHeader[pos++] = ((compressed.size shr 24) and 0xFF).toByte()
            
            // Uncompressed size
            localHeader[pos++] = (content.size and 0xFF).toByte()
            localHeader[pos++] = ((content.size shr 8) and 0xFF).toByte()
            localHeader[pos++] = ((content.size shr 16) and 0xFF).toByte()
            localHeader[pos++] = ((content.size shr 24) and 0xFF).toByte()
            
            // Filename length
            localHeader[pos++] = (filenameBytes.size and 0xFF).toByte()
            localHeader[pos++] = ((filenameBytes.size shr 8) and 0xFF).toByte()
            
            // Extra field length
            localHeader[pos++] = 0x00.toByte()
            localHeader[pos++] = 0x00.toByte()
            
            // Filename
            filenameBytes.copyInto(localHeader, pos)
            pos += filenameBytes.size
            
            // Compressed data
            compressed.copyInto(localHeader, pos)
            
            localHeaders.add(localHeader)
            localHeadersSize += localHeader.size
            
            // Create central directory header
            val centralHeader = ByteArray(46 + filenameBytes.size)
            var cpos = 0
            
            // Central directory file header signature: 0x02014b50
            centralHeader[cpos++] = 0x50.toByte()
            centralHeader[cpos++] = 0x4b.toByte()
            centralHeader[cpos++] = 0x01.toByte()
            centralHeader[cpos++] = 0x02.toByte()
            
            // Version made by: 2.0
            centralHeader[cpos++] = 0x14.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Version needed to extract: 2.0
            centralHeader[cpos++] = 0x14.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // General purpose bit flag
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Compression method: 8 (DEFLATE)
            centralHeader[cpos++] = 0x08.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Last mod file time
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Last mod file date
            centralHeader[cpos++] = 0x21.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // CRC-32
            centralHeader[cpos++] = (crc and 0xFF).toByte()
            centralHeader[cpos++] = ((crc shr 8) and 0xFF).toByte()
            centralHeader[cpos++] = ((crc shr 16) and 0xFF).toByte()
            centralHeader[cpos++] = ((crc shr 24) and 0xFF).toByte()
            
            // Compressed size
            centralHeader[cpos++] = (compressed.size and 0xFF).toByte()
            centralHeader[cpos++] = ((compressed.size shr 8) and 0xFF).toByte()
            centralHeader[cpos++] = ((compressed.size shr 16) and 0xFF).toByte()
            centralHeader[cpos++] = ((compressed.size shr 24) and 0xFF).toByte()
            
            // Uncompressed size
            centralHeader[cpos++] = (content.size and 0xFF).toByte()
            centralHeader[cpos++] = ((content.size shr 8) and 0xFF).toByte()
            centralHeader[cpos++] = ((content.size shr 16) and 0xFF).toByte()
            centralHeader[cpos++] = ((content.size shr 24) and 0xFF).toByte()
            
            // Filename length
            centralHeader[cpos++] = (filenameBytes.size and 0xFF).toByte()
            centralHeader[cpos++] = ((filenameBytes.size shr 8) and 0xFF).toByte()
            
            // Extra field length
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // File comment length
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Disk number start
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Internal file attributes
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // External file attributes
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            centralHeader[cpos++] = 0x00.toByte()
            
            // Relative offset of local header
            centralHeader[cpos++] = (currentOffset and 0xFF).toByte()
            centralHeader[cpos++] = ((currentOffset shr 8) and 0xFF).toByte()
            centralHeader[cpos++] = ((currentOffset shr 16) and 0xFF).toByte()
            centralHeader[cpos++] = ((currentOffset shr 24) and 0xFF).toByte()
            
            // Filename
            filenameBytes.copyInto(centralHeader, cpos)
            
            centralHeaders.add(centralHeader)
            currentOffset += localHeader.size
        }
        
        // Calculate central directory size
        val centralDirSize = centralHeaders.sumOf { it.size }
        
        // Create End of Central Directory Record
        val eocd = ByteArray(22)
        var epos = 0
        
        // EOCD signature: 0x06054b50
        eocd[epos++] = 0x50.toByte()
        eocd[epos++] = 0x4b.toByte()
        eocd[epos++] = 0x05.toByte()
        eocd[epos++] = 0x06.toByte()
        
        // Number of this disk
        eocd[epos++] = 0x00.toByte()
        eocd[epos++] = 0x00.toByte()
        
        // Disk where central directory starts
        eocd[epos++] = 0x00.toByte()
        eocd[epos++] = 0x00.toByte()
        
        // Number of central directory records on this disk
        eocd[epos++] = (entries.size and 0xFF).toByte()
        eocd[epos++] = ((entries.size shr 8) and 0xFF).toByte()
        
        // Total number of central directory records
        eocd[epos++] = (entries.size and 0xFF).toByte()
        eocd[epos++] = ((entries.size shr 8) and 0xFF).toByte()
        
        // Size of central directory
        eocd[epos++] = (centralDirSize and 0xFF).toByte()
        eocd[epos++] = ((centralDirSize shr 8) and 0xFF).toByte()
        eocd[epos++] = ((centralDirSize shr 16) and 0xFF).toByte()
        eocd[epos++] = ((centralDirSize shr 24) and 0xFF).toByte()
        
        // Offset of start of central directory
        eocd[epos++] = (localHeadersSize and 0xFF).toByte()
        eocd[epos++] = ((localHeadersSize shr 8) and 0xFF).toByte()
        eocd[epos++] = ((localHeadersSize shr 16) and 0xFF).toByte()
        eocd[epos++] = ((localHeadersSize shr 24) and 0xFF).toByte()
        
        // ZIP file comment length
        eocd[epos++] = 0x00.toByte()
        eocd[epos++] = 0x00.toByte()
        
        // Combine all parts
        val totalSize = localHeadersSize + centralDirSize + eocd.size
        val result = ByteArray(totalSize)
        var offset = 0
        
        // Write local headers
        localHeaders.forEach { header ->
            header.copyInto(result, offset)
            offset += header.size
        }
        
        // Write central directory headers
        centralHeaders.forEach { header ->
            header.copyInto(result, offset)
            offset += header.size
        }
        
        // Write EOCD
        eocd.copyInto(result, offset)
        
        println("[DEBUG_LOG] RelRepository.createZipManually: Created ZIP of $totalSize bytes")
        return result
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun compressDeflate(data: ByteArray): ByteArray {
        return memScoped {
            // Allocate buffer for compressed data (worst case: slightly larger than input)
            val maxCompressedSize = data.size + (data.size / 1000) + 12 + 6
            val compressedBuffer = ByteArray(maxCompressedSize)
            
            data.usePinned { dataPinned ->
                compressedBuffer.usePinned { compressedPinned ->
                    val stream = alloc<z_stream>()
                    memset(stream.ptr, 0, sizeOf<z_stream>().toULong())
                    
                    stream.next_in = dataPinned.addressOf(0).reinterpret()
                    stream.avail_in = data.size.toUInt()
                    stream.next_out = compressedPinned.addressOf(0).reinterpret()
                    stream.avail_out = maxCompressedSize.toUInt()
                    
                    // Initialize with negative windowBits for raw DEFLATE (no zlib header)
                    val initResult = deflateInit2_(
                        stream.ptr,
                        Z_DEFAULT_COMPRESSION,
                        Z_DEFLATED,
                        -15, // negative for raw DEFLATE
                        8,
                        Z_DEFAULT_STRATEGY,
                        ZLIB_VERSION,
                        sizeOf<z_stream>().toInt()
                    )
                    
                    if (initResult != Z_OK) {
                        println("[DEBUG_LOG] RelRepository.compressDeflate: deflateInit2 failed: $initResult")
                        return data // Return uncompressed on error
                    }
                    
                    val deflateResult = deflate(stream.ptr, Z_FINISH)
                    val compressedSize = stream.total_out.toInt()
                    
                    deflateEnd(stream.ptr)
                    
                    if (deflateResult != Z_STREAM_END) {
                        println("[DEBUG_LOG] RelRepository.compressDeflate: deflate failed: $deflateResult")
                        return data // Return uncompressed on error
                    }
                    
                    println("[DEBUG_LOG] RelRepository.compressDeflate: Compressed ${data.size} -> $compressedSize bytes")
                    return compressedBuffer.copyOf(compressedSize)
                }
            }
        }
    }
    
    private fun calculateCRC32(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        
        for (byte in data) {
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) {
                crc = if ((crc and 1) != 0) {
                    (crc ushr 1) xor 0xEDB88320.toInt()
                } else {
                    crc ushr 1
                }
            }
        }
        
        return crc xor 0xFFFFFFFF.toInt()
    }
}
