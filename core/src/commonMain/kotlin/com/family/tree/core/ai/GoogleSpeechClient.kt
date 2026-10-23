package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

/**
 * Клиент для работы с Google Cloud Speech-to-Text API (транскрипция аудио).
 */
class GoogleSpeechClient(
    private val httpClient: HttpClient,
    private val json: Json
) : TranscriptionClient {
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
        val isCAF = audioData.hasAscii("caff")
        val isFlac = audioData.hasAscii("fLaC")
        val isM4A = audioData.size >= 12 && audioData.hasAscii("ftyp", 4)
        val isWav = audioData.hasAscii("RIFF")

        // Если это CAF файл (iOS), конвертируем его в WAV
        val (finalData, encoding) = if (isCAF) {
            println("[DEBUG_LOG] GoogleSpeechClient: Converting CAF to WAV format")
            val wavData = convertCafToWav(audioData)
            println("[DEBUG_LOG] GoogleSpeechClient: Converted to WAV (size: ${wavData.size} bytes)")
            wavData to "LINEAR16"
        } else {
            // Google Speech API поддерживает: FLAC, LINEAR16 (WAV), MP3, WEBM_OPUS, OGG_OPUS
            // Для M4A/AAC используем MP3 encoding (работает для большинства случаев)
            val detectedEncoding = when {
                isFlac -> "FLAC"
                isWav -> "LINEAR16"
                isM4A -> "MP3"  // M4A/AAC обрабатывается как MP3
                else -> "MP3"   // Fallback на MP3 для неизвестных форматов
            }
            audioData to detectedEncoding
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
                
                // Выбор модели Google Speech-to-Text v1:
                // default (стандартная и самая надежная модель v1 API), latest_long или latest_short.
                val rawModel = config.googleSpeechModel.ifBlank { "default" }
                // chirp_2 поддерживается только в v2 API, в v1 API используем 'default'
                val speechModel = if (rawModel == "chirp_2") "default" else rawModel
                put("model", speechModel)
                
                // useEnhanced допустим ТОЛЬКО для модели 'default' для поддерживаемых языков.
                if (speechModel == "default") {
                    val supportsEnhanced = languageCode in listOf(
                        "en-US", "en-GB", "en-AU", "en-IN",
                        "es-ES", "fr-FR", "ja-JP", "ko-KR", "pt-BR", "ru-RU", "zh-CN"
                    )
                    if (supportsEnhanced) {
                        put("useEnhanced", true)
                    }
                }
            }
            putJsonObject("audio") {
                put("content", audioBase64)
            }
        }
        
        
        try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            println("[DEBUG_LOG] GoogleSpeechClient: Response status: ${response.status}")
            
            // Explicitly read as bytes and decode as UTF-8 to avoid platform encoding issues
            val responseText = response.bodyAsBytes().decodeToString()
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
        }
    }
    
    /** Конвертирует CAF (Linear PCM 16-bit mono 16kHz, iOS) в WAV. */
    private fun convertCafToWav(cafData: ByteArray): ByteArray {
        val pcm = WavAudio.findCafPcm(cafData) ?: run {
            println("[DEBUG_LOG] GoogleSpeechClient: Failed to find data chunk in CAF, using original data")
            // Если не нашли data chunk, пробуем просто взять данные после заголовка
            val offset = 4096.coerceAtMost(cafData.size / 2)
            PcmRange(offset, cafData.size - offset)
        }
        return WavAudio.fromPcm(cafData, pcm)
    }
}
