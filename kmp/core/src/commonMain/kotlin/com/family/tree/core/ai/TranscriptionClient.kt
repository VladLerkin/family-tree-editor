package com.family.tree.core.ai

/**
 * Интерфейс для транскрипции аудио в текст.
 */
interface TranscriptionClient {
    /**
     * Транскрибирует аудио в текст.
     * 
     * @param audioData Аудио данные (поддерживаемые форматы зависят от провайдера)
     * @param config Конфигурация AI с параметрами транскрипции
     * @return Транскрибированный текст
     */
    suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String
}

/**
 * Фабрика для создания клиентов транскрипции в зависимости от провайдера.
 */
object TranscriptionClientFactory {
    /**
     * Создаёт клиент транскрипции на основе конфигурации.
     */
    fun createClient(config: AiConfig): TranscriptionClient {
        return when (config.getTranscriptionProvider()) {
            TranscriptionProvider.OPENAI_WHISPER -> OpenAiWhisperClient()
            TranscriptionProvider.GOOGLE_SPEECH -> GoogleSpeechClient()
        }
    }
}
