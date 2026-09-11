package com.family.tree.core.ai

actual class SherpaRecognizerManager actual constructor() {
    actual suspend fun downloadModel(language: String, onProgress: (Float) -> Unit): String {
        throw UnsupportedOperationException("Vosk local STT is not yet supported on Web. Please use a cloud API.")
    }
    
    actual fun isModelDownloaded(language: String): Boolean = false
    
    actual fun deleteModel(language: String) {}
    
    actual suspend fun transcribeAudio(audioData: ByteArray, language: String): String {
        throw UnsupportedOperationException("Vosk local STT is not yet supported on Web.")
    }
}
