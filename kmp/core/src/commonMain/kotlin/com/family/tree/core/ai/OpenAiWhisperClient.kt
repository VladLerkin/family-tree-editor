package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с OpenAI Whisper API (транскрипция аудио).
 */
class OpenAiWhisperClient : TranscriptionClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        val apiKey = config.getApiKeyForTranscription()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("OpenAI API key is required for transcription")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://api.openai.com/v1" }
        val url = "$baseUrl/audio/transcriptions"
        
        // Определяем формат аудио по сигнатуре данных
        // .caf файлы начинаются с "caff" (0x63616666)
        // .amr файлы начинаются с "#!AMR" (0x2321414D52) для AMR-NB или "#!AMR-WB" для AMR-WB
        // .m4a файлы содержат "ftyp" на позиции 4-8
        val isCAF = audioData.size >= 4 && 
                    audioData[0] == 0x63.toByte() && 
                    audioData[1] == 0x61.toByte() && 
                    audioData[2] == 0x66.toByte() && 
                    audioData[3] == 0x66.toByte()
        
        val isAMR = audioData.size >= 6 && 
                    audioData[0] == 0x23.toByte() &&  // '#'
                    audioData[1] == 0x21.toByte() &&  // '!'
                    audioData[2] == 0x41.toByte() &&  // 'A'
                    audioData[3] == 0x4D.toByte() &&  // 'M'
                    audioData[4] == 0x52.toByte()     // 'R'
        
        // Если это CAF или AMR файл, конвертируем его в WAV для Whisper API
        val (finalData, contentType, fileName) = when {
            isCAF -> {
                println("[DEBUG_LOG] OpenAiWhisperClient: Converting CAF to WAV format")
                val wavData = convertCafToWav(audioData)
                println("[DEBUG_LOG] OpenAiWhisperClient: Converted to WAV (size: ${wavData.size} bytes)")
                Triple(wavData, "audio/wav", "audio.wav")
            }
            isAMR -> {
                println("[DEBUG_LOG] OpenAiWhisperClient: Detected AMR format, not supported by Whisper API")
                // AMR формат не поддерживается Whisper API напрямую
                // Отправляем как есть с правильным MIME типом, чтобы получить понятную ошибку
                // В будущем можно добавить конвертацию AMR -> WAV через FFmpeg
                throw IllegalArgumentException("AMR audio format is not supported by OpenAI Whisper API. Please use a different recording format (WAV, M4A, MP3, etc.) or switch to Google Speech-to-Text transcription provider.")
            }
            else -> {
                Triple(audioData, "audio/m4a", "audio.m4a")
            }
        }
        
        println("[DEBUG_LOG] OpenAiWhisperClient: Sending audio format: $contentType (size: ${finalData.size} bytes)")
        
        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000 // 120 seconds for audio processing
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        
        try {
            val response = client.submitFormWithBinaryData(
                url = url,
                formData = formData {
                    append("file", finalData, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("model", "whisper-1")
                    // Добавляем параметр language если он указан
                    if (config.language.isNotBlank()) {
                        append("language", config.language)
                        println("[DEBUG_LOG] OpenAiWhisperClient: Using language parameter: ${config.language}")
                    }
                }
            ) {
                header("Authorization", "Bearer $apiKey")
            }
            
            println("[DEBUG_LOG] OpenAiWhisperClient: Whisper API response status: ${response.status}")
            
            val responseText = response.bodyAsText()
            println("[DEBUG_LOG] OpenAiWhisperClient: Whisper API response body: $responseText")
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Извлекаем транскрибированный текст
            val text = responseJson["text"]?.jsonPrimitive?.content
            
            if (text == null) {
                println("[DEBUG_LOG] OpenAiWhisperClient: No 'text' field in response. Response keys: ${responseJson.keys}")
                throw Exception("No text in Whisper API response")
            }
            
            return text
        } finally {
            client.close()
        }
    }
    
    /**
     * Конвертирует CAF (Linear PCM) в WAV формат.
     * CAF файлы с iOS используют Linear PCM, который легко конвертируется в WAV.
     */
    private fun convertCafToWav(cafData: ByteArray): ByteArray {
        // Ищем начало аудио данных в CAF файле
        // CAF структура: заголовок "caff" + version + flags + chunks
        // Нас интересует chunk "data" который содержит PCM данные
        
        var dataOffset = -1
        var dataSize = 0
        
        // Простой поиск data chunk в CAF файле
        // CAF chunk structure: chunk type (4 bytes) + chunk size (8 bytes) + data
        var i = 8 // Пропускаем "caff" + version + flags
        while (i < cafData.size - 12) {
            // Читаем тип chunk (4 байта)
            val chunkType = cafData.sliceArray(i until i + 4).map { it.toInt().toChar() }.toCharArray().concatToString()
            
            // Читаем размер chunk (8 байт, big-endian long)
            val chunkSize = readBigEndianLong(cafData, i + 4).toInt()
            
            if (chunkType == "data") {
                dataOffset = i + 12 + 4 // После заголовка chunk + 4 байта edit count
                dataSize = chunkSize - 4 // Минус 4 байта edit count
                break
            }
            
            i += 12 + chunkSize
        }
        
        if (dataOffset == -1 || dataSize <= 0) {
            println("[DEBUG_LOG] OpenAiWhisperClient: Failed to find data chunk in CAF, using original data")
            // Если не нашли data chunk, пробуем просто взять данные после заголовка
            dataOffset = 4096.coerceAtMost(cafData.size / 2)
            dataSize = cafData.size - dataOffset
        }
        
        val pcmData = cafData.sliceArray(dataOffset until (dataOffset + dataSize).coerceAtMost(cafData.size))
        
        println("[DEBUG_LOG] OpenAiWhisperClient: Extracted PCM data from CAF: offset=$dataOffset, size=${pcmData.size}")
        
        // Создаем WAV заголовок для Linear PCM 16-bit mono 16kHz
        return createWavFile(pcmData, sampleRate = 16000, channels = 1, bitsPerSample = 16)
    }
    
    /**
     * Читает 64-битное целое число в big-endian формате.
     */
    private fun readBigEndianLong(data: ByteArray, offset: Int): Long {
        return ((data[offset].toLong() and 0xFF) shl 56) or
               ((data[offset + 1].toLong() and 0xFF) shl 48) or
               ((data[offset + 2].toLong() and 0xFF) shl 40) or
               ((data[offset + 3].toLong() and 0xFF) shl 32) or
               ((data[offset + 4].toLong() and 0xFF) shl 24) or
               ((data[offset + 5].toLong() and 0xFF) shl 16) or
               ((data[offset + 6].toLong() and 0xFF) shl 8) or
               (data[offset + 7].toLong() and 0xFF)
    }
    
    /**
     * Создает WAV файл из PCM данных.
     */
    private fun createWavFile(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size
        val fileSize = 36 + dataSize
        
        val wav = ByteArray(44 + dataSize)
        var offset = 0
        
        // RIFF заголовок
        wav[offset++] = 'R'.code.toByte()
        wav[offset++] = 'I'.code.toByte()
        wav[offset++] = 'F'.code.toByte()
        wav[offset++] = 'F'.code.toByte()
        
        // Размер файла - 8
        writeInt32LE(wav, offset, fileSize)
        offset += 4
        
        // WAVE
        wav[offset++] = 'W'.code.toByte()
        wav[offset++] = 'A'.code.toByte()
        wav[offset++] = 'V'.code.toByte()
        wav[offset++] = 'E'.code.toByte()
        
        // fmt chunk
        wav[offset++] = 'f'.code.toByte()
        wav[offset++] = 'm'.code.toByte()
        wav[offset++] = 't'.code.toByte()
        wav[offset++] = ' '.code.toByte()
        
        // fmt chunk size (16 для PCM)
        writeInt32LE(wav, offset, 16)
        offset += 4
        
        // Audio format (1 = PCM)
        writeInt16LE(wav, offset, 1)
        offset += 2
        
        // Количество каналов
        writeInt16LE(wav, offset, channels)
        offset += 2
        
        // Sample rate
        writeInt32LE(wav, offset, sampleRate)
        offset += 4
        
        // Byte rate
        writeInt32LE(wav, offset, byteRate)
        offset += 4
        
        // Block align
        writeInt16LE(wav, offset, blockAlign)
        offset += 2
        
        // Bits per sample
        writeInt16LE(wav, offset, bitsPerSample)
        offset += 2
        
        // data chunk
        wav[offset++] = 'd'.code.toByte()
        wav[offset++] = 'a'.code.toByte()
        wav[offset++] = 't'.code.toByte()
        wav[offset++] = 'a'.code.toByte()
        
        // data chunk size
        writeInt32LE(wav, offset, dataSize)
        offset += 4
        
        // Копируем PCM данные
        pcmData.copyInto(wav, offset)
        
        return wav
    }
    
    private fun writeInt16LE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }
    
    private fun writeInt32LE(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}
