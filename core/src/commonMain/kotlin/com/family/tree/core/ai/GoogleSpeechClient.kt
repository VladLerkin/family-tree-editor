package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Клиент для работы с Google Cloud Speech-to-Text API (транскрипция аудио).
 */
class GoogleSpeechClient : TranscriptionClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Преобразует ISO-639-1 код языка (например, "ru") в BCP-47 формат (например, "ru-RU")
     * для Google Speech-to-Text API.
     * 
     * Основано на списке поддерживаемых языков:
     * https://cloud.google.com/speech-to-text/docs/speech-to-text-supported-languages
     */
    private fun convertToGoogleLanguageCode(isoCode: String): String {
        if (isoCode.isBlank()) {
            return "ru-RU"  // По умолчанию русский
        }
        
        return when (isoCode.lowercase()) {
            // Основные европейские языки
            "ru" -> "ru-RU"  // Русский
            "en" -> "en-US"  // Английский
            "de" -> "de-DE"  // Немецкий
            "fr" -> "fr-FR"  // Французский
            "es" -> "es-ES"  // Испанский
            "it" -> "it-IT"  // Итальянский
            "pt" -> "pt-PT"  // Португальский
            "nl" -> "nl-NL"  // Нидерландский
            "pl" -> "pl-PL"  // Польский
            "uk" -> "uk-UA"  // Украинский
            "cs" -> "cs-CZ"  // Чешский
            "sk" -> "sk-SK"  // Словацкий
            "bg" -> "bg-BG"  // Болгарский
            "hr" -> "hr-HR"  // Хорватский
            "ro" -> "ro-RO"  // Румынский
            "sr" -> "sr-RS"  // Сербский
            "sl" -> "sl-SI"  // Словенский
            "da" -> "da-DK"  // Датский
            "sv" -> "sv-SE"  // Шведский
            "no" -> "nb-NO"  // Норвежский (букмол)
            "fi" -> "fi-FI"  // Финский
            "et" -> "et-EE"  // Эстонский
            "lv" -> "lv-LV"  // Латышский
            "lt" -> "lt-LT"  // Литовский
            
            // Кавказские языки
            "ka" -> "ka-GE"  // Грузинский
            "hy" -> "hy-AM"  // Армянский
            "az" -> "az-AZ"  // Азербайджанский
            
            // Азиатские языки
            "zh" -> "zh-CN"  // Китайский (упрощенный)
            "ja" -> "ja-JP"  // Японский
            "ko" -> "ko-KR"  // Корейский
            "hi" -> "hi-IN"  // Хинди
            "th" -> "th-TH"  // Тайский
            "vi" -> "vi-VN"  // Вьетнамский
            "id" -> "id-ID"  // Индонезийский
            "ms" -> "ms-MY"  // Малайский
            "ta" -> "ta-IN"  // Тамильский
            "te" -> "te-IN"  // Телугу
            "bn" -> "bn-IN"  // Бенгальский
            "ur" -> "ur-PK"  // Урду
            
            // Ближний Восток
            "ar" -> "ar-SA"  // Арабский
            "he" -> "he-IL"  // Иврит
            "tr" -> "tr-TR"  // Турецкий
            "fa" -> "fa-IR"  // Персидский
            
            // Африканские языки
            "af" -> "af-ZA"  // Африкаанс
            "zu" -> "zu-ZA"  // Зулу
            "sw" -> "sw-KE"  // Суахили
            
            // Если формат уже BCP-47 (содержит дефис), возвращаем как есть
            else -> if (isoCode.contains("-")) {
                isoCode
            } else {
                // Для неизвестных языков пытаемся построить BCP-47 код
                "${isoCode.lowercase()}-${isoCode.uppercase()}"
            }
        }
    }
    
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        val apiKey = config.getApiKeyForTranscription()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Google Cloud API key is required for Google Speech-to-Text. Please configure it in the AI Settings menu.")
        }
        
        val url = "https://speech.googleapis.com/v1/speech:recognize?key=$apiKey"
        
        // Преобразуем ISO-639-1 код в BCP-47 формат для Google Speech API
        val languageCode = convertToGoogleLanguageCode(config.language)
        
        println("[DEBUG_LOG] GoogleSpeechClient: Converting '${config.language}' -> '$languageCode'")
        
        // Определяем формат аудио по сигнатуре данных
        // CAF файлы начинаются с "caff" (0x63616666)
        // FLAC файлы начинаются с "fLaC" (0x664C6143)
        // M4A/MP4 файлы содержат "ftyp" на позиции 4-8 (после 4-байтового размера заголовка)
        // WAV файлы начинаются с "RIFF"
        val isCAF = audioData.size >= 4 && 
                    audioData[0] == 0x63.toByte() &&  // 'c'
                    audioData[1] == 0x61.toByte() &&  // 'a'
                    audioData[2] == 0x66.toByte() &&  // 'f'
                    audioData[3] == 0x66.toByte()     // 'f'
        
        val isFlac = audioData.size >= 4 && 
                     audioData[0] == 0x66.toByte() &&  // 'f'
                     audioData[1] == 0x4C.toByte() &&  // 'L'
                     audioData[2] == 0x61.toByte() &&  // 'a'
                     audioData[3] == 0x43.toByte()     // 'C'
        
        val isM4A = audioData.size >= 12 && 
                    audioData[4] == 0x66.toByte() &&  // 'f'
                    audioData[5] == 0x74.toByte() &&  // 't'
                    audioData[6] == 0x79.toByte() &&  // 'y'
                    audioData[7] == 0x70.toByte()     // 'p'
        
        val isWav = audioData.size >= 4 && 
                    audioData[0] == 0x52.toByte() &&  // 'R'
                    audioData[1] == 0x49.toByte() &&  // 'I'
                    audioData[2] == 0x46.toByte() &&  // 'F'
                    audioData[3] == 0x46.toByte()     // 'F'
        
        // Если это CAF файл (iOS), конвертируем его в WAV
        val (finalData, encoding) = if (isCAF) {
            println("[DEBUG_LOG] GoogleSpeechClient: Converting CAF to WAV format")
            val wavData = convertCafToWav(audioData)
            println("[DEBUG_LOG] GoogleSpeechClient: Converted to WAV (size: ${wavData.size} bytes)")
            Pair(wavData, "LINEAR16")
        } else {
            // Google Speech API поддерживает: FLAC, LINEAR16 (WAV), MP3, WEBM_OPUS, OGG_OPUS
            // Для M4A/AAC используем MP3 encoding (работает для большинства случаев)
            val detectedEncoding = when {
                isFlac -> "FLAC"
                isWav -> "LINEAR16"
                isM4A -> "MP3"  // M4A/AAC обрабатывается как MP3
                else -> "MP3"   // Fallback на MP3 для неизвестных форматов
            }
            Pair(audioData, detectedEncoding)
        }
        
        println("[DEBUG_LOG] GoogleSpeechClient: Detected audio format: $encoding (isCAF=$isCAF, isFlac=$isFlac, isM4A=$isM4A, isWav=$isWav)")
        
        // Кодируем аудио в Base64
        val audioBase64 = Base64.encode(finalData)
        
        // Формируем JSON запрос для Google Speech API
        val requestBody = buildJsonObject {
            putJsonObject("config") {
                put("encoding", encoding)
                put("sampleRateHertz", 16000)  // 16 kHz
                put("languageCode", languageCode)
                put("enableAutomaticPunctuation", true)
                
                // Параметры для улучшения распознавания длинных фраз с паузами
                put("maxAlternatives", 1)  // Возвращать только лучший результат
                put("profanityFilter", false)  // Не фильтровать нецензурную лексику
                
                // Включаем speaker diarization для лучшего распознавания длинных аудио с паузами
                // Это помогает API не обрезать результаты при паузах в речи
                putJsonObject("diarizationConfig") {
                    put("enableSpeakerDiarization", true)
                    put("minSpeakerCount", 1)
                    put("maxSpeakerCount", 2)
                }
                
                // Enhanced модели (latest_long + useEnhanced) доступны не для всех языков
                // Для ka-GE (грузинский) используем стандартную модель
                // Список языков с enhanced моделями: en-US, en-GB, es-ES, fr-FR, ja-JP, ko-KR, pt-BR, ru-RU, zh-CN и др.
                val supportsEnhanced = languageCode in listOf(
                    "en-US", "en-GB", "en-AU", "en-IN",
                    "es-ES", "fr-FR", "ja-JP", "ko-KR", "pt-BR", "ru-RU", "zh-CN"
                )
                
                if (supportsEnhanced) {
                    put("model", "latest_long")  // Модель с поддержкой длинных аудио
                    put("useEnhanced", true)     // Улучшенная модель для лучшего качества
                }
                // Для остальных языков (включая ka-GE) используется стандартная модель по умолчанию
            }
            putJsonObject("audio") {
                put("content", audioBase64)
            }
        }
        
        println("[DEBUG_LOG] GoogleSpeechClient: Sending audio (size: ${audioData.size} bytes, base64 length: ${audioBase64.length})")
        
        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000 // 120 seconds for audio processing
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        
        try {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            println("[DEBUG_LOG] GoogleSpeechClient: Response status: ${response.status}")
            
            val responseText = response.bodyAsText()
            println("[DEBUG_LOG] GoogleSpeechClient: Full response body: $responseText")
            
            // Проверяем на ошибки в ответе
            if (!response.status.isSuccess()) {
                println("[DEBUG_LOG] GoogleSpeechClient: HTTP error ${response.status}")
                throw Exception("Google Speech API error: ${response.status} - $responseText")
            }
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Проверяем наличие ошибки в JSON ответе
            val error = responseJson["error"]?.jsonObject
            if (error != null) {
                val errorMessage = error["message"]?.jsonPrimitive?.content ?: "Unknown error"
                val errorCode = error["code"]?.jsonPrimitive?.int ?: 0
                println("[DEBUG_LOG] GoogleSpeechClient: API error: $errorCode - $errorMessage")
                throw Exception("Google Speech API error ($errorCode): $errorMessage")
            }
            
            // Извлекаем транскрибированный текст из ответа Google
            // Структура: { "results": [ { "alternatives": [ { "transcript": "text", "confidence": 0.98 } ] } ] }
            val results = responseJson["results"]?.jsonArray
            
            if (results == null || results.isEmpty()) {
                println("[DEBUG_LOG] GoogleSpeechClient: No results in response. Full response: $responseText")
                throw Exception("No transcription results from Google Speech API. The audio format may not be supported or the audio is too short/silent.")
            }
            
            // Собираем все транскрипции из всех результатов
            val transcripts = results.mapNotNull { result ->
                result.jsonObject["alternatives"]?.jsonArray?.firstOrNull()?.jsonObject?.get("transcript")?.jsonPrimitive?.content
            }
            
            if (transcripts.isEmpty()) {
                throw Exception("No transcript text in Google Speech API response")
            }
            
            val fullTranscript = transcripts.joinToString(" ")
            println("[DEBUG_LOG] GoogleSpeechClient: Transcribed text: $fullTranscript")
            
            return fullTranscript
        } catch (e: Exception) {
            println("[DEBUG_LOG] GoogleSpeechClient: Error during transcription: ${e.message}")
            e.printStackTrace()
            throw e
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
            println("[DEBUG_LOG] GoogleSpeechClient: Failed to find data chunk in CAF, using original data")
            // Если не нашли data chunk, пробуем просто взять данные после заголовка
            dataOffset = 4096.coerceAtMost(cafData.size / 2)
            dataSize = cafData.size - dataOffset
        }
        
        val pcmData = cafData.sliceArray(dataOffset until (dataOffset + dataSize).coerceAtMost(cafData.size))
        
        println("[DEBUG_LOG] GoogleSpeechClient: Extracted PCM data from CAF: offset=$dataOffset, size=${pcmData.size}")
        
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
