package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Client for Yandex SpeechKit API (audio transcription).
 * 
 * Documentation: https://cloud.yandex.ru/docs/speechkit/stt/api/streaming-api
 * Supported formats: LPCM, OggOpus
 * Supported languages: ru-RU, en-US, tr-TR and others
 */
class YandexSpeechClient : TranscriptionClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Converts ISO-639-1 language code to Yandex SpeechKit format.
     * 
     * Supported languages:
     * https://cloud.yandex.ru/docs/speechkit/stt/models
     */
    private fun convertToYandexLanguageCode(isoCode: String): String {
        if (isoCode.isBlank()) {
            return "ru-RU"  // Russian by default
        }
        
        return when (isoCode.lowercase()) {
            "ru" -> "ru-RU"  // Russian
            "en" -> "en-US"  // English
            "tr" -> "tr-TR"  // Turkish
            "uz" -> "uz-UZ"  // Uzbek
            "kk" -> "kk-KZ"  // Kazakh
            "de" -> "de-DE"  // German
            "fr" -> "fr-FR"  // French
            "es" -> "es-ES"  // Spanish
            "it" -> "it-IT"  // Italian
            "pl" -> "pl-PL"  // Polish
            "nl" -> "nl-NL"  // Dutch
            "he" -> "he-IL"  // Hebrew
            "sv" -> "sv-SE"  // Swedish
            "fi" -> "fi-FI"  // Finnish
            "pt" -> "pt-PT"  // Portuguese
            "hy" -> "hy-AM"  // Armenian
            "ka" -> "ka-GE"  // Georgian
            "ar" -> "ar-AE"  // Arabic
            "fa" -> "fa-IR"  // Persian
            "uk" -> "uk-UA"  // Ukrainian
            "be" -> "be-BY"  // Belarusian
            
            // If format already contains hyphen, return as is
            else -> if (isoCode.contains("-")) {
                isoCode
            } else {
                // For unknown languages try to construct code
                "${isoCode.lowercase()}-${isoCode.uppercase()}"
            }
        }
    }
    
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        val apiKey = config.getApiKeyForTranscription()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Yandex Cloud API key is required for Yandex SpeechKit. Please configure it in the AI Settings menu.")
        }
        
        // Yandex SpeechKit REST API v3 endpoint
        val url = "https://stt.api.cloud.yandex.net/speech/v1/stt:recognize"
        
        // Convert ISO-639-1 code to Yandex format
        val languageCode = convertToYandexLanguageCode(config.language)
        
        println("[DEBUG_LOG] YandexSpeechClient: Converting '${config.language}' -> '$languageCode'")
        
        // Determine audio format
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
        
        // Yandex SpeechKit supports: LPCM, OggOpus
        // For M4A/AAC convert to OggOpus or use as is (but this won't work for v1)
        // For WAV remove header, as Yandex requires raw LPCM
        val (finalAudioData, audioFormat) = when {
            isOgg -> audioData to "oggopus"
            isWav -> {
                // Remove WAV header (usually 44 bytes)
                println("[DEBUG_LOG] YandexSpeechClient: Detected WAV header, stripping it for LPCM")
                audioData.copyOfRange(44, audioData.size) to "lpcm"
            }
            isM4A -> {
                println("[DEBUG_LOG] YandexSpeechClient: Warning: M4A format detected but Yandex requires LPCM/OggOpus. Sending as is (might fail).")
                audioData to "lpcm"
            }
            else -> audioData to "lpcm"   // LPCM by default
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
                
                // Recognition parameters in query string
                parameter("lang", languageCode)
                parameter("format", audioFormat)
                parameter("sampleRateHertz", "16000")
                parameter("profanityFilter", "false")
                parameter("model", "general")  // General model
                
                setBody(finalAudioData)
            }
            
            println("[DEBUG_LOG] YandexSpeechClient: Response status: ${response.status}")
            
            val responseText = response.bodyAsText()
            println("[DEBUG_LOG] YandexSpeechClient: Full response body: $responseText")
            
            // Check for errors
            if (!response.status.isSuccess()) {
                println("[DEBUG_LOG] YandexSpeechClient: HTTP error ${response.status}")
                throw Exception("Yandex SpeechKit API error: ${response.status} - $responseText")
            }
            
            val responseJson = json.parseToJsonElement(responseText).jsonObject
            
            // Check for error in JSON
            val error = responseJson["error_code"]?.jsonPrimitive?.content
            if (error != null) {
                val errorMessage = responseJson["error_message"]?.jsonPrimitive?.content ?: "Unknown error"
                println("[DEBUG_LOG] YandexSpeechClient: API error: $error - $errorMessage")
                throw Exception("Yandex SpeechKit API error ($error): $errorMessage")
            }
            
            // Extract transcribed text
            // Response structure: { "result": "transcribed text" }
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
