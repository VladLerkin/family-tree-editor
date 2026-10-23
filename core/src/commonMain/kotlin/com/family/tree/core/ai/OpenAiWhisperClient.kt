package com.family.tree.core.ai

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

private data class AudioPayload(val data: ByteArray, val contentType: String, val fileName: String)

/**
 * Клиент для работы с OpenAI Whisper API (транскрипция аудио).
 */
class OpenAiWhisperClient(
    private val httpClient: HttpClient,
    private val json: Json
) : TranscriptionClient {
    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        val apiKey = config.getApiKeyForTranscription()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("OpenAI API key is required for transcription. Please configure it in the AI Settings menu.")
        }
        
        val baseUrl = config.baseUrl.ifBlank { "https://api.openai.com/v1" }
        val url = "$baseUrl/audio/transcriptions"
        
        // Определяем формат аудио по сигнатуре данных
        // .caf файлы начинаются с "caff" (0x63616666)
        // .amr файлы начинаются с "#!AMR" (0x2321414D52) для AMR-NB или "#!AMR-WB" для AMR-WB
        // .m4a файлы содержат "ftyp" на позиции 4-8
        val isCAF = audioData.hasAscii("caff")
        
        val isAMR = audioData.hasAscii("#!AMR")
        
        // Если это CAF или AMR файл, конвертируем его в WAV для Whisper API
        val (finalData, contentType, fileName) = when {
            isCAF -> {
                println("[DEBUG_LOG] OpenAiWhisperClient: Converting CAF to WAV format")
                val wavData = convertCafToWav(audioData)
                println("[DEBUG_LOG] OpenAiWhisperClient: Converted to WAV (size: ${wavData.size} bytes)")
                AudioPayload(wavData, "audio/wav", "audio.wav")
            }
            isAMR -> {
                println("[DEBUG_LOG] OpenAiWhisperClient: Detected AMR format, not supported by Whisper API")
                // AMR формат не поддерживается Whisper API напрямую
                // Отправляем как есть с правильным MIME типом, чтобы получить понятную ошибку
                // В будущем можно добавить конвертацию AMR -> WAV через FFmpeg
                throw IllegalArgumentException("AMR audio format is not supported by OpenAI Whisper API. Please use a different recording format (WAV, M4A, MP3, etc.) or switch to Google Speech-to-Text transcription provider.")
            }
            else -> {
                AudioPayload(audioData, "audio/m4a", "audio.m4a")
            }
        }
        
        println("[DEBUG_LOG] OpenAiWhisperClient: Sending audio format: $contentType (size: ${finalData.size} bytes)")
        
        val response = httpClient.submitFormWithBinaryData(
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
        
        // Explicitly read as bytes and decode as UTF-8 to avoid platform encoding issues
        val responseText = response.bodyAsBytes().decodeToString()
        println("[DEBUG_LOG] OpenAiWhisperClient: Whisper API response body: $responseText")
        
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        
        // Извлекаем транскрибированный текст
        val text = responseJson["text"]?.jsonPrimitive?.content
        
        if (text == null) {
            println("[DEBUG_LOG] OpenAiWhisperClient: No 'text' field in response. Response keys: ${responseJson.keys}")
            throw Exception("No text in Whisper API response")
        }
        
        return text
    }
    
    /** Конвертирует CAF (Linear PCM 16-bit mono 16kHz, iOS) в WAV. */
    private fun convertCafToWav(cafData: ByteArray): ByteArray {
        val pcm = WavAudio.findCafPcm(cafData)
        if (pcm == null) {
            println("[DEBUG_LOG] OpenAiWhisperClient: Failed to find data chunk in CAF")
            throw IllegalArgumentException("Invalid CAF file format: 'data' chunk not found.")
        }
        println("[DEBUG_LOG] OpenAiWhisperClient: Found PCM data in CAF: offset=${pcm.offset}, size=${pcm.size}")
        return WavAudio.fromPcm(cafData, pcm)
    }
}
