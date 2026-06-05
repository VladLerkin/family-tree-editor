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
class TranscriptionClientFactory(
    private val openAiWhisperClient: OpenAiWhisperClient,
    private val googleSpeechClient: GoogleSpeechClient,
    private val yandexSpeechClient: YandexSpeechClient,
    private val voskTranscriptionClient: VoskTranscriptionClient = VoskTranscriptionClient()
) {
    /**
     * Creates a transcription client based on configuration.
     */
    fun createClient(config: AiConfig): TranscriptionClient {
        return when (config.getTranscriptionProvider()) {
            TranscriptionProvider.OPENAI_WHISPER -> openAiWhisperClient
            TranscriptionProvider.GOOGLE_SPEECH -> googleSpeechClient
            TranscriptionProvider.YANDEX_SPEECHKIT -> yandexSpeechClient
            TranscriptionProvider.VOSK_LOCAL -> voskTranscriptionClient
        }
    }
}
