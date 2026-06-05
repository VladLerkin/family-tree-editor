package com.family.tree.core.ai

actual class VoskRecognizerManager actual constructor() {
    actual suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String {
        throw UnsupportedOperationException("Vosk local STT is not yet supported on iOS. Please use system dictation or a cloud API.")
    }
    
    actual fun isModelDownloaded(language: String): Boolean = false
    
    actual suspend fun transcribeAudio(audioData: ByteArray, language: String): String {
        throw UnsupportedOperationException("Vosk local STT is not yet supported on iOS.")
    }
}
