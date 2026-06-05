package com.family.tree.core.ai

class VoskTranscriptionClient : TranscriptionClient {
    private val manager = VoskRecognizerManager()

    override suspend fun transcribeAudio(audioData: ByteArray, config: AiConfig): String {
        // Use the configured language, default to "ru" if empty
        val lang = if (config.language.isBlank()) "ru" else config.language
        
        if (!manager.isModelDownloaded(lang)) {
            // Alternatively, we could automatically download here, but that blocks for minutes without progress.
            // For now we assume the UI handles the downloading first, or we throw.
            throw Exception("Vosk model for language '$lang' is not downloaded. Please download it in AI Settings first.")
        }
        
        return manager.transcribeAudio(audioData, lang)
    }
}
