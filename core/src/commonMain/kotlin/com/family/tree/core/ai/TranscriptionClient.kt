package com.family.tree.core.ai

/**
 * Interface for transcribing audio to text.
 */
interface TranscriptionClient {
    /**
     * Transcribes audio to text.
     * 
     * @param audioData Audio data (supported formats depend on provider)
     * @param config AI configuration with transcription parameters
     * @return Transcribed text
     */
    suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String
}

/**
 * Factory for creating transcription clients based on provider.
 */
object TranscriptionClientFactory {
    /**
     * Creates a transcription client based on configuration.
     */
    fun createClient(config: AiConfig): TranscriptionClient {
        return when (config.getTranscriptionProvider()) {
            TranscriptionProvider.OPENAI_WHISPER -> OpenAiWhisperClient()
            TranscriptionProvider.GOOGLE_SPEECH -> GoogleSpeechClient()
            TranscriptionProvider.YANDEX_SPEECHKIT -> YandexSpeechClient()
        }
    }
}
