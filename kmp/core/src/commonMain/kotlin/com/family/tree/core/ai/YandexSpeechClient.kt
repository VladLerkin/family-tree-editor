package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Клиент для работы с Yandex SpeechKit API (транскрипция аудио).
 * 
 * Документация: https://cloud.yandex.ru/docs/speechkit/stt/api/streaming-api
 * Поддерживаемые форматы: LPCM, OggOpus
 * Поддерживаемые языки: ru-RU, en-US, tr-TR и другие
 */
class YandexSpeechClient : TranscriptionClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Преобразует ISO-639-1 код языка в формат Yandex SpeechKit.
     * 
     * Поддерживаемые языки:
     * https://cloud.yandex.ru/docs/speechkit/stt/models
     */
    private fun convertToYandexLanguageCode(isoCode: String): String {
        if (isoCode.isBlank()) {
            return "ru-RU"  // По умолчанию русский
        }
        
        return when (isoCode.lowercase()) {
            "ru" -> "ru-RU"  // Русский
            "en" -> "en-US"  // Английский
            "tr" -> "tr-TR"  // Турецкий
            "uz" -> "uz-UZ"  // Узбекский
            "kk" -> "kk-KZ"  // Казахский
            "de" -> "de-DE"  // Немецкий
            "fr" -> "fr-FR"  // Французский
            "es" -> "es-ES"  // Испанский
            "it" -> "it-IT"  // Итальянский
            "pl" -> "pl-PL"  // Польский
            "nl" -> "nl-NL"  // Нидерландский
            "he" -> "he-IL"  // Иврит
            "sv" -> "sv-SE"  // Шведский
            "fi" -> "fi-FI"  // Финский
            "pt" -> "pt-PT"  // Португальский
            "hy" -> "hy-AM"  // Армянский
            "ka" -> "ka-GE"  // Грузинский
            "ar" -> "ar-AE"  // Арабский
            "fa" -> "fa-IR"  // Персидский
            "uk" -> "uk-UA"  // Украинский
            "be" -> "be-BY"  // Белорусский
            
            // Если формат уже содержит дефис, возвращаем как есть
            else -> if (isoCode.contains("-")) {
                isoCode
            } else {
                // Для неизвестных языков пытаемся построить код
                "${isoCode.lowercase()}-${isoCode.uppercase()}"
            }
        }
    }
    
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        val apiKey = config.getApiKeyForTranscription()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Yandex Cloud API key is required for Yandex SpeechKit")
        }
        
        // Yandex SpeechKit REST API v3 endpoint
        val url = "https://stt.api.cloud.yandex.net/speech/v1/stt:recognize"
        
        // Преобразуем ISO-639-1 код в формат Yandex
        val languageCode = convertToYandexLanguageCode(config.language)
        
        println("[DEBUG_LOG] YandexSpeechClient: Converting '${config.language}' -> '$languageCode'")
        
        // Определяем формат аудио
        val isOgg = audioData.size >= 4 && 
                    audioData[0] == 0x4F.toByte() &&  // 'O'
                    audioData[1] == 0x67.toByte() &&  // 'g'
                    audioData[2] == 0x67.toByte() &&  // 'g'
                    audioData[3] == 0x53.toByte()     // 'S'
        
        val isM4A = audioData.size >= 12 && 
                    audioData[4] == 0x66.toByte() &&  // 'f'
                    audioData[5] == 0x74.toByte() &&  // 't'
                    audioData[6] == 0x79.toByte() &&  // 'y'
                    audioData[7] == 0x70.toByte()     // 'p'
        
        val isWav = audioData.size >= 44 && 
                    audioData[0] == 'R'.code.toByte() && 
                    audioData[1] == 'I'.code.toByte() && 
                    audioData[2] == 'F'.code.toByte() && 
                    audioData[3] == 'F'.code.toByte()
        
        // Yandex SpeechKit поддерживает: LPCM, OggOpus
        // Для M4A/AAC конвертируем в OggOpus или используем как есть (но это не сработает для v1)
        // Для WAV удаляем заголовок, так как Yandex требует raw LPCM
        val (finalAudioData, audioFormat) = when {
            isOgg -> audioData to "oggopus"
            isWav -> {
                // Удаляем WAV заголовок (обычно 44 байта)
                println("[DEBUG_LOG] YandexSpeechClient: Detected WAV header, stripping it for LPCM")
                audioData.copyOfRange(44, audioData.size) to "lpcm"
            }
            isM4A -> {
                println("[DEBUG_LOG] YandexSpeechClient: Warning: M4A format detected but Yandex requires LPCM/OggOpus. Sending as is (might fail).")
                audioData to "lpcm"
            }
            else -> audioData to "lpcm"   // По умолчанию LPCM
        }
        
        println("[DEBUG_LOG] YandexSpeechClient: Detected audio format: $audioFormat (isOgg=$isOgg, isM4A=$isM4A)")
        
        val client = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000 // 120 seconds
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
            }
        }
        
        try {
            val response = client.post(url) {
                header("Authorization", "Api-Key $apiKey")
                
                contentType(ContentType.Application.OctetStream)
                
                // Параметры распознавания в query string
                parameter("lang", languageCode)
                parameter("format", audioFormat)
                parameter("sampleRateHertz", "16000")
                parameter("profanityFilter", "false")
                parameter("model", "general")  // Общая модель
                
                setBody(finalAudioData)
            }
            
            println("[DEBUG_LOG] YandexSpeechClient: Response status: ${response.status}")
            
            val responseText = response.bodyAsText()
            println("[DEBUG_LOG] YandexSpeechClient: Full response body: $responseText")
            
            // Проверяем на ошибки
            if (!response.status.isSuccess()) {
                println("[DEBUG_LOG] YandexSpeechClient: HTTP error ${response.status}")
                throw Exception("Yandex SpeechKit API error: ${response.status} - $responseText")
            }
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Проверяем наличие ошибки в JSON
            val error = responseJson["error_code"]?.jsonPrimitive?.content
            if (error != null) {
                val errorMessage = responseJson["error_message"]?.jsonPrimitive?.content ?: "Unknown error"
                println("[DEBUG_LOG] YandexSpeechClient: API error: $error - $errorMessage")
                throw Exception("Yandex SpeechKit API error ($error): $errorMessage")
            }
            
            // Извлекаем транскрибированный текст
            // Структура ответа: { "result": "транскрибированный текст" }
            val result = responseJson["result"]?.jsonPrimitive?.content
            
            if (result.isNullOrBlank()) {
                println("[DEBUG_LOG] YandexSpeechClient: No result in response. Full response: $responseText")
                throw Exception("No transcription result from Yandex SpeechKit API")
            }
            
            println("[DEBUG_LOG] YandexSpeechClient: Transcribed text: $result")
            
            return result
        } catch (e: Exception) {
            println("[DEBUG_LOG] YandexSpeechClient: Error during transcription: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            client.close()
        }
    }
}
